package com.carled.app.domain.model

data class OwnedDevicePanel(
    val id: String,
    val label: String,          // "Panel 01-ID:232391280x"
    val firmwareVersion: String, // "1.5"
    val lastUpdated: String,     // "DD/MM/YYY" — placeholder until backend wired
    val updateAvailable: Boolean
)

data class FirmwareRelease(
    val id: String,
    val label: String,           // "Latest: Version 1.5"
    val compatibility: String,   // "All panels"
    val releaseDate: String,
    val tag: FirmwareTag
)

enum class FirmwareTag { LATEST, OLD, LEGACY_OUTDATED }

// Mirrors MockCharacterInventory — swap for a real repository before release.
object MockDeviceInventory {
    fun ownedPanels(userId: String): List<OwnedDevicePanel> = listOf(
        OwnedDevicePanel("panel-01", "Panel 01-ID:232391280x", "1.5", "DD/MM/YYY", updateAvailable = false),
        OwnedDevicePanel("panel-02", "Panel 02-ID:232391280x", "1.0", "DD/MM/YYY", updateAvailable = true)
    )

    fun firmwareReleases(): List<FirmwareRelease> = listOf(
        FirmwareRelease("fw-latest", "Latest: Version 1.5", "All panels", "DD/MM/YYY", FirmwareTag.LATEST),
        FirmwareRelease("fw-old", "Old: Version 1.0", "All panels", "DD/MM/YYY", FirmwareTag.OLD),
        FirmwareRelease("fw-legacy", "Legacy: Version 0.5.1", "32 x 32 Panels", "DD/MM/YYY", FirmwareTag.LEGACY_OUTDATED)
    )
}