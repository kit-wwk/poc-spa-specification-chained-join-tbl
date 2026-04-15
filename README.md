# POC: JPA Specification with Chained Join through Bridge Table

## Background

When a `@ManyToMany` relationship is manually decomposed into two `@ManyToOne` relationships via a bridge/join table (to hold extra columns like `priority`), queries that need to filter by the far-side entity must **chain joins** through the bridge table. This POC validates that Spring Data JPA's `Specification` API handles this correctly with Hibernate 6.6.

## Objectives

| # | Objective | Description |
|---|-----------|-------------|
| O1 | **Chained inner join** | A `Specification` can chain `root.join("abList").join("b")` to reach entity B through the bridge table AB, and filter by B's attributes. Results must be duplicate-free (`DISTINCT`). |
| O2 | **No-match returns empty** | When no B entity matches the filter, the specification correctly returns an empty result set instead of returning all or null. |
| O3 | **Left join preserves unassociated rows** | Using `JoinType.LEFT` through the chain allows A records with no AB associations to appear in results when combined with OR predicates (e.g. always-true fallback). |
| O4 | **Bridge table column filtering** | A specification can join only to the bridge table AB (without chaining to B) and filter on AB-specific columns like `priority`. |
| O5 | **Compound predicate on same join row** | A single specification can apply multiple predicates (on both B.label and AB.priority) to the **same bridge row**, ensuring row-level AND semantics. |
| O6 | **Pagination with distinct** | `Pageable` works correctly with chained-join specifications — `totalElements` reflects the deduplicated count, and page content is correctly sorted and sliced. |
| O7 | **Specification composition with shared joins** | Two independently defined specifications can be combined with `.and()` and still produce same-row join semantics (single JOIN to bridge table), avoiding the false-positive problem of independent joins matching different rows. |

## Entity Model

```
┌──────────┐       ┌──────────────┐       ┌──────────┐
│  A       │       │  AB (bridge) │       │  B       │
├──────────┤       ├──────────────┤       ├──────────┤
│ id       │──1:N──│ a_id (FK)    │       │ id       │
│ name     │       │ b_id (FK)    │──N:1──│ label    │
│          │       │ priority     │       │          │
└──────────┘       └──────────────┘       └──────────┘
```

- **A → AB**: `@OneToMany(mappedBy = "a", cascade = ALL, orphanRemoval = true)`
- **AB → A**: `@ManyToOne` with `@JoinColumn(name = "a_id")`
- **AB → B**: `@ManyToOne` with `@JoinColumn(name = "b_id")`
- **B → AB**: `@OneToMany(mappedBy = "b")`

The `priority` column on AB justifies the manual decomposition over a raw `@ManyToMany`.

## Test Data

```
A1("alpha") --[priority=1]--> B1("red")
A1("alpha") --[priority=5]--> B2("blue")
A2("beta")  --[priority=3]--> B1("red")
A3("gamma") — no associations
```

## Test Cases

| Test | Objective | Specification Used | Expected Result | Why |
|------|-----------|--------------------|-----------------|-----|
| `testChainedJoinFilterByBLabel` | O1 | `hasBWithLabel("red")` | A1(alpha), A2(beta) — no duplicates | Both A1 and A2 link to B1("red") via AB. A1 has two AB rows but `DISTINCT` prevents it appearing twice. |
| `testChainedJoinNoMatch` | O2 | `hasBWithLabel("green")` | Empty list | No B entity has label "green", so the inner join produces zero rows. |
| `testLeftJoinIncludesUnassociated` | O3 | `hasBWithLabelLeftJoin("red")` OR always-true | A1(alpha), A2(beta), A3(gamma) | LEFT JOIN keeps A3 (which has no AB rows). The OR with `cb.conjunction()` ensures A3 passes the overall predicate. |
| `testFilterByBridgePriority` | O4 | `hasAbWithPriorityGreaterThan(2)` | A1(alpha), A2(beta) | A1 has AB with priority 5 (>2) and A2 has AB with priority 3 (>2). A3 has no AB rows. |
| `testCompoundPredicate` | O5 | `hasBWithLabelAndMinPriority("red", 2)` | A2(beta) only | A1's link to "red" has priority 1 (fails >2). A2's link to "red" has priority 3 (passes). Both conditions are checked on the **same AB row**. |
| `testWithPageable` | O6 | `hasBWithLabel("red")` + `PageRequest.of(0, 1, Sort.by("name"))` | Page: totalElements=2, content=[A1(alpha)] | Two distinct matches sorted by name. First page (size 1) returns "alpha". Verifies no duplicate count inflation. |
| `testSpecComposition` | O7 | `hasBWithLabel("red").and(hasAbWithPriorityGreaterThan(2))` | A2(beta) only | Uses `getOrCreateJoin` to reuse the same AB join across both specs. A1's "red" link (priority=1) fails the priority check on the same row. Without join reuse, A1 would incorrectly match via separate rows. |

## Key Implementation Detail: Join Reuse

When composing specifications with `.and()`, each spec's lambda receives the same `Root<A>` but would normally call `root.join("abList")` independently, creating **two separate JOINs**. This means predicates from each spec apply to different AB rows — a false positive when a single entity has multiple AB associations satisfying each predicate individually but not together.

The `getOrCreateJoin` helper in `ASpecifications` solves this by checking `from.getJoins()` for an existing join on the same attribute before creating a new one:

```java
private static <X, Y> Join<X, Y> getOrCreateJoin(From<?, X> from, String attribute, JoinType joinType) {
    return (Join<X, Y>) from.getJoins().stream()
            .filter(j -> j.getAttribute().getName().equals(attribute))
            .findFirst()
            .orElseGet(() -> from.join(attribute, joinType));
}
```

This ensures composed specs share a single join path, producing correct same-row AND semantics.

## Tech Stack

- Java 17
- Spring Boot 3.4
- Hibernate 6.6.2.Final
- H2 in-memory database
- JUnit 5 + AssertJ

## Running

```bash
./mvnw test
```
