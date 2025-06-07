# Spring Boot Example

This minimal project demonstrates how to use the `validation-dsl` library in a Kotlin Spring Boot application.

It includes a `UserValidator` that validates incoming requests in `UserController`.

To build the project, run Gradle inside this directory:

```bash
./gradlew bootRun
```

The Gradle wrapper jar isn't included in version control, so the first run may
download it automatically.

The build uses a composite build to reference the parent `validation-dsl` module.
