plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("com.android.tools.lint:lint-api:31.0.1")
    compileOnly("com.android.tools.lint:lint-checks:31.0.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")
}

/**
 * Bundle implementation dependencies with the lint.jar
 */
tasks.withType(Jar::class.java) {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
