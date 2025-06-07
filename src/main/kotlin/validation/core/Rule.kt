package validation.core

typealias Rule<T> = (T) -> Validated<Unit>
