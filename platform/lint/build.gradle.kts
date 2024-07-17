plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("com.android.tools.lint:lint-api:31.5.1")
    compileOnly("com.android.tools.lint:lint-checks:31.5.1")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.android.tools.lint:lint-api:31.5.1")
    testImplementation("com.android.tools.lint:lint-tests:31.5.1")
}

/**
 * Bundle implementation dependencies with the lint.jar — Used to bundle kotlinx-metadata with it.
 */
tasks.withType<Jar>{
    dependsOn(tasks.withType<Test>())

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
