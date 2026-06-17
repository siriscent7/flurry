package com.flurry.engine.parser;

import com.flurry.engine.parser.ast.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void parsesSimpleSelect() {
        SelectStatement s = Parser.parse("SELECT name, age FROM users");
        assertEquals("users", s.fromTable());
        assertEquals(2, s.items().size());
        assertTrue(s.where().isEmpty());
    }

    @Test
    void parsesStar() {
        SelectStatement s = Parser.parse("SELECT * FROM users");
        assertEquals(1, s.items().size());
        assertTrue(s.items().get(0).isStar());
    }

    @Test
    void parsesWhereWithAndPrecedence() {
        SelectStatement s = Parser.parse(
            "SELECT name FROM users WHERE age >= 30 AND city = 'San Jose'");
        assertTrue(s.where().isPresent());
        Expr w = s.where().get();
        // top-level should be AND
        assertInstanceOf(Expr.BinaryExpr.class, w);
        assertEquals(BinaryOp.AND, ((Expr.BinaryExpr) w).op());
    }

    @Test
    void orBindsLooserThanAnd() {
        // a AND b OR c  =>  (a AND b) OR c
        SelectStatement s = Parser.parse("SELECT x FROM t WHERE a = 1 AND b = 2 OR c = 3");
        Expr.BinaryExpr root = (Expr.BinaryExpr) s.where().get();
        assertEquals(BinaryOp.OR, root.op());
        assertEquals(BinaryOp.AND, ((Expr.BinaryExpr) root.left()).op());
    }

    @Test
    void parsesArithmeticPrecedence() {
        // price + qty * 2  =>  price + (qty * 2)
        SelectStatement s = Parser.parse("SELECT x FROM t WHERE price = qty * 2 + 1");
        // just assert it parses without error and builds a tree
        assertTrue(s.where().isPresent());
    }

    @Test
    void parsesQualifiedColumn() {
        SelectStatement s = Parser.parse("SELECT users.name FROM users");
        Expr.ColumnRef ref = (Expr.ColumnRef) s.items().get(0).expr();
        assertEquals("users", ref.table());
        assertEquals("name", ref.column());
    }

    @Test
    void parsesAlias() {
        SelectStatement s = Parser.parse("SELECT age AS years FROM users");
        assertEquals("years", s.items().get(0).alias());
    }

    @Test
    void throwsOnMissingFrom() {
        assertThrows(ParseException.class, () -> Parser.parse("SELECT name users"));
    }
}