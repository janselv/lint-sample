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
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.psi.util.InheritanceUtil
import kotlinx.metadata.KmClassifier
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

class RestrictedFlowCallsDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("restrictedFlowCalls")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInRuntimePackage) {
            return
        }
        val block = node.valueArguments.find {
            node.getParameterForArgument(it)?.name == "block"
        } ?: return

        var callsFlowOperator = false
        var callExpression: UCallExpression? = null

        block.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val uMethod = node.resolveToUElement() as? UMethod ?: return false
                val receiverType = node.receiverType

                if (InheritanceUtil.isInheritor(receiverType, FlowFqn) && uMethod.isFlowOperator) {
                    callExpression = node
                    callsFlowOperator = true
                }
                return callsFlowOperator
            }
        })

        if (callsFlowOperator) {
            context.report(
                FlowOperatorCalledWithinRestrictedFlow,
                callExpression!!,
                context.getNameLocation(callExpression!!),
                "Flow operator functions should not be invoked within restrictedFlowCalls { }"
            )
        }
    }

    /**
     * This seems to work fine with only `InheritanceUtil.isInheritor` check
     */
    private inline val UMethod.isFlowOperator: Boolean get() {
        // if this method returns a flow
        if (InheritanceUtil.isInheritor(returnType, FlowFqn)) {
            return true
        }
        // this method is an extension on flow
        return when (val source = sourcePsi) {
            // Parsing a compiled class file
            is ClsMethodImpl -> {
                val kmFunction = source.toKmFunction()
                kmFunction?.receiverParameterType?.classifier == FlowClassifier
            }
            // Parsing Kotlin source
            is KtNamedFunction -> {
                val receiverType = source.receiverTypeReference?.toUElement() as? UTypeReferenceExpression
                receiverType?.getQualifiedName() == FlowFqn
            }
            else -> false
        }
    }

    private inline val PsiMethod.isInRuntimePackage
        get() = (containingFile as? PsiJavaFile)?.packageName == "me.jansv.runtime"

    companion object {
        const val FlowFqn = "kotlinx.coroutines.flow.Flow"
        val FlowClassifier = KmClassifier.Class("kotlinx/coroutines/flow/Flow")

        val FlowOperatorCalledWithinRestrictedFlow = Issue.create(
            "FlowOperatorCalledWithinRestrictedFlow",
            "Flow operator functions should not be invoked within restrictedFlowCalls { }",
            "Flow operator functions should not be invoked within restrictedFlowCalls { } " +
                    " such as restrictedFlowCalls { flowOf(2).map { ... } }",
            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            Implementation(
                RestrictedFlowCallsDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}