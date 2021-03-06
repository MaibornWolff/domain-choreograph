package de.maibornwolff.domainchoreograph.core.processing.dependencygraph

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import de.maibornwolff.domainchoreograph.core.api.*
import de.maibornwolff.domainchoreograph.core.processing.reflection.ReflectionExecutable
import de.maibornwolff.domainchoreograph.core.processing.reflection.ReflectionType
import de.maibornwolff.domainchoreograph.core.processing.reflection.ReflectionVariable

class ChoreographyDependencyError(msg: String)
    : RuntimeException("Error while resolving the Choreography Dependencies: $msg")

class DependencyGraph {
    private val nodeMapping: MutableMap<TypeName, DependencyNode> = mutableMapOf()
    private val nodeOrder: MutableList<DependencyNode> = mutableListOf()
    private var _target: DependencyNode? = null

    val target: DependencyNode
        get() = _target!!

    val nodes: List<DependencyNode>
        get() = nodeOrder

    internal fun addElement(node: DependencyNode) {
        if (nodeMapping.containsKey(node.type)) {
            return
        }
        nodeMapping[node.domainType] = node
        nodeOrder.add(node)
    }

    internal fun hasElement(type: TypeName): Boolean {
        return nodeMapping.containsKey(type)
    }

    internal fun getElement(type: TypeName): DependencyNode {
        return nodeMapping[type]!!
    }

    companion object {
        fun create(rootElement: ReflectionType, givenValues: List<ReflectionVariable>, unsafe: Boolean = false): DependencyGraph {
            val graph = DependencyGraph()

            givenValues.forEach {
                val typeElement = it.type
                graph.addElement(DependencyNode.VariableNode(
                    type = typeElement.className,
                    domainType = getDomainTypeOf(typeElement),
                    name = it.name
                ))
            }
            val node = getNodeFrom(rootElement, rootElement, graph, unsafe)
            graph._target = node
            return graph
        }
    }
}

private fun getNodeFrom(
    returnTypeElement: ReflectionType,
    definedTypeElement: ReflectionType,
    graph: DependencyGraph,
    unsafe: Boolean,
    caller: ReflectionType? = null
): DependencyNode {
    val domainType = getDomainTypeOf(definedTypeElement)
    val returnType = returnTypeElement.className
    if (graph.hasElement(domainType)) {
        return graph.getElement(domainType)
    }
    return if (returnTypeElement.isDomainChoreography()) {
        println(returnType)
        println(domainType)
        println(caller)
        val node = DependencyNode.ChoreographyNode(
            type = returnType,
            domainType = domainType,
            caller = caller!!.className
        )
        graph.addElement(node)
        node
    } else {
        var node: DependencyNode
        try {
            val (callerClass, domainMethod) = getDomainMethodOf(definedTypeElement)
            node = DependencyNode.FunctionNode(
                type = returnType,
                domainType = domainType,
                name = domainMethod.name,
                caller = callerClass.className,
                parameters = domainMethod.getParametersAsNodes(graph, callerClass, unsafe)
            )
        } catch (err: DomainException) {
            if (!unsafe) {
                throw err
            }
            node = DependencyNode.VariableNode(
                type = returnType,
                domainType = domainType,
                name = domainType.simpleName.decapitalize()
            )
        }
        graph.addElement(node)
        node
    }
}

private fun ReflectionExecutable.getParametersAsNodes(graph: DependencyGraph, caller: ReflectionType, unsafe: Boolean): List<DependencyNode> {
    return this.parameters
        .map { resolveParameterTypes(it) }
        .mapNotNull { (returnTypeElement, definedTypeElement) ->
            val type = getDomainTypeOf(definedTypeElement)
            if (graph.hasElement(type)) {
                graph.getElement(type)
            } else {
                getNodeFrom(returnTypeElement, definedTypeElement, graph, unsafe, caller)
            }
        }
}

private fun resolveParameterTypes(parameter: ReflectionVariable): Pair<ReflectionType, ReflectionType> {
    val definedBy = parameter.getAnnotationTypeValue(DefinedBy::class.java)
    val parameterType = parameter.type
    if (definedBy != null) {
        return Pair(parameterType, definedBy)
    }
    return Pair(parameterType, parameterType)
}

private fun getDomainTypeOf(element: ReflectionType): ClassName {
    if (element.isDomainChoreography()) {
        return element.className
    }

    val domainType = getDomainTypeOrNullOf(element)
    return domainType ?: throw ChoreographyDependencyError(
        "Missing @DomainDefinition or @DomainService on ${element.className}. " +
        "Make sure that the class itself or one of this superclasses uses the annotation."
    )
}

private fun getDomainTypeOrNullOf(element: ReflectionType): ClassName? {
    val superElements = sequenceOf(
        element.superclass,
        *element.interfaces.toTypedArray()
    ).filterNotNull()
    val domainDefinitions = superElements.mapNotNull { getDomainTypeOrNullOf(it) }.toMutableList()
    if (element.isDomainType()) {
        domainDefinitions.add(0, element.className)
    }
    return when {
        domainDefinitions.size > 1 -> throw ChoreographyDependencyError(
            "Only one @DomainDefinition or @DomainService is allowed in the inheritance hierarchy of a class. " +
                "Found forbidden annotation at ${domainDefinitions.joinToString(", ")}"
        )
        domainDefinitions.isNotEmpty() -> domainDefinitions[0]
        else -> null
    }
}

private fun ReflectionType.isDomainType() =
    hasAnnotation(DomainDefinition::class.java) || hasAnnotation(DomainService::class.java)

private fun getDomainMethodOf(element: ReflectionType): Pair<ReflectionType, ReflectionExecutable> {
    fun getAllDomainMethodsOf(element: ReflectionType): List<ReflectionExecutable> =
        element.enclosedMethods.filter { it.hasAnnotation(DomainFunction::class.java) }

    var domainMethods = getAllDomainMethodsOf(element)
    val companionType = element.companionType

    if (domainMethods.isEmpty() && companionType != null) {
        domainMethods = getAllDomainMethodsOf(companionType)
    }

    if (domainMethods.isEmpty()) {
        throw DomainException("No @DomainFunction defined for ${element.className}")
    }
    if (domainMethods.size > 1) {
        throw DomainException("Only one @DomainFunction allowed. Multiple @DomainFunctions are defined for ${element.className}")
    }

    return element to domainMethods[0]
}

private fun ReflectionType.isDomainChoreography() = hasAnnotation(DomainChoreography::class.java)
