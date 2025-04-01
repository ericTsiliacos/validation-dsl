# validation-dsl

A principled, functional Kotlin validation DSL built by [@ericTsiliacos](https://github.com/ericTsiliacos).  
Inspired by algebraic structures like Functor, Applicative, and Monad — and designed for clarity and composability.

---

## ✨ Features

- ✅ Declarative rule chaining
- 📚 Nested field and list validation
- ♻️ Reusable composable validators
- 🧠 Backed by `Validated<T>` core
- 🧪 Fully unit tested

---

## 🚀 Example Usage

```kotlin
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
