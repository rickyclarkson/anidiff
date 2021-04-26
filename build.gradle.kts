import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
}
group = "me.ricky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("io.github.java-diff-utils:java-diff-utils:4.9")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.4")
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}