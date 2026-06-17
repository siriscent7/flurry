package com.flurry.engine.parser;

/**
 * A single lexical token.
 * @param type   the token category
 * @param lexeme the exact text matched
 * @param pos    starting character position in the input (for error messages)
 */
public record Token(TokenType type, String lexeme, int pos) {

    @Override
    public String toString() {
        return String.format("%s(%s)", type, lexeme);
    }
}