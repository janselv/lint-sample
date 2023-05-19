package me.jansv.runtime

interface State<T> {
    val value: T
}

interface MutableState<T> : State<T> {
    override var value: T
}

interface ProducingStateScope<T> : MutableState<T>

fun <T> producingState(initialValue: T, producer: ProducingStateScope<T>.() -> Unit): State<T> {
    val scope = object : ProducingStateScope<T> {
        override var value: T = initialValue
    }
    scope.producer()
    return scope
}

// this is here just to test that we exclude usages of producingState in the lint analysis
// for usages inside this same package
fun producingStateSample() {
    val state = producingState(9) { }
}