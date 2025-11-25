package io.github.factorybot

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for managing sequences with identity-based caching.
 */
object SequenceRegistry {
    private val sequences = ConcurrentHashMap<String, Sequence<*>>()

    /**
     * Gets an existing sequence or creates a new one.
     *
     * @param V The type of value the sequence generates
     * @param key The unique key for this sequence
     * @param generator Function to generate sequence values
     * @return The sequence instance
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> getOrCreate(key: String, generator: (Long) -> V): Sequence<V> {
        return sequences.getOrPut(key) { Sequence(generator) } as Sequence<V>
    }

    /**
     * Clears all registered sequences.
     */
    fun clear() {
        sequences.clear()
    }
}

/**
 * DSL builder for defining factory attributes, traits, and callbacks.
 *
 * @param T The type of object this builder creates
 * @param factory The factory being configured
 */
class FactoryBuilder<T : Any>(
    private val factory: Factory<T>
) {
    private var sequenceCounter = 0
    private val sequenceKeys = mutableMapOf<Int, String>()

    /**
     * Defines an attribute with lazy evaluation.
     *
     * @param V The type of value this attribute produces
     * @param name The attribute name
     * @param evaluator Function to compute the attribute value
     */
    fun <V> attribute(name: String, evaluator: (EvaluationContext) -> V) {
        factory.attributes[name] = AttributeDefinition(name, evaluator)
    }

    /**
     * Creates a sequence that generates unique values.
     *
     * @param V The type of value to generate
     * @param generator Function that takes a number and returns a value
     * @return The next value in the sequence
     */
    fun <V> sequence(generator: (Long) -> V): V {
        val generatorHash = System.identityHashCode(generator)
        val sequenceKey = sequenceKeys.getOrPut(generatorHash) {
            "${factory.name}:${factory.klass.simpleName}:seq${sequenceCounter++}"
        }
        val seq = SequenceRegistry.getOrCreate(sequenceKey, generator)
        return seq.next()
    }

    /**
     * Generates a value from a named global sequence.
     *
     * @param V The type of value to generate
     * @param sequenceName The name of the sequence
     * @return The next value from the named sequence
     * @throws IllegalArgumentException If the sequence is not found
     */
    inline fun <reified V> generate(sequenceName: String): V {
        return FactoryBot.nextSequence<V>(sequenceName)
    }

    /**
     * Builds an associated instance from another factory.
     *
     * @param A The type of associated object
     * @return A new instance of the associated type
     */
    inline fun <reified A : Any> association(): A {
        return FactoryBot.build()
    }

    /**
     * Defines a named trait with its own attributes and callbacks.
     *
     * @param name The trait name
     * @param block Configuration block for the trait
     */
    fun trait(name: String, block: TraitBuilder<T>.() -> Unit) {
        val trait = Trait<T>(name)
        val builder = TraitBuilder(trait)
        builder.block()
        factory.traits[name] = trait
    }

    /**
     * Registers a callback to run after building instances.
     *
     * @param callback Function to execute after build
     */
    fun afterBuild(callback: (T) -> Unit) {
        factory.afterBuildCallbacks.add(callback)
    }

    /**
     * Registers a callback to run after creating instances.
     *
     * @param callback Function to execute after create
     */
    fun afterCreate(callback: (T) -> Unit) {
        factory.afterCreateCallbacks.add(callback)
    }
}

/**
 * DSL builder for defining trait attributes and callbacks.
 *
 * @param T The type of object this trait applies to
 * @param trait The trait being configured
 */
class TraitBuilder<T : Any>(
    private val trait: Trait<T>
) {
    /**
     * Defines an attribute within this trait.
     *
     * @param V The type of value this attribute produces
     * @param name The attribute name
     * @param evaluator Function to compute the attribute value
     */
    fun <V> attribute(name: String, evaluator: (EvaluationContext) -> V) {
        trait.attributes[name] = AttributeDefinition(name, evaluator)
    }

    /**
     * Registers a callback to run after building instances with this trait.
     *
     * @param callback Function to execute after build
     */
    fun afterBuild(callback: (T) -> Unit) {
        trait.afterBuildCallbacks.add(callback)
    }

    /**
     * Registers a callback to run after creating instances with this trait.
     *
     * @param callback Function to execute after create
     */
    fun afterCreate(callback: (T) -> Unit) {
        trait.afterCreateCallbacks.add(callback)
    }
}
