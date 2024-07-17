package me.jansv.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

abstract class AbstractViewModel(
    private val preconditions: UiPreconditions
) : ViewModel() {

    constructor(
        preconditions: UiPreconditions,
        savedStateHandle: SavedStateHandle,
    ) : this(preconditions) {
        println("Calling with ")
    }

    protected fun onUncaughtException(action: suspend (Throwable) -> Unit) {
        println("Launching block...")
    }
}
