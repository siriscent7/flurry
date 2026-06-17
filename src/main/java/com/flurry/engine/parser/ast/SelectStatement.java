package com.flurry.engine.parser.ast;

import java.util.List;
import java.util.Optional;

/**
 * Represents: SELECT <items> FROM <table> [WHERE <predicate>]
 * (JOIN / GROUP BY / ORDER BY / LIMIT added in later phases.)
 */
public record SelectStatement(
        List<SelectItem> items,
        String fromTable,
        Optional<Expr> where) {

    /** A projected expression with an optional alias, or `*`. */
    public record SelectItem(Expr expr, String alias, boolean isStar) {
        public static SelectItem star() { return new SelectItem(null, null, true); }
        public static SelectItem of(Expr e, String alias) { return new SelectItem(e, alias, false); }

        @Override public String toString() {
            if (isStar) return "*";
            return alias == null ? expr.toString() : expr + " AS " + alias;
        }
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(String.join(", ", items.stream().map(Object::toString).toList()));
        sb.append(" FROM ").append(fromTable);
        where.ifPresent(w -> sb.append(" WHERE ").append(w));
        return sb.toString();
    }
}