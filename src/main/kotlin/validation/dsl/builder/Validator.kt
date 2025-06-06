package validation.dsl.builder

import validation.dsl.scopes.FieldValidationScope
import validation.core.*
import kotlin.reflect.KProperty1

/**
 * Entry point for creating a [Validator] using a fluent DSL.
 */
fun <T> validator(block: Validator<T>.() -> Unit): Validator<T> =
    Validator<T>().apply(block)

@ValidationDsl
class Validator<T> {
    private val validations = mutableListOf<(T) -> Validated<Unit>>()

    @ValidationDsl
    fun root(block: FieldValidationScope<T>.() -> Unit) {
        validations += { target ->
            FieldValidationScope(PropertyPath.EMPTY) { target }.apply(block).evaluate()
        }
    }

    fun <R> validate(
        prop: KProperty1<T, R>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        validations += { target ->
            val value = prop.get(target)
            FieldValidationScope(PropertyPath(prop.name), { value }).apply(block).evaluate()
        }
    }

    fun <R> validateEach(
        prop: KProperty1<T, List<R>>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        validations += { target ->
            val list = prop.get(target)
            combineResults(
                *list.mapIndexed { index, item ->
                    val path = PropertyPath(prop.name).index(index)
                    FieldValidationScope(path, { item }).apply(block).evaluate()
                }.toTypedArray()
            ).toUnit()
        }
    }

    fun validate(target: T): ValidationResult<T> = validations.map { it(target) }
        .flatMap {
            when (it) {
                is Validated.Valid -> emptyList()
                is Validated.Invalid -> it.errors
            }
        }
        .let { ValidationResult.from(target, it) }

}

@ValidationDsl
fun <T> FieldValidationScope<T>.use(validator: Validator<T>) {
    nested += {
        when (val result = validator.validate(root())) {
            is ValidationResult.Valid -> Validated.Valid(Unit)
            is ValidationResult.Invalid -> Validated.Invalid(
                result.errors.map { error ->
                    val updatedPath = if (error.path == PropertyPath.EMPTY) {
                        path
                    } else {
                        path.child(error.path.toString())
                    }
                    error.copy(path = updatedPath)
                }
            )
        }
    }
}
