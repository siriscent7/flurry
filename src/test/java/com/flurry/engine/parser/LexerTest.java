package com.flurry.engine.parser;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private List<Token> lex(String sql) {
        return new Lexer(sql).tokenize();
    }

    @Test
    void tokenizesSimpleSelect() {
        List<Token> t = lex("SELECT name FROM users");
        assertEquals(TokenType.SELECT, t.get(0).type());
        assertEquals(TokenType.IDENTIFIER, t.get(1).type());
        assertEquals("name", t.get(1).lexeme());
        assertEquals(TokenType.FROM, t.get(2).type());
        assertEquals(TokenType.IDENTIFIER, t.get(3).type());
        assertEquals(TokenType.EOF, t.get(4).type());
    }

    @Test
    void tokenizesOperators() {
        List<Token> t = lex("age >= 30 AND price != 9.99");
        assertEquals(TokenType.IDENTIFIER, t.get(0).type());
        assertEquals(TokenType.GTE, t.get(1).type());
        assertEquals(TokenType.INTEGER_LITERAL, t.get(2).type());
        assertEquals(TokenType.AND, t.get(3).type());
        assertEquals(TokenType.IDENTIFIER, t.get(4).type());
        assertEquals(TokenType.NEQ, t.get(5).type());
        assertEquals(TokenType.DOUBLE_LITERAL, t.get(6).type());
        assertEquals("9.99", t.get(6).lexeme());
    }

    @Test
    void tokenizesStringLiteral() {
        List<Token> t = lex("city = 'San Jose'");
        assertEquals(TokenType.STRING_LITERAL, t.get(2).type());
        assertEquals("San Jose", t.get(2).lexeme()); // quotes stripped
    }

    @Test
    void keywordsAreCaseInsensitive() {
        List<Token> t = lex("select * from t");
        assertEquals(TokenType.SELECT, t.get(0).type());
        assertEquals(TokenType.STAR, t.get(1).type());
        assertEquals(TokenType.FROM, t.get(2).type());
    }

    @Test
    void throwsOnUnterminatedString() {
        assertThrows(LexException.class, () -> lex("name = 'oops"));
    }

    @Test
    void throwsOnUnexpectedChar() {
        assertThrows(LexException.class, () -> lex("a @ b"));
    }
}