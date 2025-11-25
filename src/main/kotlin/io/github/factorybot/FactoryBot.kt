package io.github.factorybot

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Central registry for factories and sequences.
 */
object FactoryBot {
    private val factories = ConcurrentHashMap<String, Factory<*>>()
    private val sequences = ConcurrentHashMap<String, Sequence<*>>()

    /**
     * Registers a factory with the given name.
     *
     * @param T The type of object the factory creates
     * @param name The factory name
     * @param factory The factory instance
     */
    fun <T : Any> register(name: String, factory: Factory<T>) {
        factories[name] = factory
    }

    /**
     * Retrieves a registered factory by name.
     *
     * @param T The type of object the factory creates
     * @param name The factory name
     * @return The factory instance
     * @throws IllegalArgumentException If the factory is not found
     */
    fun <T : Any> getFactory(name: String): Factory<T> {
        @Suppress("UNCHECKED_CAST")
        return factories[name] as? Factory<T>
            ?: throw IllegalArgumentException("Factory not found: $name")
    }

    /**
     * Builds an instance using the Build strategy.
     *
     * @param T The type of object to build
     * @param name The factory name (defaults to class simple name)
     * @param traitNames Traits to apply
     * @param overrides Attribute overrides
     * @return The built instance
     * @throws IllegalArgumentException If the factory is not found
     */
    inline fun <reified T : Any> build(
        name: String = T::class.simpleName!!,
        vararg traitNames: String,
        noinline overrides: (FactoryBuilder<T>.() -> Unit)? = null
    ): T {
        val factory = getFactory<T>(name)
        val overrideMap = overrides?.let { buildOverrides(factory, it) } ?: emptyMap()
        return factory.build(BuildStrategy.Build, traitNames.toList(), overrideMap)
    }

    /**
     * Builds an instance using the Create strategy with afterCreate callbacks.
     *
     * @param T The type of object to create
     * @param name The factory name (defaults to class simple name)
     * @param traitNames Traits to apply
     * @param overrides Attribute overrides
     * @return The created instance
     * @throws IllegalArgumentException If the factory is not found
     */
    inline fun <reified T : Any> create(
        name: String = T::class.simpleName!!,
        vararg traitNames: String,
        noinline overrides: (FactoryBuilder<T>.() -> Unit)? = null
    ): T {
        val factory = getFactory<T>(name)
        val overrideMap = overrides?.let { buildOverrides(factory, it) } ?: emptyMap()
        return factory.build(BuildStrategy.Create, traitNames.toList(), overrideMap)
    }

    /**
     * Builds a stubbed instance using the BuildStubbed strategy.
     *
     * @param T The type of object to build
     * @param name The factory name (defaults to class simple name)
     * @param traitNames Traits to apply
     * @param overrides Attribute overrides
     * @return The stubbed instance
     * @throws IllegalArgumentException If the factory is not found
     */
    inline fun <reified T : Any> buildStubbed(
        name: String = T::class.simpleName!!,
        vararg traitNames: String,
        noinline overrides: (FactoryBuilder<T>.() -> Unit)? = null
    ): T {
        val factory = getFactory<T>(name)
        val overrideMap = overrides?.let { buildOverrides(factory, it) } ?: emptyMap()
        return factory.build(BuildStrategy.BuildStubbed, traitNames.toList(), overrideMap)
    }

    /**
     * Builds attribute overrides from a configuration block.
     *
     * @param T The type of object
     * @param factory The factory to use as a template
     * @param block Configuration block for overrides
     * @return Map of attribute definitions
     */
    fun <T : Any> buildOverrides(factory: Factory<T>, block: FactoryBuilder<T>.() -> Unit): Map<String, AttributeDefinition<*>> {
        val overrideFactory = Factory(factory.klass, factory.name)
        val builder = FactoryBuilder(overrideFactory)
        builder.block()
        return overrideFactory.attributes
    }

    /**
     * Defines a global named sequence.
     *
     * @param T The type of value the sequence generates
     * @param name The sequence name
     * @param generator Function to generate sequence values
     */
    fun <T> defineSequence(name: String, generator: (Long) -> T) {
        sequences[name] = Sequence(generator)
    }

    /**
     * Gets the next value from a named sequence.
     *
     * @param T The type of value to generate
     * @param name The sequence name
     * @return The next sequence value
     * @throws IllegalArgumentException If the sequence is not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> nextSequence(name: String): T {
        val sequence = sequences[name] as? Sequence<T>
            ?: throw IllegalArgumentException("Sequence not found: $name")
        return sequence.next()
    }

    /**
     * Clears all registered factories and sequences.
     */
    fun clear() {
        factories.clear()
        sequences.clear()
        SequenceRegistry.clear()
    }
}

/**
 * Defines a factory for type T.
 *
 * @param T The type of object this factory creates
 * @param name The factory name (defaults to class simple name)
 * @param parent Optional parent factory name for inheritance
 * @param block Configuration block for the factory
 */
inline fun <reified T : Any> factory(
    name: String = T::class.simpleName!!,
    parent: String? = null,
    noinline block: FactoryBuilder<T>.() -> Unit
) {
    val parentFactory = parent?.let { FactoryBot.getFactory<T>(it) }
    val factory = Factory(T::class, name, parentFactory)
    val builder = FactoryBuilder(factory)
    builder.block()
    FactoryBot.register(name, factory)
}

/**
 * Builds an instance using default factory settings.
 *
 * @param T The type of object to build
 * @return The built instance
 */
