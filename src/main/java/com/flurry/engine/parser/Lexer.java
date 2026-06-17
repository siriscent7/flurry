package com.flurry.engine.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts a SQL string into a list of tokens.
 * Single-pass, character-by-character scanner.
 */
public final class Lexer {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("SELECT", TokenType.SELECT),
        Map.entry("FROM",   TokenType.FROM),
        Map.entry("WHERE",  TokenType.WHERE),
        Map.entry("AND",    TokenType.AND),
        Map.entry("OR",     TokenType.OR),
        Map.entry("NOT",    TokenType.NOT),
        Map.entry("JOIN",   TokenType.JOIN),
        Map.entry("ON",     TokenType.ON),
        Map.entry("GROUP",  TokenType.GROUP),
        Map.entry("BY",     TokenType.BY),
        Map.entry("ORDER",  TokenType.ORDER),
        Map.entry("ASC",    TokenType.ASC),
        Map.entry("DESC",   TokenType.DESC),
        Map.entry("LIMIT",  TokenType.LIMIT),
        Map.entry("AS",     TokenType.AS),
        Map.entry("NULL",   TokenType.NULL),
        Map.entry("TRUE",   TokenType.TRUE),
        Map.entry("FALSE",  TokenType.FALSE)
    );

    private final String src;
    private int pos = 0;
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(String src) {
        this.src = src;
    }

    public List<Token> tokenize() {
        while (!atEnd()) {
            char c = peek();

            if (Character.isWhitespace(c)) { advance(); continue; }

            if (Character.isLetter(c) || c == '_') { readIdentifierOrKeyword(); continue; }
            if (Character.isDigit(c))              { readNumber();              continue; }
            if (c == '\'')                          { readString();             continue; }

            readSymbol();
        }
        tokens.add(new Token(TokenType.EOF, "", pos));
        return tokens;
    }

    // --- token readers ---

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (!atEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) advance();
        String text = src.substring(start, pos);
        TokenType kw = KEYWORDS.get(text.toUpperCase());
        if (kw != null) {
            add(kw, text, start);
        } else {
            add(TokenType.IDENTIFIER, text, start);
        }
    }

    private void readNumber() {
        int start = pos;
        boolean isDouble = false;
        while (!atEnd() && Character.isDigit(peek())) advance();
        if (!atEnd() && peek() == '.') {
            isDouble = true;
            advance();
            while (!atEnd() && Character.isDigit(peek())) advance();
        }
        String text = src.substring(start, pos);
        add(isDouble ? TokenType.DOUBLE_LITERAL : TokenType.INTEGER_LITERAL, text, start);
    }

    private void readString() {
        int start = pos;
        advance(); // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (!atEnd() && peek() != '\'') {
            sb.append(peek());
            advance();
        }
        if (atEnd()) throw new LexException("unterminated string literal", start);
        advance(); // consume closing quote
        add(TokenType.STRING_LITERAL, sb.toString(), start);
    }

    private void readSymbol() {
        int start = pos;
        char c = advance();
        switch (c) {
            case '=' -> add(TokenType.EQ, "=", start);
            case '<' -> {
                if (match('=')) add(TokenType.LTE, "<=", start);
                else if (match('>')) add(TokenType.NEQ, "<>", start);
                else add(TokenType.LT, "<", start);
            }
            case '>' -> {
                if (match('=')) add(TokenType.GTE, ">=", start);
                else add(TokenType.GT, ">", start);
            }
            case '!' -> {
                if (match('=')) add(TokenType.NEQ, "!=", start);
                else throw new LexException("unexpected '!'", start);
            }
            case '+' -> add(TokenType.PLUS, "+", start);
            case '-' -> add(TokenType.MINUS, "-", start);
            case '*' -> add(TokenType.STAR, "*", start);
            case '/' -> add(TokenType.SLASH, "/", start);
            case ',' -> add(TokenType.COMMA, ",", start);
            case '(' -> add(TokenType.LPAREN, "(", start);
            case ')' -> add(TokenType.RPAREN, ")", start);
            case '.' -> add(TokenType.DOT, ".", start);
            default  -> throw new LexException("unexpected character '" + c + "'", start);
        }
    }

    // --- helpers ---

    private boolean atEnd() { return pos >= src.length(); }
    private char peek()     { return src.charAt(pos); }
    private char advance()  { return src.charAt(pos++); }

    private boolean match(char expected) {
        if (atEnd() || src.charAt(pos) != expected) return false;
        pos++;
        return true;
    }

    private void add(TokenType type, String lexeme, int start) {
        tokens.add(new Token(type, lexeme, start));
    }
}