package com.toblad.khwab.core.capability

data class IntentCapability(
    val requiresEntity: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val requiresPermission: Boolean = false,
    val requiresNetwork: Boolean = false,
    val isDestructive: Boolean = false
)
