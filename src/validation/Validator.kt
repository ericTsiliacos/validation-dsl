package validation

import kotlin.reflect.KProperty1

class Validator<T> {
    private val rules = mutableListOf<(T) -> List<ValidationError>>()

    fun <R> validate(
        prop: KProperty1<T, R>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        val path = prop.name
        rules += { target ->
            val value = prop.get(target)
            FieldValidationScope(path) { value }.apply(block).evaluate()
        }
    }

    fun <R> validateEach(
        prop: KProperty1<T, List<R>>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        val path = prop.name
        rules += { target ->
            val list = prop.get(target)
            validateEachList(list, path, block)
        }
    }

    fun validate(target: T): ValidationResult {
        return ValidationResult(rules.flatMap { it(target) })
    }
}

fun <T> validator(block: Validator<T>.() -> Unit): Validator<T> =
    Validator<T>().apply(block)
