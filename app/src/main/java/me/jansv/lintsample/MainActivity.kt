package me.jansv.lintsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.jansv.runtime.MutableState
import me.jansv.runtime.producingState
import me.jansv.runtime.remember
import me.jansv.runtime.restrictedFlowCalls
import me.jansv.runtime.someNiceExtension

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

/**
 * ------------------------ TESTING producingState { } ------------------------
 */


fun producingState() { }

fun testingProducingState() {
    val state1 = producingState(0) {
        val s = 90
        val r = if (s == 90) true else false
        value = 8
    }
    val state2 = producingState(0) {
        accepting(this)
    }
    producingState()
}

fun accepting(state: MutableState<Int>) { }

/**
 * ------------------------ TESTING remember { } ------------------------
 */

fun testingRemember() {
    val d = remember { 90 }
}

/**
 * ------------------------ TESTING flow method calls within restrictedFlowCalls ------------------------
 */

fun <T> Flow<T>.someExtension() = this

fun testingRestrictedFlowCalls() {
    restrictedFlowCalls {
//        flowOf(3).map { it * 2 }
//        flowOf(3).someExtension()
    }
}