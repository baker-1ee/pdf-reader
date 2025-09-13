plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.example"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        // 모든 서브프로젝트에 공통으로 적용할 의존성이 있다면 여기에 추가
    }
}
