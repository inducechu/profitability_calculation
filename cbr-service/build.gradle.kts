plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.induce"
version = "0.0.1-SNAPSHOT"
description = "cbr-service"

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

    // Starters
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.grpc.server.starter)
    // Kafka
    implementation(libs.spring.boot.starter.kafka)

    // gRPC
    implementation(libs.grpc.services)
    implementation(libs.grpc.netty.shaded)
    modules {
        module("io.grpc:grpc-netty") {
            replacedBy("io.grpc:grpc-netty-shaded", "Use Netty shaded instead of regular Netty")
        }
    }

    implementation(libs.kotlin.reflect)

    // XML
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.xml)

    // Postgres
    runtimeOnly(libs.postgresql)

    // Discovery
    implementation(libs.spring.cloud.starter.netflix.eureka.client)

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
