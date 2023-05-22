package me.jansv.lintsample

import kotlinx.coroutines.flow.Flow
import me.jansv.internallib.InternalPlatformClassImpl

class SomeKotlinClass {
//    @Inject
//    lateinit var data: InternalPlatformClass
//
    val utilityClass: InternalPlatformClassImpl = InternalPlatformClassImpl()
}


fun <T> Flow<T>.someExtension2() = Unit