package com.flurry.engine.parser;

import com.flurry.engine.parser.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recursive-descent parser.
 *
 * query := SELECT selectList FROM IDENTIFIER (WHERE expr)?
 *          (GROUP BY expr (, expr)*)? (ORDER BY orderKey (, orderKey)*)? (LIMIT INT)?
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

    private Expr.ColumnRef parseColumnRef() {
        String first = expect(TokenType.IDENTIFIER).lexeme();
        if (match(TokenType.DOT)) {
            String col = expect(TokenType.IDENTIFIER).lexeme();
            return new Expr.ColumnRef(first, col);
        }
        return new Expr.ColumnRef(first);
    }

    public SelectStatement parseQuery() {
        expect(TokenType.SELECT);
        List<SelectStatement.SelectItem> items = parseSelectList();
        expect(TokenType.FROM);
        String table = expect(TokenType.IDENTIFIER).lexeme();

        // optional JOIN ... ON a.x = b.y
        Optional<SelectStatement.JoinClause> join = Optional.empty();
        if (match(TokenType.JOIN)) {
            String rightTable = expect(TokenType.IDENTIFIER).lexeme();
            expect(TokenType.ON);
            Expr.ColumnRef leftKey = parseColumnRef();
            expect(TokenType.EQ);
            Expr.ColumnRef rightKey = parseColumnRef();
            join = Optional.of(new SelectStatement.JoinClause(rightTable, leftKey, rightKey));
        }

        Optional<Expr> where = Optional.empty();
        if (match(TokenType.WHERE)) {
            where = Optional.of(parseExpr());
        }

        List<Expr> groupBy = new ArrayList<>();
        if (match(TokenType.GROUP)) {
            expect(TokenType.BY);
            groupBy.add(parseExpr());
            while (match(TokenType.COMMA)) groupBy.add(parseExpr());
        }

        List<SelectStatement.OrderKey> orderBy = new ArrayList<>();
        if (match(TokenType.ORDER)) {
            expect(TokenType.BY);
            orderBy.add(parseOrderKey());
            while (match(TokenType.COMMA)) orderBy.add(parseOrderKey());
        }

        Optional<Integer> limit = Optional.empty();
        if (match(TokenType.LIMIT)) {
            Token n = expect(TokenType.INTEGER_LITERAL);
            limit = Optional.of(Integer.parseInt(n.lexeme()));
        }

        expect(TokenType.EOF);
        return new SelectStatement(items, table, join, where, groupBy, orderBy, limit);
    }

    private SelectStatement.OrderKey parseOrderKey() {
        Expr e = parseExpr();
        boolean desc = false;
        if (match(TokenType.DESC)) desc = true;
        else match(TokenType.ASC); // optional ASC
        return new SelectStatement.OrderKey(e, desc);
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

                // function call?  e.g. COUNT(*), SUM(age)
                if (check(TokenType.LPAREN)) {
                    advance(); // consume (
                    if (match(TokenType.STAR)) {
                        expect(TokenType.RPAREN);
                        return new Expr.FunctionCall(first.toUpperCase(), List.of(), true);
                    }
                    List<Expr> fnArgs = new ArrayList<>();
                    if (!check(TokenType.RPAREN)) {
                        fnArgs.add(parseExpr());
                        while (match(TokenType.COMMA)) fnArgs.add(parseExpr());
                    }
                    expect(TokenType.RPAREN);
                    return new Expr.FunctionCall(first.toUpperCase(), fnArgs, false);
                }

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