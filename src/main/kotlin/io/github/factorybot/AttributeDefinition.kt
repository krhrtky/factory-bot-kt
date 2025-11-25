package io.github.factorybot

/**
 * Defines a lazy attribute that will be evaluated during instance creation.
 *
 * @param T The type of value this attribute produces
 * @param name The attribute name
 * @param evaluator Function that evaluates the attribute value given a context
 */
class AttributeDefinition<T>(
    val name: String,
    val evaluator: (EvaluationContext) -> T
)
