package de.zalando.lounge.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.singleConstructorCallOrNull
import org.jetbrains.kotlin.analysis.api.calls.symbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that detects whether an `AbstractViewModel` subclass calls
 * AbstractViewModel.onUncaughtException whenever it is using a precondition other than
 * `SkipPreconditions`.
 */
class OnUncaughtExceptionDetector : Detector(), SourceCodeScanner {
    override fun applicableSuperClasses() = listOf(AbstractViewModelFqn)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.isAbstractViewModel() || declaration.isSkippingPreconditions()) {
            return
        }
        if (!declaration.invokesOnUncaughtException(context)) {
            context.report(
                MissingOnUncaughtExceptionCall,
                declaration,
                context.getNameLocation(declaration),
                message = "`${declaration.name}` must call `onUncaughtException`",
                quickfixData = fixMissingOnUncaughtException(context, declaration)
            )
        }
    }

    private fun UClass.invokesOnUncaughtException(context: JavaContext): Boolean {
        var invokesOnUncaughtException = false
        accept(
            callExpressionVisitor { call ->
                if (call.methodName == OnUncaughtException) {
                    call.resolve()?.let {
                        invokesOnUncaughtException =
                            context.evaluator.isMemberInClass(it, AbstractViewModelFqn)
                    }
                }
                invokesOnUncaughtException
            },
        )
        return invokesOnUncaughtException
    }

    /**
     * Returns whether the evaluated class desires to skip preconditions.
     *
     * A class is skipping preconditions if the following holds:
     * 1. Is invoking super with a non-parameter expression that evaluates to `SkipPreconditions`.
     *    e.g `AbstractViewModel(SkipPreconditions, ...)`, `AbstractViewModel(getSkipPreconditions(), ...)`
     * 2. Is invoking super with a parameter expression with static type `SkipPreconditions`.
     *
     * Secondary constructors are ignored since they will eventually call the primary one and this will
     * call super. Having a preconditions parameter in the primary constructor with `UiPreconditions` type
     * implies that the class can be instantiated with a non `SkipPreconditions` which makes it fallible.
     *
     * **Note**: UAST will resolve the preconditions argument's type to a ClsClassImpl PsiElement.
     * Instead of resolving to the JVM compiled type we need to resolve the Kotlin type. If ClsClassImpl
     * were to be used to resolve it, it will require parsing the kotlin-metadata annotations which will
     * leak to the lint's consumer. Instead we use the [analyze] API to transparently resolve the Kotlin type.
     */
    private fun UClass.isSkippingPreconditions(): Boolean {
        // assume is skipping preconditions then try to disprove it.
        var isSkippingPreconditions = true

        methods.filter { it.isConstructor }.forEach { method ->
            method.analyzeSuperConstructorCall { call ->
                val constructor = call.resolveCall()
                    ?.singleConstructorCallOrNull()
                    ?.takeIf {
                        // is an AbstractViewModel constructor?
                        isAbstractViewModel(it.symbol.returnType)
                    }

                // find the argument for the preconditions parameter
                val argument = constructor?.argumentMapping?.entries
                    ?.find { (_, parameter) ->
                        isUiPreconditionsOrSubclass(parameter.returnType)
                    }
                    ?.key

                argument?.getKtType()?.let {
                    isSkippingPreconditions = isSkipPreconditions(it)
                }
            }
        }
        return isSkippingPreconditions
    }

    private inline fun UMethod.analyzeSuperConstructorCall(
        crossinline action: KtAnalysisSession.(KtSuperTypeCallEntry) -> Unit,
    ) = accept(
        callExpressionVisitor {
            val sourcePsi = it.sourcePsi
            if (sourcePsi is KtSuperTypeCallEntry) {
                analyze(sourcePsi) {
                    action(sourcePsi)
                }
            }
            sourcePsi is KtSuperTypeCallEntry
        },
    )

    private fun fixMissingOnUncaughtException(context: JavaContext, declaration: UClass): LintFix? {
        val sourcePsi = declaration.sourcePsi as? KtClass
        val insertLocation = sourcePsi?.getOnUncaughtExceptionInsertLocation(context) ?: return null

        return fix()
            .name("Insert onUncaughtException call")
            .replace()
            .range(insertLocation)
            .end()
            .with(sourcePsi.buildOnUncaughtExceptionCall())
            .reformat(true)
            .shortenNames()
            .build()
    }

    // if the class does not have a body, insert at the very end of its last supertype,
    // otherwise at the end of the l-brace.
    private fun KtClass.getOnUncaughtExceptionInsertLocation(context: JavaContext) =
        when (null != body) {
            true -> body?.lBrace?.let { context.getLocation(it) }
            else -> superTypeListEntries.lastOrNull()?.let { context.getLocation(it) }
        }

    private fun KtClass.buildOnUncaughtExceptionCall(addClassBodyBraces: Boolean = body == null) =
        buildString {
            if (addClassBodyBraces) append(" { ")
            appendLine()
            appendLine("init {")
            appendLine("onUncaughtException { /* TODO */ }")
            appendLine("}")
            if (addClassBodyBraces) append("}")
        }

    private inline fun callExpressionVisitor(
        crossinline action: (UCallExpression) -> Boolean,
    ) = object : AbstractUastVisitor() {
        override fun visitCallExpression(node: UCallExpression): Boolean {
            return action(node)
        }
    }

    private fun UClass.isAbstractViewModel() = qualifiedName == AbstractViewModelFqn

    private fun KtAnalysisSession.isAbstractViewModel(type: KtType) =
        fqn(type) == AbstractViewModelFqn

    private fun KtAnalysisSession.isUiPreconditions(type: KtType) =
        fqn(type) == UiPreconditionsFqn

    private fun KtAnalysisSession.isSkipPreconditions(type: KtType) =
        fqn(type) == SkipPreconditionsFqn

    private fun KtAnalysisSession.isUiPreconditionsOrSubclass(type: KtType) =
        isUiPreconditions(type) || type.getAllSuperTypes().any { isUiPreconditions(it) }

    private fun KtAnalysisSession.fqn(type: KtType) =
        type.expandedClassSymbol?.classIdIfNonLocal?.asFqNameString()

    companion object {
        private const val UiPreconditionsFqn = "me.jansv.viewmodel.UiPreconditions"
        private const val SkipPreconditionsFqn = "me.jansv.viewmodel.SkipPreconditions"
        private const val AbstractViewModelFqn = "me.jansv.viewmodel.AbstractViewModel"
        private const val OnUncaughtException = "onUncaughtException"

        val MissingOnUncaughtExceptionCall = Issue.create(
            id = "MissingOnUncaughtExceptionCall",
            briefDescription = "Missing `onUncaughtException` invocation.",
            explanation = "View models using a `UiPreconditions` other than `SkipPreconditions` " +
                    "must handle failures reported through `onUncaughtException`. `UiPreconditions` " +
                    "other than `SkipPreconditions` might be fallible, e.g network exceptions might arise " +
                    "as part of the preconditions check. `AbstractViewModel` subclasses should handle these errors " +
                    "accordingly.",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                OnUncaughtExceptionDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
