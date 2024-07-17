package de.zalando.lounge.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class OnUncaughtExceptionDetectorTest {
    private val savedStateHandle = kotlin(
        """
        package androidx.lifecycle

        class SavedStateHandle
    """
    ).indented()

    private val abstractViewModel = kotlin(
        """
        package me.jansv.viewmodel
        import androidx.lifecycle.SavedStateHandle

        sealed interface UiPreconditions
        data object SkipPreconditions : UiPreconditions

        abstract class AbstractViewModel(preconditions: UiPreconditions) {
            constructor(
                preconditions: UiPreconditions, 
                savedStateHandle: SavedStateHandle,
            ): this(preconditions)
            
            protected fun onUncaughtException(action: suspend (Throwable) -> Unit) = Unit
        }
    """
    ).indented()

    @Test
    fun `should issue when using UiPreconditions and is not calling onUncaughtException`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1(p: UiPreconditions) : AbstractViewModel(p)
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expect(
                """
                src/ViewModel1.kt:3: Error: ViewModel1 must call onUncaughtException [MissingOnUncaughtExceptionInvocation]
                class ViewModel1(p: UiPreconditions) : AbstractViewModel(p)
                      ~~~~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/ViewModel1.kt line 3: Insert onUncaughtException call:
                @@ -3 +3
                - class ViewModel1(p: UiPreconditions) : AbstractViewModel(p)
                + class ViewModel1(p: UiPreconditions) : AbstractViewModel(p) { 
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                + }
            """.trimIndent()
            )
    }

    @Test
    fun `should not issue when using UiPreconditions and onUncaughtException is called from init`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1(p: UiPreconditions) : AbstractViewModel(p) {
                        init {
                            onUncaughtException { }
                        }
                    }
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should not issue when using UiPreconditions and onUncaughtException is called from a method`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1(p: UiPreconditions) : AbstractViewModel(p) {
                        fun observeExceptions() = onUncaughtException { }
                    }
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should not issue when providing SkipPreconditions argument`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1 : AbstractViewModel(SkipPreconditions)
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should not issue when using SkipPreconditions in constructor parameter`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1(p: SkipPreconditions) : AbstractViewModel(p)
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should issue when using SkipPreconditions but assigned to UiPreconditions`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    class ViewModel1(p: UiPreconditions = SkipPreconditions) : AbstractViewModel(p)
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expect(
                """
                src/ViewModel1.kt:3: Error: ViewModel1 must call onUncaughtException [MissingOnUncaughtExceptionInvocation]
                class ViewModel1(p: UiPreconditions = SkipPreconditions) : AbstractViewModel(p)
                      ~~~~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/ViewModel1.kt line 3: Insert onUncaughtException call:
                @@ -3 +3
                - class ViewModel1(p: UiPreconditions = SkipPreconditions) : AbstractViewModel(p)
                + class ViewModel1(p: UiPreconditions = SkipPreconditions) : AbstractViewModel(p) { 
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                + }
            """.trimIndent()
            )
    }

    @Test
    fun `should not issue when a default value expression evaluates to SkipPreconditions `() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    fun evaluatePreconditions() = SkipPreconditions
                    class ViewModel1(p: SkipPreconditions = evaluatePreconditions()) : AbstractViewModel(p)
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should not issue when an argument expression evaluates to SkipPreconditions`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.*

                    fun evaluatePreconditions() = SkipPreconditions
                    class ViewModel1 : AbstractViewModel(evaluatePreconditions())
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectClean()
    }

    @Test
    fun `should issue when providing SkipPreconditions through secondary constructor`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.* 
                    import androidx.lifecycle.SavedStateHandle
                
                    class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s) {
                        constructor(s: SavedStateHandle): this(SkipPreconditions, s)
                    }
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expect(
                """
                src/ViewModel1.kt:4: Error: ViewModel1 must call onUncaughtException [MissingOnUncaughtExceptionInvocation]
                class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s) {
                      ~~~~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
            )
    }

    @Test
    fun `should apply fix for long parameter multiline list with no body`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.* 
                    import androidx.lifecycle.SavedStateHandle
                    
                    interface Mixin1
                    interface Mixin2
                    class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : 
                        AbstractViewModel(p, s),
                        Mixin1,
                        Mixin2
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/Mixin1.kt line 6: Insert onUncaughtException call:
                @@ -9 +9
                -     Mixin2
                +     Mixin2 { 
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                + }
            """.trimIndent()
            )
    }

    @Test
    fun `should apply fix for long parameter list with no body`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.* 
                    import androidx.lifecycle.SavedStateHandle
                    
                    interface Mixin1
                    interface Mixin2
                    class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/Mixin1.kt line 6: Insert onUncaughtException call:
                @@ -6 +6
                - class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2
                + class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2 { 
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                + }
            """.trimIndent()
            )
    }

    @Test
    fun `should apply fix for long parameter list with body`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.* 
                    import androidx.lifecycle.SavedStateHandle
                    
                    interface Mixin1
                    interface Mixin2
                    class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2 { 
                        fun foo() = Unit
                    }
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/Mixin1.kt line 6: Insert onUncaughtException call:
                @@ -6 +6
                - class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2 {
                + class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : AbstractViewModel(p, s), Mixin1, Mixin2 {
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                +
            """.trimIndent()
            )
    }

    @Test
    fun `should apply fix for long parameter multiline list with body`() {
        lint()
            .files(
                savedStateHandle,
                abstractViewModel,
                kotlin(
                    """
                    import me.jansv.viewmodel.* 
                    import androidx.lifecycle.SavedStateHandle
                    
                    interface Mixin1
                    interface Mixin2
                    class ViewModel1(p: UiPreconditions, s: SavedStateHandle) : 
                        AbstractViewModel(p, s), 
                        Mixin1, 
                        Mixin2 
                    { 
                        fun foo() = Unit
                    }
                """
                ).indented()
            )
            .issues(OnUncaughtExceptionDetector.MissingOnUncaughtExceptionCall)
            .run()
            .expectErrorCount(1)
            .expectFixDiffs(
                """
                Fix for src/Mixin1.kt line 6: Insert onUncaughtException call:
                @@ -10 +10
                - {
                + {
                + init {
                + onUncaughtException { /* TODO */ }
                + }
                +
            """.trimIndent()
            )
    }
}
