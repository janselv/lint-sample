package me.jansv.runtime

import kotlinx.coroutines.flow.Flow

/**
 * With this we'll mimic the behavior of this detector: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime-lint/src/main/java/androidx/compose/runtime/lint/ComposableFlowOperatorDetector.kt
 * In which we won't allow flow method operators withing this method
 */
fun restrictedFlowCalls(block: () -> Unit) = Unit

fun <T> Flow<T>.someNiceExtension() = this
