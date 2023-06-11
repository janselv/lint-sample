package de.zalando.lounge.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class CustomIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API
    override val minApi = CURRENT_API

    override val issues: List<Issue>
        get() = listOf(
            InternalPlatformApiDetector.InternalPlatformApiUsage,
            InternalPlatformApiDetector.InternalPlatformApiAnnotationUsage,
            ProducingStateValueDetector.ProducingStateValueNotAssigned,
            RememberDetector.RememberReturnType,
            RestrictedFlowCallsDetector.FlowOperatorCalledWithinRestrictedFlow,
            ParametrizedDetector.ISSUE,
        )

    override val vendor = Vendor(
        "Lounge Platform",
        "de.zalando.lounge.lint",
        feedbackUrl = "https://github.bus.zalan.do/LoungeMobile/android-app"
    )
}
