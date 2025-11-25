package io.github.factorybot

/**
 * Context object available during attribute evaluation.
 *
 * @param strategy The build strategy being used
 * @param factoryName The name of the factory being evaluated
 */
class EvaluationContext(
    val strategy: BuildStrategy,
    val factoryName: String
) {
    private val attributes = mutableMapOf<String, Any?>()

    /**
     * Stores an attribute value in the context.
     *
     * @param name The attribute name
     * @param value The attribute value
     */
    fun setAttribute(name: String, value: Any?) {
        attributes[name] = value
    }

    /**
     * Retrieves an attribute value from the context.
     *
     * @param name The attribute name
     * @return The attribute value, or null if not found
     */
    fun getAttribute(name: String): Any? = attributes[name]
}
