package me.jansv.lintsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.jansv.runtime.MutableState
import me.jansv.runtime.producingState

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

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
