package validation.dsl

import validation.core.*
import kotlin.reflect.KProperty1

@ValidationDsl
class Validator<T> {
    private val validations = mutableListOf<(T) -> Validated<Unit>>()

    fun <R> validate(
        prop: KProperty1<T, R>,
        block: FieldValidationScope<R>.() -> Unit
    ) {
        validations += { target ->
            val value = prop.get(target)
            FieldValidationScope(prop.name, { value }).apply(block).evaluate()
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
                    val path = "${prop.name}[$index]"
                    FieldValidationScope(path, { item }).apply(block).evaluate()
                }.toTypedArray()
            ).toUnit()
        }
    }

    fun validate(target: T): ValidationResult = validations.map { it(target) }
        .let(ValidationResult.Companion::fromMany)
}
