package com.flurry.engine.parser;

import com.flurry.engine.parser.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recursive-descent parser.
 *
 * Grammar (simplified):
 *   query      := SELECT selectList FROM IDENTIFIER (WHERE expr)?
 *   selectList := STAR | selectItem (COMMA selectItem)*
 *   selectItem := expr (AS? IDENTIFIER)?
 *
 *   expr       := orExpr
 *   orExpr     := andExpr (OR andExpr)*
 *   andExpr    := notExpr (AND notExpr)*
 *   notExpr    := NOT notExpr | comparison
 *   comparison := additive ((= | != | < | <= | > | >=) additive)?
 *   additive   := multiplicative ((+ | -) multiplicative)*
 *   multiplic. := unary ((* | /) unary)*
 *   unary      := - unary | primary
 *   primary    := literal | columnRef | ( expr )
 */
public final class Parser {

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static SelectStatement parse(String sql) {
        return new Parser(new Lexer(sql).tokenize()).parseQuery();
    }

    public SelectStatement parseQuery() {
        expect(TokenType.SELECT);
        List<SelectStatement.SelectItem> items = parseSelectList();
        expect(TokenType.FROM);
        String table = expect(TokenType.IDENTIFIER).lexeme();

        Optional<Expr> where = Optional.empty();
        if (match(TokenType.WHERE)) {
            where = Optional.of(parseExpr());
        }
        expect(TokenType.EOF);
        return new SelectStatement(items, table, where);
    }

    // --- SELECT list ---

    private List<SelectStatement.SelectItem> parseSelectList() {
        List<SelectStatement.SelectItem> items = new ArrayList<>();
        if (match(TokenType.STAR)) {
            items.add(SelectStatement.SelectItem.star());
            return items;
        }
        items.add(parseSelectItem());
        while (match(TokenType.COMMA)) {
            items.add(parseSelectItem());
        }
        return items;
    }

    private SelectStatement.SelectItem parseSelectItem() {
        Expr e = parseExpr();
        String alias = null;
        if (match(TokenType.AS)) {
            alias = expect(TokenType.IDENTIFIER).lexeme();
        } else if (check(TokenType.IDENTIFIER)) {
            alias = advance().lexeme(); // implicit alias: `expr alias`
        }
        return SelectStatement.SelectItem.of(e, alias);
    }

    // --- expression precedence climbing ---

    private Expr parseExpr() { return parseOr(); }

    private Expr parseOr() {
        Expr left = parseAnd();
        while (match(TokenType.OR)) {
            Expr right = parseAnd();
            left = new Expr.BinaryExpr(BinaryOp.OR, left, right);
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseNot();
        while (match(TokenType.AND)) {
            Expr right = parseNot();
            left = new Expr.BinaryExpr(BinaryOp.AND, left, right);
        }
        return left;
    }

    private Expr parseNot() {
        if (match(TokenType.NOT)) {
            return new Expr.UnaryExpr(UnaryOp.NOT, parseNot());
        }
        return parseComparison();
    }

    private Expr parseComparison() {
        Expr left = parseAdditive();
        BinaryOp op = switch (peek().type()) {
            case EQ  -> BinaryOp.EQ;
            case NEQ -> BinaryOp.NEQ;
            case LT  -> BinaryOp.LT;
            case LTE -> BinaryOp.LTE;
            case GT  -> BinaryOp.GT;
            case GTE -> BinaryOp.GTE;
            default  -> null;
        };
        if (op != null) {
            advance();
            Expr right = parseAdditive();
            return new Expr.BinaryExpr(op, left, right);
        }
        return left;
    }

    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            BinaryOp op = advance().type() == TokenType.PLUS ? BinaryOp.PLUS : BinaryOp.MINUS;
            Expr right = parseMultiplicative();
            left = new Expr.BinaryExpr(op, left, right);
        }
        return left;
    }

    private Expr parseMultiplicative() {
        Expr left = parseUnary();
        while (check(TokenType.STAR) || check(TokenType.SLASH)) {
            BinaryOp op = advance().type() == TokenType.STAR ? BinaryOp.STAR : BinaryOp.SLASH;
            Expr right = parseUnary();
            left = new Expr.BinaryExpr(op, left, right);
        }
        return left;
    }

    private Expr parseUnary() {
        if (match(TokenType.MINUS)) {
            return new Expr.UnaryExpr(UnaryOp.NEG, parseUnary());
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        Token t = peek();
        switch (t.type()) {
            case INTEGER_LITERAL -> {
                advance();
                return new Expr.Literal(Integer.parseInt(t.lexeme()), Expr.Literal.LiteralType.INTEGER);
            }
            case DOUBLE_LITERAL -> {
                advance();
                return new Expr.Literal(Double.parseDouble(t.lexeme()), Expr.Literal.LiteralType.DOUBLE);
            }
            case STRING_LITERAL -> {
                advance();
                return new Expr.Literal(t.lexeme(), Expr.Literal.LiteralType.STRING);
            }
            case TRUE -> { advance(); return new Expr.Literal(true, Expr.Literal.LiteralType.BOOLEAN); }
            case FALSE -> { advance(); return new Expr.Literal(false, Expr.Literal.LiteralType.BOOLEAN); }
            case NULL -> { advance(); return new Expr.Literal(null, Expr.Literal.LiteralType.NULL); }
            case LPAREN -> {
                advance();
                Expr inner = parseExpr();
                expect(TokenType.RPAREN);
                return inner;
            }
            case IDENTIFIER -> {
                String first = advance().lexeme();
                if (match(TokenType.DOT)) {
                    String col = expect(TokenType.IDENTIFIER).lexeme();
                    return new Expr.ColumnRef(first, col);   // table.column
                }
                return new Expr.ColumnRef(first);            // column
            }
            default -> throw new ParseException(
                    "unexpected token " + t.type() + " ('" + t.lexeme() + "')", t.pos());
        }
    }

    // --- helpers ---

    private Token peek()    { return tokens.get(current); }
    private Token advance() { return tokens.get(current++); }
    private boolean check(TokenType type) { return peek().type() == type; }

    private boolean match(TokenType type) {
        if (check(type)) { advance(); return true; }
        return false;
    }

    private Token expect(TokenType type) {
        if (check(type)) return advance();
        Token t = peek();
        throw new ParseException(
                "expected " + type + " but got " + t.type() + " ('" + t.lexeme() + "')", t.pos());
    }
}