inline fun <reified T : Any> build(): T =
    FactoryBot.build(T::class.simpleName!!, overrides = null)

/**
 * Builds an instance using a named factory or trait.
 *
 * @param T The type of object to build
 * @param factoryOrTrait Factory name or trait name
 * @return The built instance
 */
inline fun <reified T : Any> build(
    factoryOrTrait: String
): T {
    return try {
        FactoryBot.build<T>(factoryOrTrait, overrides = null)
    } catch (e: IllegalArgumentException) {
        FactoryBot.build(T::class.simpleName!!, factoryOrTrait, overrides = null)
    }
}

/**
 * Builds an instance with multiple traits.
 *
 * @param T The type of object to build
 * @param trait1 First trait name
 * @param trait2 Second trait name
 * @param moreTraits Additional trait names
 * @return The built instance
 */
inline fun <reified T : Any> build(
    trait1: String,
    trait2: String,
    vararg moreTraits: String
): T = FactoryBot.build(T::class.simpleName!!, trait1, trait2, *moreTraits, overrides = null)

/**
 * Builds an instance with attribute overrides.
 *
 * @param T The type of object to build
 * @param overrides Configuration block for overriding attributes
 * @return The built instance
 */
inline fun <reified T : Any> build(
    noinline overrides: FactoryBuilder<T>.() -> Unit
): T = FactoryBot.build(T::class.simpleName!!, overrides = overrides)

/**
 * Creates an instance using default factory settings.
 *
 * @param T The type of object to create
 * @return The created instance
 */
inline fun <reified T : Any> create(): T =
    FactoryBot.create(T::class.simpleName!!, overrides = null)

/**
 * Creates an instance with a trait.
 *
 * @param T The type of object to create
 * @param trait Trait name to apply
 * @return The created instance
 */
inline fun <reified T : Any> create(
    trait: String
): T = FactoryBot.create(T::class.simpleName!!, trait, overrides = null)

/**
 * Creates an instance with multiple traits.
 *
 * @param T The type of object to create
 * @param trait1 First trait name
 * @param trait2 Second trait name
 * @param moreTraits Additional trait names
 * @return The created instance
 */
inline fun <reified T : Any> create(
    trait1: String,
    trait2: String,
    vararg moreTraits: String
): T = FactoryBot.create(T::class.simpleName!!, trait1, trait2, *moreTraits, overrides = null)

/**
 * Creates an instance with attribute overrides.
 *
 * @param T The type of object to create
 * @param overrides Configuration block for overriding attributes
 * @return The created instance
 */
inline fun <reified T : Any> create(
    noinline overrides: FactoryBuilder<T>.() -> Unit
): T = FactoryBot.create(T::class.simpleName!!, overrides = overrides)

/**
 * Builds a stubbed instance using default factory settings.
 *
 * @param T The type of object to build
 * @return The stubbed instance
 */
inline fun <reified T : Any> buildStubbed(): T =
    FactoryBot.buildStubbed(T::class.simpleName!!, overrides = null)

/**
 * Builds a stubbed instance with a trait.
 *
 * @param T The type of object to build
 * @param trait Trait name to apply
 * @return The stubbed instance
 */
inline fun <reified T : Any> buildStubbed(
    trait: String
): T = FactoryBot.buildStubbed(T::class.simpleName!!, trait, overrides = null)

/**
 * Builds a stubbed instance with multiple traits.
 *
 * @param T The type of object to build
 * @param trait1 First trait name
 * @param trait2 Second trait name
 * @param moreTraits Additional trait names
 * @return The stubbed instance
 */
inline fun <reified T : Any> buildStubbed(
    trait1: String,
    trait2: String,
    vararg moreTraits: String
): T = FactoryBot.buildStubbed(T::class.simpleName!!, trait1, trait2, *moreTraits, overrides = null)

/**
 * Builds a stubbed instance with attribute overrides.
 *
 * @param T The type of object to build
 * @param overrides Configuration block for overriding attributes
 * @return The stubbed instance
 */
inline fun <reified T : Any> buildStubbed(
    noinline overrides: FactoryBuilder<T>.() -> Unit
): T = FactoryBot.buildStubbed(T::class.simpleName!!, overrides = overrides)

/**
 * Defines a global named sequence.
 *
 * @param T The type of value the sequence generates
 * @param name The sequence name
 * @param generator Function to generate sequence values
 */
fun <T> defineSequence(name: String, generator: (Long) -> T) {
    FactoryBot.defineSequence(name, generator)
}

inline fun <reified T : Any> FactoryBuilder<T>.firstName(noinline block: () -> String) {
    attribute("firstName") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.lastName(noinline block: () -> String) {
    attribute("lastName") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.email(noinline block: () -> String) {
    attribute("email") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.role(noinline block: () -> Any) {
    attribute("role") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.title(noinline block: () -> String) {
    attribute("title") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.content(noinline block: () -> String) {
    attribute("content") { block() }
}

inline fun <reified T : Any> FactoryBuilder<T>.author(noinline block: () -> Any?) {
    attribute("author") { block() }
}

inline fun <reified T : Any> TraitBuilder<T>.firstName(noinline block: () -> String) {
    attribute("firstName") { block() }
}

inline fun <reified T : Any> TraitBuilder<T>.role(noinline block: () -> Any) {
    attribute("role") { block() }
}

inline fun <reified T : Any> TraitBuilder<T>.email(noinline block: () -> String) {
    attribute("email") { block() }
}
