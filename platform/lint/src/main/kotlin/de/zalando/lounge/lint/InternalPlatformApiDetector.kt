package de.zalando.lounge.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UClass

class InternalPlatformApiDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses() = listOf("java.lang.Object")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.isInPlatformModule) {
            return
        }
        checkAnnotationUsage(context, declaration)
        checkFieldDeclaration(context, declaration)
    }

    private fun checkAnnotationUsage(context: JavaContext, declaration: UClass) {
        val annotation = declaration.annotations.find {
            it.qualifiedName == InternalPlatformApiFqn
        } ?: return

        context.report(
            InternalPlatformApiAnnotationUsage,
            declaration,
            context.getLocation(annotation),
            "Only platform classes can be annotated with `InternalPlatformApi`."
        )
    }

    private fun checkFieldDeclaration(context: JavaContext, declaration: UClass) {
        declaration.fields.forEach loop@{ field ->
            // Optimistically most usages will be in the context of field injection. This filtering
            // seeks to fasten up processing when is the case. This can be removed if needed later on.
            if (!field.hasAnnotation(InjectFqn)) return@loop

            val fieldType = (field.type as? PsiClassReferenceType)?.resolve() ?: return@loop

            if (fieldType.hasAnnotation(InternalPlatformApiFqn) && fieldType.isInPlatformModule) {
                context.report(
                    InternalPlatformApiUsage,
                    field,
                    context.getLocation(field),
                    "`${fieldType.name}` is a platform internal API."
                )
            }
        }
    }

    private inline val PsiClass.isInPlatformModule
        get() = PlatformModulesDir in containingFile.virtualFile.path

    private inline val UClass.isInPlatformModule
        get() = PlatformModulesDir in javaPsi.containingFile.virtualFile.path

    companion object {
        private const val PlatformModulesDir = "platform"
        private const val InjectFqn =
            "javax.inject.Inject"
        private const val InternalPlatformApiFqn =
            "de.zalando.lounge.annotations.InternalPlatformApi"

        val InternalPlatformApiUsage = Issue.create(
            id = "InternalPlatformApiUsage",
            briefDescription = "Using platform internal API",
            explanation = "`InternalPlatformApi` annotated types are meant to be used in " +
                    ":platform modules alone. Types annotated with `InternalPlatformApi` usually " +
                    "represent interfaces for which implementation must be provided from modules outside " +
                    "the platform domain. Using these interfaces in :app module for example is discouraged.",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                InternalPlatformApiDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )

        val InternalPlatformApiAnnotationUsage = Issue.create(
            id = "InternalPlatformApiAnnotationUsage",
            briefDescription = "Using `InternalPlatformApi` on a non-platform module",
            explanation = "`PlatformInternalApi` is meant to be used only on :platform modules",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                InternalPlatformApiDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
