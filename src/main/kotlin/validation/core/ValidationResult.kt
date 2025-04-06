package validation.core

sealed class ValidationResult<T> {
    data class Valid<T>(val value: T) : ValidationResult<T>()
    data class Invalid<T>(val errors: List<ValidationError>) : ValidationResult<T>()

    companion object {
        fun <T> from(value: T, errors: List<ValidationError>): ValidationResult<T> =
            if (errors.isEmpty()) Valid(value) else Invalid(errors)

        fun fromMany(errors: List<ValidationError>): ValidationResult<Unit> =
            if (errors.isEmpty()) Valid(Unit) else Invalid(errors)
    }

    fun isValid(): Boolean = this is Valid
    fun isInvalid(): Boolean = this is Invalid

    fun getOrNull(): T? = when (this) {
        is Valid -> value
        is Invalid -> null
    }

    fun getOrElse(default: () -> T): T = when (this) {
        is Valid -> value
        is Invalid -> default()
    }

    inline fun <R> map(transform: (T) -> R): ValidationResult<R> = when (this) {
        is Valid -> Valid(transform(value))
        is Invalid -> Invalid(errors)
    }

    inline fun <R> flatMap(transform: (T) -> ValidationResult<R>): ValidationResult<R> = when (this) {
        is Valid -> transform(value)
        is Invalid -> Invalid(errors)
    }

    fun onInvalid(block: (List<ValidationError>) -> Unit): ValidationResult<T> =
        also { if (this is Invalid) block(errors) }

    fun onValid(block: (T) -> Unit): ValidationResult<T> =
        also { if (this is Valid) block(value) }

}
