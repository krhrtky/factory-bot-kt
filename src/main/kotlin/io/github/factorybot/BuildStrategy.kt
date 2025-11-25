package io.github.factorybot

/**
 * Defines different strategies for building instances from factories.
 */
sealed interface BuildStrategy {
    /**
     * Builds an instance without persistence.
     */
    object Build : BuildStrategy

    /**
     * Builds an instance and executes afterCreate callbacks.
     */
    object Create : BuildStrategy

    /**
     * Builds a stubbed instance (optimized for testing).
     */
    object BuildStubbed : BuildStrategy

    /**
     * Returns only the attributes without building an instance.
     */
    object AttributesFor : BuildStrategy
}
