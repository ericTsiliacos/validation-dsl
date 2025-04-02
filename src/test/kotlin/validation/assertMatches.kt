package validation

import org.testng.AssertJUnit.*

fun ValidationError.assertMatches(path: String, message: String, code: String? = null) {
    assertEquals(path, this.path)
    assertEquals(message, this.message)
    assertEquals(code, this.code)
}

inline fun <T> Validated<T>.assertInvalid(block: (List<ValidationError>) -> Unit) {
    assertTrue(this is Validated.Invalid)
    block((this as Validated.Invalid).errors)
}

fun <T> Validated<T>.assertValid() {
    assertTrue(this is Validated.Valid)
}

fun <T> fieldScope(path: String, value: T, block: FieldValidationScope<T>.() -> Unit): Validated<Unit> {
    val scope = FieldValidationScope(path) { value }
    scope.block()
    return scope.evaluate()
}

fun ValidationResult.toValidated(): Validated<Unit> =
    if (isValid) Validated.Valid(Unit) else Validated.Invalid(errors)

fun ValidationResult.assertValid() {
    assertTrue(this.isValid)
}

fun ValidationResult.assertInvalid(block: (List<ValidationError>) -> Unit) {
    assertFalse(this.isValid)
    block(this.errors)
}
