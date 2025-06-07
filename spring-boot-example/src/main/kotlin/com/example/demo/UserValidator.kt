package com.example.demo

import validation.dsl.validator
import validation.dsl.validate
import validation.dsl.rule

val userValidator = validator<UserDto> {
    validate(UserDto::username) {
        rule("must not be blank") { it.isNotBlank() }
    }
    validate(UserDto::email) {
        rule("must contain @") { it.contains("@") }
    }
    validate(UserDto::password) {
        rule("must be at least 8 chars") { it.length >= 8 }
    }
}
