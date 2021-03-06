package de.maibornwolff.domainchoreograph.core.processing.reflection

import com.squareup.kotlinpoet.ClassName

interface ReflectionType : WithAnnotations {
    val className: ClassName
    val superclass: ReflectionType?
    val interfaces: List<ReflectionType>
    val enclosedMethods: List<ReflectionExecutable>
    val companionType: ReflectionType?

    val simpleName: String
        get() = className.canonicalName.split(".").last()
}

interface ReflectionVariable : WithAnnotations {
    val name: String
    val type: ReflectionType
}

interface ReflectionExecutable : WithAnnotations {
    val name: String
    val parameters: List<ReflectionVariable>
    val returnType: ReflectionType
}

interface WithAnnotations {
    fun <T : Annotation> getAnnotation(annotation: Class<T>): T?

    fun <T : Annotation> getAnnotationTypeValue(annotation: Class<T>): ReflectionType? =
        getAnnotation(annotation)?.getAnnotationTypeValueAsReflectionType()

    fun <T : Annotation> hasAnnotation(annotation: Class<T>): Boolean {
        return getAnnotation(annotation) != null
    }
}

private fun Annotation.getAnnotationTypeValueAsReflectionType(): ReflectionType {
    val nameField = annotationClass
        .java
        .declaredMethods[0]
    return (nameField(this) as Class<*>).kotlin.asReflectionType()
}
