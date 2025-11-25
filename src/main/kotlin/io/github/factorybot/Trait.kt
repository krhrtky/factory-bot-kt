package io.github.factorybot

/**
 * Named group of attributes and callbacks that can be applied to factories.
 *
 * @param T The type of object this trait applies to
 * @param name The trait name
 * @param attributes Map of attribute definitions in this trait
 * @param afterBuildCallbacks Callbacks executed after building instances with this trait
 * @param afterCreateCallbacks Callbacks executed after creating instances with this trait
 */
class Trait<T : Any>(
    val name: String,
    internal val attributes: MutableMap<String, AttributeDefinition<*>> = mutableMapOf(),
    internal val afterBuildCallbacks: MutableList<(T) -> Unit> = mutableListOf(),
    internal val afterCreateCallbacks: MutableList<(T) -> Unit> = mutableListOf()
)
