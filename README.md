# ✨ validation-dsl

[![Build](https://github.com/ericTsiliacos/validation-dsl/actions/workflows/ci.yml/badge.svg)](https://github.com/ericTsiliacos/validation-dsl/actions)  
[![codecov](https://codecov.io/gh/ericTsiliacos/validation-dsl/branch/main/graph/badge.svg)](https://codecov.io/gh/ericTsiliacos/validation-dsl)  
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight, expressive, and composable validation DSL for Kotlin.  
Perfect for validating nested data structures with readable, declarative syntax.

---

## 🌟 Features

- Intuitive DSL for nested object and list validation
- Reusable atomic rules with contextual path injection
- Short-circuiting rule chains via `chain { }`
- Nullable-aware validation with `whenNotNull`
- Reuse validators with `use(...)`
- Error grouping and code tags for UI/i18n
- Functional-style result types
- No reflection-based magic — everything is testable and explicit

---

## 🚀 Getting Started

```kotlin
data class User(val name: String, val tags: List<Tag>)
data class Tag(val value: Int)

val validator = validator<User> {
    validate(User::name) {
        rule("must not be blank") { it.isNotBlank() }
    }

    validateEach(User::tags) {
        validate(Tag::value) {
            rule("must not be negative") { it >= 0 }
        }
    }
}

val result = validator.validate(User(name = "", tags = listOf(Tag(-1), Tag(-1))))

if (!result.isValid) {
    result.errors.forEach {
        println("${it.path}: ${it.message}")
    }
}
```

---

## 🌱 Root Level Validation

Validate the entire object when you need cross-field checks:

```kotlin
val orderValidator = validator<Order> {
    root {
        rule("total must match sum") { it.total == it.items.sumOf { i -> i.price } }
    }
}
```

---

## 🧱 Reusable Rules

```kotlin
val notBlank = rule<String>("must not be blank") { it.isNotBlank() }

validate(User::username) {
    rule(notBlank)
}
```

---

## 🔁 Reusing Validators

```kotlin
val tagValidator = validator<Tag> {
    validate(Tag::value) {
        rule("must be ≥ 0") { it >= 0 }
    }
}

val userValidator = validator<User> {
    validateEach(User::tags) {
        use(tagValidator)
    }
}
```

---

## 🧠 Rule Chaining

Use `chain { }` to short-circuit dependent validations:

```kotlin
validate(User::age) {
    chain {
        rule("must be numeric") { it.all(Char::isDigit) }
        rule("must be ≥ 18") { it.toInt() >= 18 }
    }
}
```

---

## 📊 List Validation

```kotlin
validateEach(User::emails) {
    rule("must contain @") { it.contains("@") }
}
```

---

## ❓ Nullable Fields

```kotlin
validate(User::nickname) {
    whenNotNull {
        rule("must be at least 3 characters") { it.length >= 3 }
    }
}
```

Also supported:

```kotlin
ruleIfPresent("must be at least 3 characters") { it.length >= 3 }
```

---

## 🚫 Inverting Rules with `isForbidden`

```kotlin
val mustBeRed = rule<String>("must be red") { it == "red" }
val mustNotBeRed = mustBeRed.isForbidden("red not allowed")

validate(Item::color) {
    rule(mustNotBeRed)
}
```

---

## 🧪 Testability

Every rule returns a `Validated<Unit>`, making it easy to test in isolation:

```kotlin
val nonEmpty = rule<String>("must not be blank") { it.isNotBlank() }

assertEquals(Validated.Valid(Unit), nonEmpty("hello"))
assertTrue(nonEmpty("") is Validated.Invalid)
```

---

## 🧹 Grouping and Error Codes

Attach a group label for better UI/debugging, and an error code for i18n:

```kotlin
validate(User::name) {
    group("basic") {
        rule("must not be blank", code = "required") { it.isNotBlank() }
        rule("min 3 chars", code = "too_short") { it.length >= 3 }
    }
}
```

Each error will include the group and code:

```text
[group=basic] name: must not be blank (code=required)
```

---

## 🌍 i18n Support via Codes

Rules can have machine-readable codes:

```kotlin
val emailRule = rule<String>("invalid email", code = "invalid_email") {
    it.contains("@")
}
```

---

## 🏗️ Full Example

```kotlin
data class Address(val street: String, val zip: String)
data class Registration(val username: String, val password: String, val address: Address)

val strongPassword = rule<String>(
    "must be at least 8 chars and contain a digit", code = "weak_password"
) {
    it.length >= 8 && it.any(Char::isDigit)
}

val addressValidator = validator<Address> {
    group("address") {
        validate(Address::street) { rule("required") { it.isNotBlank() } }
        validate(Address::zip) {
            rule("invalid zip", code = "invalid_zip") { it.all(Char::isDigit) }
        }
    }
}

val registrationValidator = validator<Registration> {
    validate(Registration::username) { rule("required") { it.isNotBlank() } }
    validate(Registration::password) { rule(strongPassword) }
    validate(Registration::address) { use(addressValidator) }
}
```

---

## 📦 Publishing

This library has no dependencies and is suitable for use in any Kotlin project.  
You can publish it via Maven Central, JitPack, or include it locally.

---

## ⚖️ License

MIT
