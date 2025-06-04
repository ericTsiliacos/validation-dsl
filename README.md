# ✨ validation-dsl

[![Build](https://github.com/ericTsiliacos/validation-dsl/actions/workflows/ci.yml/badge.svg)](https://github.com/ericTsiliacos/validation-dsl/actions)
[![codecov](https://codecov.io/gh/ericTsiliacos/validation-dsl/branch/main/graph/badge.svg)](https://codecov.io/gh/ericTsiliacos/validation-dsl)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A lightweight, expressive, and composable validation DSL for Kotlin.  
Perfect for validating nested data structures with readable, declarative syntax.

---

## 🌟 Features

- Intuitive DSL for nested and list validation
- Root level & cross-field checks
- Reusable rules and powerful combinators
- Nullable field helpers and optional rules
- Dependent rule chaining (short-circuiting)
- Reuse validators with `use`
- Group labels & error codes for i18n
- Functional-style result types
- Zero dependencies and fully testable

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

You can also run a rule **only when the value is present** using `ruleIfPresent`:

```kotlin
validate(User::nickname) {
    ruleIfPresent("must be at least 3 chars") { it.length >= 3 }
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

## ♻️ Reusing Validators

Delegate validation of nested objects to standalone validators:

```kotlin
val tagValidator = validator<Tag> {
    validate(Tag::value) { rule("must not be blank") { it.isNotBlank() } }
}

val userValidator = validator<User> {
    validateEach(User::tags) { use(tagValidator) }
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

Other helpers include `isForbidden` to invert a rule and `fromFunction` to adapt
existing checks:

```kotlin
val onlyRed = fromPredicate(PropertyPath("color"), "must be red") { it == "red" }
val noRed = onlyRed.isForbidden("red not allowed")
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

## 🏗️ Mini Tutorial

The following example shows how to build a small validation module for a registration form. It demonstrates custom reusable rules, grouping for i18n, and composing validators.

```kotlin
data class Address(val street: String, val city: String, val zip: String)
data class Registration(
    val username: String,
    val email: String,
    val password: String,
    val address: Address
)

// reusable rule with an error code
val strongPassword = fromPredicate(
    "password",
    "must be at least 8 chars and contain a digit",
    code = "weak_password"
) {
    it.length >= 8 && it.any(Char::isDigit)
}

// group all address checks under the "address" label
val addressValidator = validator<Address> {
    group("address") {
        validate(Address::street) { rule("required") { it.isNotBlank() } }
        validate(Address::zip) {
            rule("invalid zip", code = "invalid_zip") { it.all(Char::isDigit) }
        }
    }
}

// compose the registration validator
val registrationValidator = validator<Registration> {
    validate(Registration::username) { rule("must not be blank") { it.isNotBlank() } }
    validate(Registration::password) { rule(strongPassword) }
    validate(Registration::address) { use(addressValidator) }
}

val result = registrationValidator.validate(
    Registration("js", "john@example.com", "pass", Address("", "", "ABCD"))
)

for (error in result.errors) {
    println("[${error.group}] ${error.path}: ${error.message} (${error.code})")
}
```

This prints something like:

```
[address] street: required (null)
[address] zip: invalid zip (invalid_zip)
password: must be at least 8 chars and contain a digit (weak_password)
```

The snippet illustrates how to define custom rules with codes, group related validations, and compose validators for nested structures.

---

## 📦 Publishing

This library is lightweight and designed for easy reuse.  
You can include it in your own projects or publish it to Maven Central / JitPack.

---

## ⚖️ License

MIT
