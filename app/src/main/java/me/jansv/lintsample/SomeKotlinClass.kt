package me.jansv.lintsample

import de.zalando.lounge.annotations.InternalPlatformApi
import me.jansv.internallib.DefaultUtility
import me.jansv.internallib.UtilityClass
import javax.inject.Inject

@InternalPlatformApi
interface KotlinInterface

class SomeKotlinClass {
//    @Inject
//    val data: SuperService1 = SuperService1()

    @Inject
    val utilityClass: UtilityClass = DefaultUtility()

    @Inject
    lateinit var kotlinInterface: KotlinInterface

    @Inject
    lateinit var javaInterface: JavaInterface
}
