package me.jansv.lintsample

import androidx.lifecycle.SavedStateHandle
import me.jansv.viewmodel.AbstractViewModel
import me.jansv.viewmodel.SkipPreconditions
import me.jansv.viewmodel.SkipPreconditions2
import me.jansv.viewmodel.UiPreconditions
import java.io.Serializable

/**
 * This ViewModel don't call onUncaughtException and don't use SkipPreconditions
 */
class ViewModel1(preconditions: UiPreconditions) : Serializable, AbstractViewModel(preconditions) {
    // comment this to test and remove the body braces
    init {
        onUncaughtException { /* TODO */ }
    }
}

/**
 * This ViewModel call onUncaughtException and don't use SkipPreconditions
 */
class ViewModel2(preconditions: UiPreconditions) : AbstractViewModel(preconditions) {
    init {
        onUncaughtException {  }
    }
}

/**
 * This ViewModel don't call onUncaughtException and don't use SkipPreconditions
 */
class ViewModel3(
    preconditions: UiPreconditions,
    savedStateHandle: SavedStateHandle
) : AbstractViewModel(preconditions, savedStateHandle) {
    // comment this to test
    init {
        onUncaughtException { /* TODO */ }
    }
}


/**
 * This ViewModel call onUncaughtException and don't use SkipPreconditions
 */
class ViewModel4(
    preconditions: UiPreconditions,
    savedStateHandle: SavedStateHandle
) : AbstractViewModel(preconditions, savedStateHandle) {
    init {
        onUncaughtException { }
    }
}

/**
 * This ViewModel call onUncaughtException and don't use SkipPreconditions
 */
class ViewModel5(preconditions: UiPreconditions) : AbstractViewModel(preconditions) {
    fun foo() {
        onUncaughtException { }
    }
}

/**
 * This ViewModel call onUncaughtException and use SkipPreconditions
 */
class SkipViewModel1 : AbstractViewModel(SkipPreconditions)

/**
 * This ViewModel call onUncaughtException and use SkipPreconditions
 */
class SkipViewModel2(
    savedStateHandle: SavedStateHandle
) : AbstractViewModel(SkipPreconditions2(), savedStateHandle) {
    // comment this to test
    init {
        onUncaughtException { /* TODO */ }
    }
}


/**
 * This ViewModel call onUncaughtException and use SkipPreconditions - Object
 */
object SkipViewModel3 : AbstractViewModel(SkipPreconditions)

/**
 * This ViewModel call onUncaughtException and use SkipPreconditions
 */
class SkipViewModel4(
    preconditions: UiPreconditions,
    savedStateHandle: SavedStateHandle,
) : AbstractViewModel(preconditions, savedStateHandle) {
    // comment this to test
    init {
        onUncaughtException { /* TODO */ }
    }

    constructor(
        savedStateHandle: SavedStateHandle,
    ) : this(SkipPreconditions, savedStateHandle)
}

class SkipViewModel5(
    preconditions: SkipPreconditions,
    savedStateHandle: SavedStateHandle,
) : AbstractViewModel(preconditions, savedStateHandle) {
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : this(SkipPreconditions, savedStateHandle)
}

/**
 * This one should be flagged because as long as the parameter has static type of `UiPreconditions`
 * chances are that other precondition than SkipPreconditions can be injected.
 */
fun evaluatePreconditions() = SkipPreconditions
class SkipViewModel6(
    preconditions: UiPreconditions = evaluatePreconditions(),
    savedStateHandle: SavedStateHandle,
) : AbstractViewModel(preconditions, savedStateHandle) {
    // comment this to test
    init {
        onUncaughtException { /* TODO */ }
    }

    constructor(
        savedStateHandle: SavedStateHandle,
    ) : this(SkipPreconditions, savedStateHandle)
}

/**
 * This one should be flagged because as long as the primary constructor parameter has static type of
 * `UiPreconditions` chances are that other precondition than SkipPreconditions can be injected.
 *
 * Secondary constructors are irrelevant because they can never call super(...) directly. All they can
 * do is delegate to the primary, so if primary constructor's precondition parameter is UiPreconditions
 * chances are the class can be instantiated with a non SkipPreconditions.
 */
class SkipViewModel7(
    preconditions: UiPreconditions,
    savedStateHandle: SavedStateHandle,
) : AbstractViewModel(preconditions, savedStateHandle) {
    // comment this to test
    init {
        onUncaughtException { /* TODO */ }
    }

    constructor(
        savedStateHandle: SavedStateHandle,
    ) : this(SkipPreconditions, savedStateHandle)
}

class SkipViewModel8(
    savedStateHandle: SavedStateHandle,
) : AbstractViewModel(evaluatePreconditions(), savedStateHandle)
