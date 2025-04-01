package validation

typealias Rule<T> = (T) -> Validated<Unit>

object Rules {

    fun <T> fromPredicate(
        path: String,
        message: String,
        predicate: (T) -> Boolean
    ): Rule<T> = { value ->
        if (predicate(value)) {
            Validated.Valid(Unit)
        } else {
            Validated.Invalid(listOf(ValidationError(path, message)))
        }
    }

    infix fun <T> Rule<T>.andThen(next: Rule<T>): Rule<T> = { value ->
        this(value).flatMap { next(value) }
    }

    infix fun <T> Rule<T>.combine(other: Rule<T>): Rule<T> = { value ->
        val r1 = this(value)
        val r2 = other(value)
        combineResults(r1, r2).map { }
    }

}
