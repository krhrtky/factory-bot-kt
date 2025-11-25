package io.github.factorybot

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * Factory for building instances of type T with predefined attributes.
 *
 * @param T The type of object this factory creates
 * @param klass The KClass of the target type
 * @param name The factory name
 * @param parent Optional parent factory for inheritance
 */
class Factory<T : Any>(
    val klass: KClass<T>,
    val name: String,
    val parent: Factory<T>? = null
) {
    internal val attributes = mutableMapOf<String, AttributeDefinition<*>>()
    internal val traits = mutableMapOf<String, Trait<T>>()
    internal val afterBuildCallbacks = mutableListOf<(T) -> Unit>()
    internal val afterCreateCallbacks = mutableListOf<(T) -> Unit>()

    private val propertyCache: Map<String, KMutableProperty1<T, Any?>> by lazy {
        klass.memberProperties
            .filterIsInstance<KMutableProperty1<T, Any?>>()
            .associateBy { it.name }
    }

    /**
     * Builds an instance using the specified strategy and configuration.
     *
     * @param strategy The build strategy to use
     * @param traitNames List of trait names to apply
     * @param overrides Attribute overrides to apply
     * @return The built instance
     * @throws IllegalArgumentException If a trait is not found or property cannot be set
     */
    fun build(
        strategy: BuildStrategy = BuildStrategy.Build,
        traitNames: List<String> = emptyList(),
        overrides: Map<String, AttributeDefinition<*>> = emptyMap()
    ): T {
        val instance = klass.java.getDeclaredConstructor().newInstance()
        val context = EvaluationContext(strategy, name)

        val allAttributes = mutableMapOf<String, AttributeDefinition<*>>()

        parent?.let { parentFactory ->
            allAttributes.putAll(parentFactory.attributes)
        }

        allAttributes.putAll(attributes)

        traitNames.forEach { traitName ->
            val trait = traits[traitName]
                ?: throw IllegalArgumentException(
                    "Trait '$traitName' not found in factory '$name'. " +
                    "Available traits: ${traits.keys.joinToString()}"
                )
            allAttributes.putAll(trait.attributes)
        }

        allAttributes.putAll(overrides)

        allAttributes.forEach { (name, definition) ->
            val value = definition.evaluator(context)
            setProperty(instance, name, value)
        }

        val allAfterBuildCallbacks = mutableListOf<(T) -> Unit>()
        parent?.afterBuildCallbacks?.let { allAfterBuildCallbacks.addAll(it) }
        allAfterBuildCallbacks.addAll(afterBuildCallbacks)
        traitNames.forEach { traitName ->
            traits[traitName]?.afterBuildCallbacks?.let { allAfterBuildCallbacks.addAll(it) }
        }

        val allAfterCreateCallbacks = mutableListOf<(T) -> Unit>()
        parent?.afterCreateCallbacks?.let { allAfterCreateCallbacks.addAll(it) }
        allAfterCreateCallbacks.addAll(afterCreateCallbacks)
        traitNames.forEach { traitName ->
            traits[traitName]?.afterCreateCallbacks?.let { allAfterCreateCallbacks.addAll(it) }
        }

        allAfterBuildCallbacks.forEach { it(instance) }

        when (strategy) {
            BuildStrategy.Create -> allAfterCreateCallbacks.forEach { it(instance) }
            else -> {}
        }

        return instance
    }

    private fun setProperty(instance: T, propertyName: String, value: Any?) {
        val property = propertyCache[propertyName]
            ?: throw IllegalArgumentException(
                "Property '$propertyName' not found on ${klass.simpleName}. " +
                "Available properties: ${propertyCache.keys.joinToString()}"
            )
        try {
            property.set(instance, value)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to set property '$propertyName' on ${klass.simpleName}: ${e.message}",
                e
            )
        }
    }
}
