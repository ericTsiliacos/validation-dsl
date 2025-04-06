package validation.core

import org.testng.AssertJUnit.*
import validation.dsl.FieldValidationScope

fun ValidationError.assertMatches(
    path: PropertyPath,
    message: String,
    code: String? = null,
    group: String? = null
) {
    assertEquals(this.path, path)
    assertEquals(this.message, message)
    if (code != null) assertEquals(this.code, code)
    if (group != null) assertEquals(this.group, group)
}

fun ValidationError.assertMatches(path: String, message: String, code: String? = null, group: String? = null) {
    assert(this.path.toString() == path) {
        "Expected path '$path' but got '${this.path}'"
    }
    assert(this.message == message) {
        "Expected message '$message' but got '${this.message}'"
    }
    if (code != null) {
        assert(this.code == code) {
            "Expected code '$code' but got '${this.code}'"
        }
    }
    if (group != null) {
        assert(this.group == group) {
            "Expected group '$group' but got '${this.group}'"
        }
    }
}

inline fun <T> Validated<T>.assertInvalid(block: (List<ValidationError>) -> Unit) {
    assertTrue(this is Validated.Invalid)
    block((this as Validated.Invalid).errors)
}

fun <T> Validated<T>.assertValid() {
    assertTrue(this is Validated.Valid)
}

fun <T> fieldScope(path: PropertyPath, value: T, block: FieldValidationScope<T>.() -> Unit): Validated<Unit> {
    val scope = FieldValidationScope(path) { value }
    scope.block()
    return scope.evaluate()
}

fun <T> ValidationResult<T>.toValidated(): Validated<Unit> = when (this) {
    is ValidationResult.Valid -> Validated.Valid(Unit)
    is ValidationResult.Invalid -> Validated.Invalid(errors)
}

fun <T> ValidationResult<T>.assertValid() {
    assertTrue("Expected result to be Valid, but was Invalid", this is ValidationResult.Valid)
}

fun <T> ValidationResult<T>.assertInvalid(block: (List<ValidationError>) -> Unit) {
    assertTrue("Expected result to be Invalid, but was Valid", this is ValidationResult.Invalid)
    if (this is ValidationResult.Invalid) {
        block(errors)
    }
}

