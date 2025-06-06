package validation.dsl.collection

import validation.core.combineResults
import validation.core.Validated
import validation.dsl.ValidationDsl
import validation.dsl.scopes.FieldValidationScope

@ValidationDsl
fun <T> FieldValidationScope<List<T>>.validateEach(
    block: FieldValidationScope<T>.() -> Unit
) {
    this.nested += {
        val parentList = this.root()
        val results = parentList.mapIndexed { index, item ->
            val itemPath = this.path.index(index)
            FieldValidationScope(itemPath) { item }.apply(block).evaluate()
        }
        combineResults(*results.toTypedArray()).toUnit()
    }
}
