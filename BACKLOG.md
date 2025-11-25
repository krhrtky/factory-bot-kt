# factory-bot-kt Backlog

## Overview
Remaining tasks for factory-bot-kt library, organized by priority.

---

## P1: Core Features

### FBK-001: buildList/createList operations
- **Description**: Multiple record creation support
- **API**:
  ```kotlin
  val users = buildList<User>(5)
  val saved = createList<User>(3) { role { Role.ADMIN } }
  ```
- **Acceptance Criteria**:
  - [ ] `buildList<T>(count)` returns `List<T>`
  - [ ] `createList<T>(count)` persists all instances
  - [ ] Supports attribute overrides
  - [ ] Supports traits

### FBK-002: buildPair/createPair operations
- **Description**: Convenience methods for creating exactly 2 instances
- **API**:
  ```kotlin
  val (user1, user2) = buildPair<User>()
  val (saved1, saved2) = createPair<User>("admin")
  ```
- **Acceptance Criteria**:
  - [ ] Returns `Pair<T, T>`
  - [ ] Supports traits and overrides

### FBK-003: Transient attributes
- **Description**: Attributes not set on built object, accessible in callbacks
- **API**:
  ```kotlin
  factory<User> {
      transient("postsCount", 0)
      afterCreate { user, evaluator ->
          repeat(evaluator.get<Int>("postsCount")) {
              create<Post> { author { user } }
          }
      }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Transient attributes not set on instance
  - [ ] Accessible via evaluator in callbacks
  - [ ] Override at build time

### FBK-004: Dependent attributes (attribute references)
- **Description**: Lazy evaluation based on other attribute values
- **API**:
  ```kotlin
  factory<User> {
      firstName { "John" }
      lastName { "Doe" }
      email { "${get<String>("firstName").lowercase()}@example.com" }
  }
  ```
- **Acceptance Criteria**:
  - [ ] `get<T>(name)` resolves other attributes
  - [ ] Handles circular dependency detection
  - [ ] Works with sequences

### FBK-005: attributesFor strategy
- **Description**: Returns attribute map instead of instance
- **API**:
  ```kotlin
  val attrs: Map<String, Any?> = attributesFor<User>()
  ```
- **Acceptance Criteria**:
  - [ ] Returns `Map<String, Any?>`
  - [ ] Excludes transient attributes
  - [ ] Supports traits and overrides

### FBK-006: beforeCreate callback
- **Description**: Callback before persistence
- **API**:
  ```kotlin
  factory<User> {
      beforeCreate { user -> user.validate() }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Called before persistence
  - [ ] Can modify instance
  - [ ] Exception cancels create

### FBK-007: Sequence aliases and rewind
- **Description**: Share sequences across names, reset to initial value
- **API**:
  ```kotlin
  defineSequence("email", aliases = listOf("userEmail", "adminEmail")) { n -> "user$n@test.com" }
  rewindSequences()
  ```
- **Acceptance Criteria**:
  - [ ] Aliases share counter
  - [ ] `rewindSequences()` resets all
  - [ ] `rewindSequence(name)` resets one

---

## P2: Advanced Features

### FBK-008: has_many associations
- **Description**: Create multiple associated objects
- **API**:
  ```kotlin
  factory<User> {
      posts { buildList(3) { association<Post>() } }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Inline association list creation
  - [ ] Callback-based approach for complex cases
  - [ ] Proper back-reference handling

### FBK-009: Polymorphic associations
- **Description**: Support polymorphic relationships
- **API**:
  ```kotlin
  factory<Comment> {
      trait("forPost") { commentable { association<Post>() } }
      trait("forVideo") { commentable { association<Video>() } }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Trait-based polymorphic type selection
  - [ ] Type-safe association resolution

### FBK-010: Custom construction (initializeWith)
- **Description**: Override default instantiation
- **API**:
  ```kotlin
  factory<User> {
      initializeWith {
          User.Builder()
              .firstName(get("firstName"))
              .build()
      }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Custom constructor/builder support
  - [ ] Access to evaluated attributes
  - [ ] Works with all strategies

### FBK-011: Custom persistence (toCreate)
- **Description**: Override default save behavior
- **API**:
  ```kotlin
  factory<User> {
      toCreate { user -> userRepository.save(user) }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Custom persistence per factory
  - [ ] Global persistence adapter

### FBK-012: skipCreate
- **Description**: Disable persistence for specific factories
- **API**:
  ```kotlin
  factory<User> {
      skipCreate()
  }
  ```
- **Acceptance Criteria**:
  - [ ] `create()` behaves like `build()`
  - [ ] Callbacks still fire appropriately

### FBK-013: Factory linting
- **Description**: Validate all factories at test startup
- **API**:
  ```kotlin
  FactoryBot.lint<User>()
  FactoryBot.lintAll()
  FactoryBot.lint<User>(traits = listOf("admin"))
  ```
- **Acceptance Criteria**:
  - [ ] Build all factories to verify validity
  - [ ] Trait-specific linting
  - [ ] Clear error messages

### FBK-014: Enum trait auto-generation
- **Description**: Auto-generate traits from enum values
- **API**:
  ```kotlin
  factory<Task> {
      enumTraits<Status>() // generates "statusPending", "statusDone", etc.
  }
  ```
- **Acceptance Criteria**:
  - [ ] Reflection-based enum discovery
  - [ ] Configurable trait naming

### FBK-015: Traits within traits
- **Description**: Compose traits from other traits
- **API**:
  ```kotlin
  factory<User> {
      trait("admin") { role { Role.ADMIN } }
      trait("superAdmin") {
          admin() // include admin trait
          permissions { listOf(Permission.ALL) }
      }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Nested trait composition
  - [ ] Proper attribute override order

### FBK-016: Modify existing factory definitions
- **Description**: Override factory after initial definition
- **API**:
  ```kotlin
  FactoryBot.modify<User> {
      email { "modified@example.com" }
  }
  ```
- **Acceptance Criteria**:
  - [ ] Merge with existing definition
  - [ ] Callbacks compound, not replace

---

## P3: Nice to Have

### FBK-017: Custom strategy registration
- **Description**: User-defined build strategies
- **API**:
  ```kotlin
  FactoryBot.registerStrategy("json") { factory, overrides ->
      factory.build(overrides).toJson()
  }
  FactoryBot.json<User>()
  ```

### FBK-018: JPA persistence adapter
- **Description**: Integration with JPA/Hibernate
- **Scope**: Separate module `factory-bot-kotlin-jpa`

### FBK-019: Exposed persistence adapter
- **Description**: Integration with Exposed SQL
- **Scope**: Separate module `factory-bot-kotlin-exposed`

### FBK-020: MongoDB persistence adapter
- **Description**: Integration with MongoDB
- **Scope**: Separate module `factory-bot-kotlin-mongodb`

### FBK-021: Factory aliases
- **Description**: Multiple names for same factory
- **API**:
  ```kotlin
  factory<User>(aliases = listOf("author", "commenter")) { ... }
  ```

### FBK-022: IntelliJ IDEA plugin
- **Description**: Navigation and autocomplete for factories
- **Scope**: Separate project

### FBK-023: Performance benchmarks
- **Description**: Benchmark suite for performance validation
- **Target**: <10ms per 1000 builds

---

## Tech Debt / Cleanup

### FBK-024: Remove unused variable warning
- **File**: `src/test/kotlin/io/github/factorybot/FactoryBotTest.kt:225`
- **Issue**: Variable `stubbedId` is never used
- **Priority**: Low

### FBK-025: Add KDoc to convenience extensions
- **File**: `src/main/kotlin/io/github/factorybot/FactoryBot.kt:324-362`
- **Issue**: 10 extension functions lack documentation
- **Priority**: Low (only if publishing externally)

### FBK-026: Fix Gradle deprecation warnings
- **Issue**: Deprecated Gradle features used
- **Action**: Update for Gradle 9.0 compatibility

### FBK-027: Add README.md
- **Description**: User-facing documentation
- **Content**:
  - Installation instructions
  - Quick start guide
  - API reference link
  - Migration guide from Ruby factory_bot

---

## Completed (P0)

- [x] Basic factory definition DSL with type safety
- [x] build/create/buildStubbed strategies
- [x] Attribute lazy evaluation
- [x] Sequences (global and inline)
- [x] Traits with composition
- [x] Factory inheritance
- [x] Simple associations (belongs_to)
- [x] afterBuild/afterCreate callbacks
- [x] Override attributes at build time
- [x] Factory registry and lookup
- [x] KDoc documentation (53 blocks)
- [x] Property caching for performance
- [x] Error handling tests (5 tests)
- [x] Thread safety tests (4 tests)
- [x] 32 passing tests (~95% coverage)
