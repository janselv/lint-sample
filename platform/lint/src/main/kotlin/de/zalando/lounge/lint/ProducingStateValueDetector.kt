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
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

class ProducingStateValueDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("producingState")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInRuntimePackage) {
            val producer = node.valueArguments.find {
                node.getParameterForArgument(it)?.name == "producer"
            } ?: return

            var referencesReceiver = false
            var callsSetValue = false

            producer.accept(object : AbstractUastVisitor() {
                val mutableStatePsiClass = context.evaluator.findClass(MutableStateFqn)

                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val resolvedMethod = node.resolve() ?: return false
                    return resolvedMethod.parameterList.parameters.any { param ->
                        val type = param.type

                        if (InheritanceUtil.isInheritor(type, ProducingScopeFqn)) {
                            referencesReceiver = true
                        }

                        if (null != mutableStatePsiClass &&
                            context.evaluator.getTypeClass(type) == mutableStatePsiClass
                        ) {
                            referencesReceiver = true
                        }
                        referencesReceiver
                    }
                }

                override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                    if (node.identifier != "value") return false

                    val resolvedMethod = node.tryResolve() as? PsiMethod ?: return false
                    if (resolvedMethod.name == "setValue" &&
                        InheritanceUtil.isInheritor(resolvedMethod.containingClass, MutableStateFqn)) {
                        callsSetValue = true
                    }
                    return callsSetValue
                }
            })

            if (!callsSetValue && !referencesReceiver) {
                context.report(
                    ProducingStateValueNotAssigned,
                    node,
                    context.getNameLocation(node),
                    "producingState calls should assign `value` inside the producer lambda"
                )
            }
        }
    }

    private inline val PsiMethod.isInRuntimePackage
        get() = (containingFile as? PsiJavaFile)?.packageName == "me.jansv.runtime"

    companion object {
        private const val ProducingScopeFqn = "me.jansv.runtime.ProducingStateScope"
        private const val MutableStateFqn = "me.jansv.runtime.MutableState"

        val ProducingStateValueNotAssigned = Issue.create(
            "ProducingStateValueNotAssigned",
            "producingState should set `value` inside the producer lambda",
            "producingState returns an observable State using values assigned inside the producer",
            Category.CORRECTNESS, 9, Severity.ERROR,
            Implementation(
                ProducingStateValueDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
