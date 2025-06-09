package validation.dsl

import validation.core.*
import kotlin.reflect.KProperty1

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
            val results = list.mapIndexed { index, item ->
                val path = PropertyPath(prop.name).index(index)
                FieldValidationScope(path) { item }.apply(block).evaluate()
            }
            combineResultsFromList(results).toUnit()
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
