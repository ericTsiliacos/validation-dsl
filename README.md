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
val notBlank = Rules.fromPredicate<String>("username", "must not be blank") { it.isNotBlank() }

validate(User::username) {
    rule(notBlank)
}
```

---

## 🎯 Composing Rules

Use `combine` for parallel evaluation, or `andThen` for dependent chaining:

```kotlin
val numeric = Rules.fromPredicate<String>("age", "must be numeric") { it.all(Char::isDigit) }
val over18 = Rules.fromPredicate<String>("age", "must be ≥ 18") { it.toInt() >= 18 }

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
val rule: Rule<String> = Rules.fromPredicate("name", "must not be blank") { it.isNotBlank() }

assertEquals(Validated.Valid(Unit), rule("valid"))
assertTrue(rule("") is Validated.Invalid)
```

---

## 📦 Publishing

This library is lightweight and designed for easy reuse.  
You can include it in your own projects or publish it to Maven Central / Jitpack.

---

## ⚖️ License

MIT
