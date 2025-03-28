package validation

data class ValidationError(val path: String, val message: String)

class ValidationResult(val errors: List<ValidationError>) {
    val isValid: Boolean get() = errors.isEmpty()
}
