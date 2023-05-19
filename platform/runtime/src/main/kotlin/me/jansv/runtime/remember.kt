package me.jansv.runtime

inline fun <T> remember(calculation: () -> T): T = calculation()

