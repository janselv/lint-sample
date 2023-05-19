package de.zalando.lounge.annotations

/**
 * This annotation is used to enforce that certain type is meant to be used internally
 * in platform modules alone.
 *
 * An example of usage is when certain implementation is needed for a platform API meant to be used
 * internally in platform modules only. Outside usages of such API and their implementation
 * thereof is not advised to be used anywhere else, not even in the module providing the
 * implementation if is outside of platform scope.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class InternalPlatformApi
