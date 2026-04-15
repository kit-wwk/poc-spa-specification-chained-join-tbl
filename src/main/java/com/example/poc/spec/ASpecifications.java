package com.example.poc.spec;

import com.example.poc.entity.A;
import com.example.poc.entity.AB;
import com.example.poc.entity.B;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class ASpecifications {

    public static Specification<A> hasBWithLabel(String label) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<A, B> bJoin = root.join("abList").join("b");
            return cb.equal(bJoin.get("label"), label);
        };
    }

    public static Specification<A> hasBWithLabelLeftJoin(String label) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<A, AB> abJoin = root.join("abList", JoinType.LEFT);
            Join<AB, B> bJoin = abJoin.join("b", JoinType.LEFT);
            return cb.equal(bJoin.get("label"), label);
        };
    }

    public static Specification<A> hasAbWithPriorityGreaterThan(int priority) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<A, AB> abJoin = getOrCreateJoin(root, "abList", JoinType.INNER);
            return cb.greaterThan(abJoin.get("priority"), priority);
        };
    }

    public static Specification<A> hasBWithLabelAndMinPriority(String label, int minPriority) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<A, AB> abJoin = root.join("abList");
            Join<AB, B> bJoin = abJoin.join("b");
            return cb.and(
                    cb.equal(bJoin.get("label"), label),
                    cb.greaterThan(abJoin.get("priority"), minPriority)
            );
        };
    }

    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> getOrCreateJoin(From<?, X> from, String attribute, JoinType joinType) {
        return (Join<X, Y>) from.getJoins().stream()
                .filter(j -> j.getAttribute().getName().equals(attribute))
                .findFirst()
                .orElseGet(() -> from.join(attribute, joinType));
    }
}
