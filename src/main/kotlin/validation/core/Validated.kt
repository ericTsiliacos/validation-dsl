package validation.core

sealed class Validated<out T> {
    data class Valid<T>(val value: T) : Validated<T>()
    data class Invalid(val errors: List<ValidationError>) : Validated<Nothing>()

    fun isValid(): Boolean = this is Valid
}

fun <A, B> Validated<A>.map(f: (A) -> B): Validated<B> = when (this) {
    is Validated.Valid -> Validated.Valid(f(this.value))
    is Validated.Invalid -> this
}

fun <A, B> Validated<(A) -> B>.ap(fa: Validated<A>): Validated<B> = when (this) {
    is Validated.Invalid -> when (fa) {
        is Validated.Invalid -> Validated.Invalid(this.errors + fa.errors)
        else -> this
    }

    is Validated.Valid -> when (fa) {
        is Validated.Valid -> Validated.Valid(this.value(fa.value))
        is Validated.Invalid -> fa
    }
}

fun <A, B> Validated<A>.flatMap(f: (A) -> Validated<B>): Validated<B> = when (this) {
    is Validated.Valid -> f(this.value)
    is Validated.Invalid -> this
}

fun <T> combineResults(vararg results: Validated<T>): Validated<List<T>> {
    val errors = results.filterIsInstance<Validated.Invalid>().flatMap { it.errors }
    return if (errors.isEmpty()) {
        Validated.Valid(results.filterIsInstance<Validated.Valid<T>>().map { it.value })
    } else {
        Validated.Invalid(errors)
    }
}
