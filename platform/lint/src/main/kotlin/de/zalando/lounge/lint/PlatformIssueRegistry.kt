package de.zalando.lounge.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

// TODO Remove with LCFAP-9456. This should be set to CURRENT_API
private const val MIN_API = 13 // AGP 7.3 - 7.4

class PlatformIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API
    override val minApi = MIN_API

    override val issues: List<Issue>
        get() = listOf(
            InternalPlatformApiDetector.InternalPlatformApiUsage,
            InternalPlatformApiDetector.InternalPlatformApiAnnotationUsage,
        )

    override val vendor = Vendor(
        "Lounge Platform",
        "de.zalando.lounge.lint",
        feedbackUrl = "https://github.bus.zalan.do/LoungeMobile/android-app"
    )
}
