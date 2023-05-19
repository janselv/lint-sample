package de.zalando.lounge.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

class RememberDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("remember")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInRuntimePackage || !node.returnType.isVoidOrUnit) {
            return
        }

        val sourcePsi = node.sourcePsi
        // in practice it seemed this extra computation is not needed as the return type
        // was properly computed in all the cases this extra step seeked to cover.
        val isReallyUnit = when {
            node.typeArguments.singleOrNull().isVoidOrUnit -> {
                true
            }
            sourcePsi is KtCallExpression -> {
                val calculationParameterIndex = method.parameters.lastIndex
                val argument = node.getArgumentForParameter(calculationParameterIndex)?.sourcePsi
                if (argument is KtLambdaExpression) {
                    val lastExpr = argument.bodyExpression?.statements?.lastOrNull()
                    val lastExprType = lastExpr?.toUElementOfType<UExpression>()?.getExpressionType()
                    // the check null == lastExprType is not present in here:https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime-lint/src/main/java/androidx/compose/runtime/lint/RememberDetector.kt;l=67
                    // but without it, constructs like remember { } this fails because there's no last expression to begin with.
                    // do note that even without `isReallyUnit` extra check the detector work as expected.
                    node.getExpressionType() == lastExprType || null == lastExprType
                } else {
                    true
                }
            }
            else -> true
        }
        if (isReallyUnit) {
            context.report(
                RememberReturnType,
                node,
                context.getNameLocation(node),
                "`remember` calls must not return `Unit`"
            )
        }
    }

    private inline val PsiMethod.isInRuntimePackage
        get() = (containingFile as? PsiJavaFile)?.packageName == "me.jansv.runtime"

    private inline val PsiType?.isVoidOrUnit
        get() =
            this == PsiType.VOID || this?.canonicalText == "kotlin.Unit"

    companion object {
        val RememberReturnType = Issue.create(
            "RememberReturnType",
            "`remember` must not return `Unit`",
            "A call to `remember` that returns `Unit` is always an error. calls like remember { Unit } are not allowed",
            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            Implementation(
                RememberDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}