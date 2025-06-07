package com.example.demo

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import validation.core.Validated
import validation.core.toValidated

@RestController
@RequestMapping("/users")
class UserController {

    @PostMapping
    fun createUser(@RequestBody user: UserDto): ResponseEntity<Any> {
        val result = userValidator.validate(user).toValidated()
        return if (result is Validated.Invalid) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.errors)
        } else {
            ResponseEntity.ok("OK")
        }
    }
}
