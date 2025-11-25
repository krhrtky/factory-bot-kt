package io.github.factorybot

import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe sequence generator for producing unique values.
 *
 * @param T The type of value this sequence generates
 * @param generator Function that takes a sequence number and returns a value
 */
class Sequence<T>(
    private val generator: (Long) -> T
) {
    private val counter = AtomicLong(1)

    /**
     * Generates the next value in the sequence.
     *
     * @return The next generated value
     */
    fun next(): T = generator(counter.getAndIncrement())
}
