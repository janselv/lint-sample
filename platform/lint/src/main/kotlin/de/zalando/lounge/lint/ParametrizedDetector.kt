package de.zalando.lounge.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UClass

class ParametrizedDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses() = listOf("java.lang.Object")

    override fun beforeCheckEachProject(context: Context) {
//        println(">>>>> About to check project: ${context.project.dir}: ${context.configuration.getOption(PlatformDirsOption)}")
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
//        println("---- Visiting ${declaration.qualifiedName} - ${context.project.dir}")
    }

    companion object {
        private val PlatformDirsOption = StringOption(
            "platform-dirs",
            "A directory or regex indicating platform modules",
            "platform",
            "Specify either a directory or a regex indicating the location of platform modules"
        )

        val ISSUE = Issue.create(
            "ParametrizedIssue",
            "Brief Description for ParametrizedIssue",
            "Large explanation for ParametrizedIssue",
            Category.CORRECTNESS,
            10,
            Severity.ERROR,
            Implementation(ParametrizedDetector::class.java, Scope.JAVA_FILE_SCOPE),
        ).setOptions(listOf(PlatformDirsOption))
    }
}
