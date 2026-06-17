package com.flurry.engine.parser.ast;

import java.util.List;
import java.util.Optional;

public record SelectStatement(
        List<SelectItem> items,
        String fromTable,
        Optional<JoinClause> join,
        Optional<Expr> where,
        List<Expr> groupBy,
        List<OrderKey> orderBy,
        Optional<Integer> limit) {

    public record SelectItem(Expr expr, String alias, boolean isStar) {
        public static SelectItem star() { return new SelectItem(null, null, true); }
        public static SelectItem of(Expr e, String alias) { return new SelectItem(e, alias, false); }
        @Override public String toString() {
            if (isStar) return "*";
            return alias == null ? String.valueOf(expr) : expr + " AS " + alias;
        }
    }

    public record OrderKey(Expr expr, boolean descending) {
        @Override public String toString() { return expr + (descending ? " DESC" : " ASC"); }
    }

    /** INNER JOIN <table> ON <leftCol> = <rightCol> */
    public record JoinClause(String rightTable, Expr.ColumnRef leftKey, Expr.ColumnRef rightKey) {
        @Override public String toString() {
            return "JOIN " + rightTable + " ON " + leftKey + " = " + rightKey;
        }
    }
}