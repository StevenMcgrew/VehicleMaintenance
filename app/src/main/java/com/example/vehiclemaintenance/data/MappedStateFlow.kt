package com.example.vehiclemaintenance.data

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * A read-only view of [this] through [transform].
 *
 * `stateIn` would need a scope and would leave `value` briefly stale after a write, which callers
 * here read synchronously right after mutating.
 */
fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> =
    MappedStateFlow(this, transform)

private class MappedStateFlow<T, R>(
    private val source: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {

    override val value: R get() = transform(source.value)

    override val replayCache: List<R> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        source.map(transform).distinctUntilChanged().collect(collector)
        error("source is a StateFlow and never completes")
    }
}
