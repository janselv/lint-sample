plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("com.android.lint")
}

dependencies {
    api(project(":platform:annotations"))
    api(project(":platform:runtime"))
    lintChecks(project(":platform:lint"))
}