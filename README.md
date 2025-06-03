# ✨ validation-dsl

[![Build](https://github.com/ericTsiliacos/validation-dsl/actions/workflows/ci.yml/badge.svg)](https://github.com/ericTsiliacos/validation-dsl/actions)
[![codecov](https://codecov.io/gh/ericTsiliacos/validation-dsl/branch/main/graph/badge.svg)](https://codecov.io/gh/ericTsiliacos/validation-dsl)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight, expressive, and composable validation DSL for Kotlin.  
Perfect for validating nested data structures with readable, declarative syntax.

---

## 🌟 Features

- Intuitive DSL for nested validation
- Composable and reusable rules
- List and nullable field support
- Dependent rule chaining (short-circuiting)
- Functional-style Validated/Invalid result types
- Fully testable and side-effect free
- Zero dependencies

---

## 🚀 Getting Started

```kotlin
data class User(val name: String, val tags: List<Tag>)
data class Tag(val value: String)

val validator = validator<User> {
    validate(User::name) {
        rule("must not be blank") { it.isNotBlank() }
    }

    validateEach(User::tags) {
        validate(Tag::value) {
            rule("must not be blank") { it.isNotBlank() }
        }
    }
}

val result = validator.validate(User(name = "", tags = listOf(Tag("ok"), Tag(""))))

if (!result.isValid) {
    result.errors.forEach {
        println("${it.path}: ${it.message}")
    }
}
```

---

## 🧠 Dependent Rule Chaining

Use `dependent {}` to short-circuit evaluation. Each rule only runs if the previous one passed:

```kotlin
validate(User::age) {
    dependent {
        rule("must be numeric") { it.all(Char::isDigit) }
        rule("must be ≥ 18") { it.toInt() >= 18 }
    }
}
```

---

## 📊 Validating Lists

```kotlin
validateEach(User::emails) {
    rule("must contain @") { it.contains("@") }
}
```

---

## ❓ Nullable Field Validation

```kotlin
validate(User::nickname) {
    whenNotNull {
        rule("must be at least 3 chars") { it.length >= 3 }
    }
}
```

---

## 🛠️ Custom Reusable Rules

```kotlin
val notBlank = fromPredicate<String>("username", "must not be blank") { it.isNotBlank() }

validate(User::username) {
    rule(notBlank)
}
```

---

## 🎯 Composing Rules

Use `and` for parallel evaluation, or `andThen` for dependent chaining:

```kotlin
val numeric = fromPredicate<String>("age", "must be numeric") { it.all(Char::isDigit) }
val over18 = fromPredicate<String>("age", "must be ≥ 18") { it.toInt() >= 18 }

val ageRule = numeric andThen over18

validate(User::age) {
    rule(ageRule)
}
```

---

## ✅ ValidationResult API

```kotlin
val result: ValidationResult = validator.validate(user)

if (result.isValid) {
    println("All good!")
} else {
    result.errors.forEach {
        println("${it.path}: ${it.message}")
    }
}
```

---

## 🧪 Test Utilities

All validations yield `Validated<Unit>` underneath, enabling composition, unit tests, and error aggregation:

```kotlin
val rule: Rule<String> = fromPredicate("name", "must not be blank") { it.isNotBlank() }

assertEquals(Validated.Valid(Unit), rule("valid"))
assertTrue(rule("") is Validated.Invalid)
```

---

## 🧹 Grouping Related Rules

Use `group("label") { ... }` to logically organize related validation rules and attach a `group` label to any errors produced inside that block.

This helps you:
- Structure validation output by logical sections (e.g., "identity", "address checks")
- Improve traceability and UI presentation
- Group related error messages together for the user

```kotlin
validate(User::name) {
    group("name rules") {
        rule("must not be blank") { it.isNotBlank() }
        rule("must be at least 3 characters") { it.length >= 3 }
    }
}
```

Any validation error produced inside the group will include the group label:

```kotlin
val result = validator.validate(User(name = "", tags = emptyList()))
for (error in result.errors) {
    println("[${error.group}] ${error.path}: ${error.message}")
}
```

> 🧠 Group labels are **not inherited** by nested `validate`, `validateEach`, `dependent`, or `group` blocks.  
> Only rules directly inside the block receive the label. This keeps grouping explicit and reusable.

---

## 🌍 Internationalization Support

You can associate machine-readable error `code`s with your rules, enabling flexible localization or internationalization strategies.

```kotlin
val emailRule = fromPredicate("email", "invalid email", code = "invalid_email") {
    it.contains("@")
}

validate(User::email) {
    rule(emailRule)
}
```

The resulting `ValidationError` contains a `code` field:

```kotlin
ValidationError(
    path = "email",
    message = "invalid email",
    code = "invalid_email"
)
```

You can use this to:
- Map error codes to localized messages
- Track and test for specific rule failures
- Cleanly separate user-facing messages from logic

---

## 📦 Publishing

This library is lightweight and designed for easy reuse.  
You can include it in your own projects or publish it to Maven Central / JitPack.

---

## ⚖️ License

MIT
