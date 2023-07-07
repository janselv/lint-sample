import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lint-sample"
include(":app")
include(":platform:lint")

include(":platform:annotations")
include(":platform:internallib")
include(":platform:runtime")

/**
 * Copy IDE live templates located in .idea/templates into the data directory of this AS.
 * Copy is attempted only while gradle sync/build happens from AS and when doing clean build.
 */
fun tryCopyLiveTemplates() = System.getProperty("idea.paths.selector")?.let { selector ->
    val isMacOS = System.getProperty("os.name", "").contains("mac", ignoreCase = true)
    val userHome = System.getProperty("user.home", "")

    if (isMacOS && userHome.isNotEmpty()) {
        val selectorFile = Paths.get(rootDir.absolutePath, "build/idea-version.txt")

        if (!selectorFile.exists() || selectorFile.readText() != selector) {
            val ideTemplatesPath =
                Paths.get(userHome, "/Library/Application Support/Google", selector, "templates")
            val gitTemplatesPath =
                Paths.get(rootDir.absolutePath, ".idea/templates")

            @OptIn(ExperimentalPathApi::class)
            gitTemplatesPath.copyToRecursively(
                ideTemplatesPath,
                followLinks = false,
                overwrite = true
            )

            selectorFile.parent.createDirectories()
            selectorFile.writeText(selector)
        }
    }
}
