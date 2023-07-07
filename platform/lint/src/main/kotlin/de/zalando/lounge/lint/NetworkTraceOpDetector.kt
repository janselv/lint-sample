package de.zalando.lounge.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.DefaultPosition
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElement

/**
 * Detector that checks Retrofit annotated methods and diagnose either a missing `NetworkTraceOp` tag
 * or a wrong type on the tag parameter.
 */
class NetworkTraceOpDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitMethod(node: UMethod) {
            if (node.annotations.none { it.qualifiedName in HttpMethodAnnotationsFqn }) {
                return
            }
            val containsTraceTag = node.parameterList.parameters.any {
                it.hasAnnotation(TagAnnotationFqn) && InheritanceUtil.isInheritor(
                    it.type,
                    NetworkTraceOpFqn
                )
            }
            if (!containsTraceTag) {
                return context.report(
                    MissingNetworkTraceOpTag,
                    node,
                    context.getNameLocation(node),
                    message = "`${node.name}` is missing a `NetworkTraceOp` tag parameter",
                    quickfixData = fixMissingNetworkTraceOpTag(context, node)
                )
            }
            checkNetworkTraceOpTagParameterType(context, node)
        }
    }

    private fun checkNetworkTraceOpTagParameterType(context: JavaContext, method: UMethod) {
        val traceParameters = method.parameterList.parameters
            .filter {
                InheritanceUtil.isInheritor(it.type, NetworkTraceOpFqn)
            }
        traceParameters.forEach loop@{ param ->
            val paramTypeClass = context.evaluator.getTypeClass(param.type) ?: return@loop

            if (paramTypeClass.qualifiedName != NetworkTraceOpFqn) {
                val uParameter = param.toUElement() as UParameter

                val location = uParameter
                    .typeReference?.let {
                        context.getNameLocation(it)
                    } ?: context.getNameLocation(param)

                context.report(
                    WrongNetworkTraceOpParamType,
                    param,
                    location,
                    message = "Trace tag must have `NetworkTraceOp` type",
                    quickfixData = fixWrongNetworkTraceOpParameterType(location)
                )
            }
        }
    }

    /**
     * Add a `NetworkTraceOp` to a Retrofit method. This fix consider different method formatting
     * styles and shape such as:
     *
     * ```kotlin
     * @GET
     * fun method(): T
     *
     * @GET
     * fun method(
     * ): T
     *
     * @GET
     * fun method(url: String): T
     *
     * @GET
     * fun method(
     *  url: String, ...
     * ): T
     *
     * @GET
     * fun method(
     *  url: String,
     *  extra: String,
     *  ...
     * ): T
     * ```
     */
    private fun fixMissingNetworkTraceOpTag(context: JavaContext, method: UMethod): LintFix? {
        val paramsLocation = context.getLocation(method.parameterList)

        if (null == paramsLocation.start || null == paramsLocation.end)
            return null

        val insertLocation = when (!method.hasParameters()) {
            true -> {
                // location of parameterList starts before `(` and end after `)`.
                // We move a position backward to insert before `)` but after `(`.
                val start = requireNotNull(paramsLocation.start)
                val end = requireNotNull(paramsLocation.end)
                val insertColumn = if (end.column - 1 < 0) return null else end.column - 1
                Location.create(
                    file = paramsLocation.file,
                    start = start,
                    end = DefaultPosition(end.line, insertColumn, end.offset - 1),
                )
            }
            else -> context.getLocation(method.parameterList.parameters.last())
        }
        val isParameterListMultiline =
            method.isParameterListMultiline(context, paramsLocation)
        // honor parameter list's formatting and trailing `,`
        val traceParameter = method.getTraceParameter(isParameterListMultiline)

        return fix()
            .name("Insert a NetworkTraceOp tag")
            .replace()
            .range(insertLocation)
            .end()
            .with(traceParameter)
            .reformat(true)
            .shortenNames()
            .build()
    }

    private fun UMethod.getTraceParameter(isParameterListMultiline: Boolean) = buildString {
        if (hasParameters()) {
            // insert a `,` right after last parameter's type before any existing trailing `,`
            append(",")
        }
        // parameterless but multiline methods don't need the extra \n
        when (isParameterListMultiline && hasParameters()) {
            true -> append("\n")
            else -> append("")
        }
        append("@$TagAnnotationFqn tag: $NetworkTraceOpFqn")
        // parameterless but multiline methods need an extra \n after the trace op but before `)`
        if (isParameterListMultiline && !hasParameters())
            append("\n")
    }

    private fun UMethod.isParameterListMultiline(context: JavaContext, parametersLocation: Location) =
        when (!hasParameters() || parameterList.parametersCount == 1) {
            true -> !parametersLocation.isSingleLine()
            else -> {
                // check if all parameters are in the same line separated by \n
                parameterList.parameters
                    .mapTo(mutableSetOf()) {
                        context.getLocation(it).start?.line ?: 0
                    }.size != 1
            }
        }

    /**
     * Change the type of the trace tag parameter to be exactly `NetworkTraceOp`
     */
    private fun fixWrongNetworkTraceOpParameterType(location: Location) = fix()
        .name("Change to NetworkTraceOp")
        .replace()
        .range(location)
        .with(NetworkTraceOpFqn)
        .reformat(true)
        .shortenNames()
        .build()

    companion object {
        private val HttpMethodAnnotationsFqn = listOf(
            "retrofit2.http.GET",
            "retrofit2.http.POST",
            "retrofit2.http.PUT",
            "retrofit2.http.DELETE"
        )
        private const val TagAnnotationFqn = "retrofit2.http.Tag"
        private const val NetworkTraceOpFqn = "de.zalando.lounge.tracing.NetworkTraceOp"

        val MissingNetworkTraceOpTag = Issue.create(
            id = "MissingNetworkTraceOpTag",
            briefDescription = "Retrofit methods should have a NetworkTraceOp tag.",
            explanation = "Retrofit methods are required to have a tag of type `NetworkTraceOp` " +
                    "in order to get proper coverage on LightStep tracing. A `NetworkTraceOp` " +
                    "is used to set the network operation name and its group. A missing network " +
                    "trace tag won't cause the endpoint to be traced on LightStep but will lead " +
                    "to unexpected behavior such as creating a complete new operation group and " +
                    "assigning the URL path as operation name, which might change over time.",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                NetworkTraceOpDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )

        val WrongNetworkTraceOpParamType = Issue.create(
            id = "WrongNetworkTraceOpParamType",
            briefDescription = "The network trace op parameter must have `NetworkTraceOp` type.",
            explanation = "Retrofit network operation tags are looked up by their static types." +
                    "This means that statements such as `Request.tag(NetworkTraceOp::class)` won't " +
                    "consider tags set for `NetworkTraceOp` subclasses. Only method parameters with " +
                    "type `NetworkTraceOp` will be considered. E.g. `@Tag op: NetworkTraceOp = <subclass>` " +
                    "will be valid, but `@Tag op: NetworkTraceOpSubclass = <subclass>` won't.",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                NetworkTraceOpDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
