plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.induce"
version = "0.0.1-SNAPSHOT"  
description = "investment-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(project(":common-proto"))
    // Spring Boot Starters
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.grpc.client.starter)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.cache)
    // Kafka
    implementation(libs.spring.boot.starter.kafka)


    // gRPC Client & Proto
    implementation(libs.grpc.netty.shaded)

    // Discovery
    implementation(libs.spring.cloud.starter.netflix.eureka.client)

    // Kotlin & JSON
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)

    // Database
    runtimeOnly(libs.postgresql)

    // Documentation
    implementation(libs.springdoc.openapi.webmvc)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.grpc.dependencies.get().toString())
        mavenBom(libs.spring.cloud.dependencies.get().toString())
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
