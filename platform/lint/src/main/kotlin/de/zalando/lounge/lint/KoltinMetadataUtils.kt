package de.zalando.lounge.lint

import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.ClassUtil
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmFunction
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.signature


const val KotlinMetadataFqn = "kotlin.Metadata"

/**
 * Taken from https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/lint/common/src/main/java/androidx/compose/lint/KotlinMetadataUtils.kt
 */
fun PsiMethod.toKmFunction(): KmFunction? =
    containingClass?.getKmDeclarationContainer()?.findKmFunctionForPsiMethod(this)

/**
 * The statement in the original document of there's no built-in support for reading
 * kotlin.Metadata refers to built-in support from jetbrains PSI to return a kotlin.Metadata
 * for a compiled PSI.
 */
private fun PsiClass.getKmDeclarationContainer(): KmDeclarationContainer? {
    val classKotlinMetadataPsiAnnotation = annotations.find {
        it.qualifiedName == KotlinMetadataFqn
    } ?: return null

    val metadata = KotlinClassMetadata.read(classKotlinMetadataPsiAnnotation.toMetadataAnnotation())
        ?: return null

    return when (metadata) {
        is KotlinClassMetadata.Class -> metadata.toKmClass()
        is KotlinClassMetadata.FileFacade -> metadata.toKmPackage()
        is KotlinClassMetadata.SyntheticClass -> null
        is KotlinClassMetadata.MultiFileClassFacade -> null
        is KotlinClassMetadata.MultiFileClassPart -> metadata.toKmPackage()
        is KotlinClassMetadata.Unknown -> null
    }
}

private fun PsiAnnotation.toMetadataAnnotation(): Metadata {
    val attributes = attributes.associate { it.attributeName to it.attributeValue }

    fun JvmAnnotationAttributeValue.parseString(): String =
        (this as JvmAnnotationConstantValue).constantValue as String

    fun JvmAnnotationAttributeValue.parseInt(): Int =
        (this as JvmAnnotationConstantValue).constantValue as Int

    fun JvmAnnotationAttributeValue.parseStringArray(): Array<String> =
        (this as JvmAnnotationArrayValue).values.map {
            it.parseString()
        }.toTypedArray()

    fun JvmAnnotationAttributeValue.parseIntArray(): IntArray =
        (this as JvmAnnotationArrayValue).values.map {
            it.parseInt()
        }.toTypedArray().toIntArray()

    val kind = attributes["k"]?.parseInt() ?: 1
    val metadataVersion = attributes["mv"]?.parseIntArray() ?: intArrayOf()
    val data1 = attributes["d1"]?.parseStringArray() ?: arrayOf()
    val data2 = attributes["d2"]?.parseStringArray() ?: arrayOf()
    val extraString = attributes["xs"]?.parseString().orEmpty()
    val packageName = attributes["pn"]?.parseString().orEmpty()
    val extraInt = attributes["xi"]?.parseInt() ?: 0

    return Metadata(
        kind = kind,
        metadataVersion = metadataVersion,
        data1 = data1,
        data2 = data2,
        packageName = packageName,
        extraString = extraString,
        extraInt = extraInt
    )
}


private fun KmDeclarationContainer.findKmFunctionForPsiMethod(method: PsiMethod): KmFunction? {
    val expectedName = method.name.substringBefore("-")
    val expectedSignature = ClassUtil.getAsmMethodSignature(method)

    return functions.find {
        it.name == expectedName && it.signature?.desc == expectedSignature
    }
}