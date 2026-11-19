package com.granit.survival

/**
 * Routes a context action to a native station panel.
 *
 * The renderer/simulation owns world interactions. This router only decides which
 * Activity callback should be invoked after the simulation has processed the action.
 */
internal class StationPanelRouter(
    private val actions: Map<TileType, () -> Unit>
) {
    fun route(target: TileType?, hasNearbyResource: Boolean) {
        if (hasNearbyResource || target == null) return
        actions[target]?.invoke()
    }
}
