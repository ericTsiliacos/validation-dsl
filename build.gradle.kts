plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "io.github.ericTsiliacos"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.testng:testng:7.8.0")
    testImplementation("org.slf4j:slf4j-nop:2.0.17")
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.test {
    useTestNG()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "validation-dsl"
            version = version
        }
    }
}
