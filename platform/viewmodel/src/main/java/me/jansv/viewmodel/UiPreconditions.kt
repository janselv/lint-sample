package me.jansv.viewmodel

sealed interface UiPreconditions

data object SkipPreconditions : UiPreconditions

class SkipPreconditions2 : UiPreconditions

class StandardUiPreconditions : UiPreconditions
