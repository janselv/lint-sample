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
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement

class InternalPlatformApiDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses() = listOf("java.lang.Object")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.isInPlatformModule) {
            return
        }
        checkAnnotationUsage(context, declaration)
        checkFieldDeclaration(context, declaration)
        checkConstructorUsage(context, declaration)
    }

    private fun checkAnnotationUsage(context: JavaContext, declaration: UClass) {
        val annotation = declaration.annotations.find {
            it.qualifiedName == InternalPlatformApiFqn
        } ?: return

        context.report(
            InternalPlatformApiAnnotationUsage,
            annotation,
            context.getNameLocation(annotation),
            "Only platform classes can be annotated with `InternalPlatformApi`."
        )
    }

    private fun checkFieldDeclaration(context: JavaContext, declaration: UClass) {
        declaration.fields.forEach loop@{ field ->
            if (!field.hasAnnotation(InjectFqn)) return@loop

            val fieldType = context.evaluator.getTypeClass(field.type) ?: return@loop

            if (fieldType.hasAnnotation(InternalPlatformApiFqn) && fieldType.isInPlatformModule) {
                context.report(
                    InternalPlatformApiUsage,
                    field,
                    context.getNameLocation(field.typeReference ?: field),
                    "`${fieldType.name}` is a platform internal API.",
                )
            }
        }
    }

    private fun checkConstructorUsage(context: JavaContext, declaration: UClass) {
        val injectConstructor = declaration.constructors.find {
            it.hasAnnotation(InjectFqn)
        } ?: return

        injectConstructor.parameterList.parameters.forEach loop@{ param ->
            val paramType = context.evaluator.getTypeClass(param.type) ?: return@loop

            if (paramType.hasAnnotation(InternalPlatformApiFqn) && paramType.isInPlatformModule) {
                val location = (param.toUElement() as UParameter)
                    .typeReference?.let {
                        context.getNameLocation(it)
                    } ?: context.getNameLocation(param)

                context.report(
                    InternalPlatformApiUsage,
                    param,
                    location,
                    "`${paramType.name}` is a platform internal API.",
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
        // Optimistically most usages will be in the context injection. Filtering on Inject annotation
        // seeks to fasten up processing. This can be removed if needed later on.
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
