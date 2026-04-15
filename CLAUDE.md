# JPA Specification Chained Join POC

## Goal

Verify that JPA Specification can chain joins across a manually decomposed ManyToMany relationship (A → AB → B) and query correctly. This is a minimal throwaway POC — keep it simple, no extra abstraction.

## Tech Stack

- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- H2 in-memory database
- JUnit 5 integration tests

## Entity Model

### Entity A
- `id` (Long, generated)
- `name` (String)
- `@OneToMany(mappedBy = "a", cascade = ALL, orphanRemoval = true)` → `List<AB> abList`

### Entity B
- `id` (Long, generated)
- `label` (String)
- `@OneToMany(mappedBy = "b")` → `List<AB> abList`

### Bridge Entity AB
- `id` (Long, generated)
- `@ManyToOne` → `A a` (with `@JoinColumn(name = "a_id")`)
- `@ManyToOne` → `B b` (with `@JoinColumn(name = "b_id")`)
- `priority` (Integer) — extra column to justify the decomposition over raw `@ManyToMany`

## Repository

- `ARepository extends JpaRepository<A, Long>, JpaSpecificationExecutor<A>`
- Repositories for B and AB as needed for test data seeding

## Specification Class — `ASpecifications`

Implement the following static methods returning `Specification<A>`:

1. **`hasBWithLabel(String label)`**
   - Chained join: `root.join("abList").join("b")`
   - Filter: `cb.equal(bJoin.get("label"), label)`
   - Must set `query.distinct(true)`

2. **`hasBWithLabelLeftJoin(String label)`**
   - Same as above but using `JoinType.LEFT`
   - Verify that A records without any AB associations are included when combined with OR predicates

3. **`hasAbWithPriorityGreaterThan(int priority)`**
   - Single join to AB only: `root.join("abList")`
   - Filter on the bridge table's extra column: `cb.greaterThan(abJoin.get("priority"), priority)`
   - Must set `query.distinct(true)`

4. **`hasBWithLabelAndMinPriority(String label, int minPriority)`**
   - Chained join through AB to B
   - Compound predicate: `cb.and(labelPredicate, priorityPredicate)` filtering on both B.label and AB.priority

## Integration Test — `ASpecificationTest`

Use `@SpringBootTest` with `@Transactional`. Seed the following data in `@BeforeEach`:

```
A1("alpha") --[priority=1]--> B1("red")
A1("alpha") --[priority=5]--> B2("blue")
A2("beta")  --[priority=3]--> B1("red")
A3("gamma") — no associations
```

### Test Cases

1. **`testChainedJoinFilterByBLabel`**
   - Spec: `hasBWithLabel("red")`
   - Expected: returns A1, A2 — no duplicates

2. **`testChainedJoinNoMatch`**
   - Spec: `hasBWithLabel("green")`
   - Expected: empty list

3. **`testLeftJoinIncludesUnassociated`**
   - Spec: `hasBWithLabelLeftJoin("red")` OR always-true predicate
   - Expected: returns A1, A2, A3 (A3 included because LEFT JOIN)

4. **`testFilterByBridgePriority`**
   - Spec: `hasAbWithPriorityGreaterThan(2)`
   - Expected: returns A1 (priority 5), A2 (priority 3)

5. **`testCompoundPredicate`**
   - Spec: `hasBWithLabelAndMinPriority("red", 2)`
   - Expected: returns only A2 (A1's link to red has priority 1)

6. **`testWithPageable`**
   - Spec: `hasBWithLabel("red")`
   - Use `PageRequest.of(0, 1, Sort.by("name"))`
   - Expected: page with totalElements=2, content contains A1 only (first page, sorted by name)
   - Verify no duplicate count issues

7. **`testSpecComposition`**
   - Combine specs: `hasBWithLabel("red").and(hasAbWithPriorityGreaterThan(2))`
   - Expected: returns only A2

## Application Config — `application.yml` (test profile)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
    hibernate:
      ddl-auto: create-drop
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Project Structure

```
src/main/java/com/example/poc/
  entity/       → A.java, B.java, AB.java
  repository/   → ARepository.java, BRepository.java, ABRepository.java
  spec/         → ASpecifications.java
  PocApplication.java

src/test/java/com/example/poc/
  ASpecificationTest.java

src/test/resources/
  application.yml
```

## Constraints

- No Lombok — use plain getters/setters or records for clarity
- No service layer — tests call repository directly
- No REST controllers — this is a pure persistence test
- Keep all specs in one class
- Every test must pass with `./mvnw test`
- **Commit changes on every task** — after completing any task (fix, feature, refactor, doc update), stage and commit the changes before moving on
