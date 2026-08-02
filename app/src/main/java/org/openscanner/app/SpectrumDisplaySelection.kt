package org.openscanner.app

/**
 * A null custom selection means "show every network in the current channel
 * group", including networks that appear in later snapshots. A non-null set is
 * an explicit user selection. The focused network is always pinned so the
 * chart, summary, and overlap analysis keep referring to the same AP.
 */
internal fun visibleSpectrumNetworks(
    networks: List<NetworkUiModel>,
    customSelectionIds: Set<String>?,
    focusedNetworkId: String?,
): List<NetworkUiModel> {
    if (customSelectionIds == null) return networks
    return networks.filter { network ->
        network.uiId == focusedNetworkId || network.uiId in customSelectionIds
    }
}

/**
 * Applies one checkbox change. Returning null restores the adaptive "all"
 * mode so newly detected networks are automatically included.
 */
internal fun updateSpectrumSelection(
    networks: List<NetworkUiModel>,
    customSelectionIds: Set<String>?,
    focusedNetworkId: String?,
    networkId: String,
    displayed: Boolean,
): Set<String>? {
    if (networkId == focusedNetworkId && !displayed) return customSelectionIds

    val availableIds = networks.mapTo(linkedSetOf()) { it.uiId }
    val updated = (customSelectionIds ?: availableIds).toMutableSet().apply {
        if (displayed) add(networkId) else remove(networkId)
        focusedNetworkId?.let(::add)
    }
    return updated.takeUnless { it.containsAll(availableIds) }
}

/**
 * Keeps display and focus state independent while guaranteeing that a newly
 * focused network is visible. A one-item "Only focused" selection follows the
 * new focus instead of unexpectedly leaving the previous focus on the chart.
 */
internal fun updateSpectrumSelectionForFocus(
    networks: List<NetworkUiModel>,
    customSelectionIds: Set<String>?,
    previousFocusedNetworkId: String?,
    newFocusedNetworkId: String,
): Set<String>? {
    if (customSelectionIds == null) return null
    val availableIds = networks.mapTo(linkedSetOf()) { it.uiId }
    if (newFocusedNetworkId !in availableIds) return customSelectionIds
    val updated = if (
        previousFocusedNetworkId != null &&
        customSelectionIds == setOf(previousFocusedNetworkId)
    ) {
        linkedSetOf(newFocusedNetworkId)
    } else {
        customSelectionIds.filterTo(linkedSetOf()) { it in availableIds }.apply {
            add(newFocusedNetworkId)
        }
    }
    return updated.takeUnless { it.containsAll(availableIds) }
}
