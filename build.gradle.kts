buildscript {
    val springBootVersion = project.property("springBootVersion").toString()
    val springDependencyManagementVersion = project.property("springDependencyManagementVersion").toString()

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
        classpath("io.spring.gradle:dependency-management-plugin:$springDependencyManagementVersion")
    }
}

plugins {
    java
    jacoco
    id("com.diffplug.spotless") version "8.12.0"
}

apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")

val springdocVersion = providers.gradleProperty("springdocVersion").get()
val mapstructVersion = providers.gradleProperty("mapstructVersion").get()
val javaVersion = providers.gradleProperty("javaVersion").get()

group = "ru.ticketflow-platform"
version = "0.0.1-SNAPSHOT"
description = "event-inventory-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
    }
}

repositories {
    mavenCentral()
}

spotless {
    java {
        target("src/*/java/**/*.java")
        eclipse("4.41").configProperties("""
            org.eclipse.jdt.core.formatter.tabulation.char=space
            org.eclipse.jdt.core.formatter.tabulation.size=4
            org.eclipse.jdt.core.formatter.indentation.size=4
            org.eclipse.jdt.core.formatter.continuation_indentation=1
            org.eclipse.jdt.core.formatter.continuation_indentation_for_array_initializer=1
            org.eclipse.jdt.core.formatter.lineSplit=120
            org.eclipse.jdt.core.formatter.join_wrapped_lines=false
            org.eclipse.jdt.core.formatter.insert_new_line_after_annotation_on_parameter=insert
            org.eclipse.jdt.core.formatter.insert_new_line_after_annotation_on_method=insert
            org.eclipse.jdt.core.formatter.alignment_for_annotations_on_parameter=49
            org.eclipse.jdt.core.formatter.alignment_for_arguments_in_annotation=16
            org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve=1
            org.eclipse.jdt.core.formatter.insert_space_before_opening_brace_in_array_initializer=insert
            org.eclipse.jdt.core.formatter.insert_space_after_opening_brace_in_array_initializer=insert
            org.eclipse.jdt.core.formatter.insert_space_before_closing_brace_in_array_initializer=insert
        """.trimIndent())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-jdbc")

    testCompileOnly("org.projectlombok:lombok")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testAnnotationProcessor("org.projectlombok:lombok")
}

sourceSets {
    test {
        resources {
            srcDir("src/test/java")
            include("application.yaml", "scenario/cases/sql/**/*.sql")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

val jacocoMainClasses = sourceSets.main.get().output.classesDirs

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(jacocoMainClasses)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(jacocoMainClasses.asFileTree.matching {
        exclude("ru/ticketflowplatform/eventinventoryservice/mapper/*MapperImpl.class")
    })
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
