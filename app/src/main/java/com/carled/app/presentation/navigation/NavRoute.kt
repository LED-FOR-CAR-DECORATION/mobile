package com.carled.app.presentation.navigation

import android.net.Uri

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    object Main : NavRoute("main")
    object Library : NavRoute("library")
    object Scan : NavRoute("scan")
    object Control : NavRoute("control")
    object Effect : NavRoute("effect")
    object Photo : NavRoute("image")
    object Marquee : NavRoute("marquee")

    object Studio : NavRoute("studio?draft={draft}") {
        fun create(draftId: String? = null) = "studio?draft=${Uri.encode(draftId.orEmpty())}"
    }

    object Character : NavRoute("character/{id}") {
        fun create(characterId: String) = "character/${Uri.encode(characterId)}"
    }
    object PanelControl : NavRoute("panel-control/{id}") {
        fun create(panelId: String) = "panel-control/${Uri.encode(panelId)}"
    }
    object ClaimDevice : NavRoute("claim-device")

    @Deprecated("No such destination in AppNavigation. Use Studio.")
    object Canvas : NavRoute("canvas")

    @Deprecated("No such destination in AppNavigation. Use Photo.")
    object Upload : NavRoute("upload")

    @Deprecated("No such destination in AppNavigation.")
    object Store : NavRoute("store")

    @Deprecated("No such destination in AppNavigation. Use Library.")
    object Gallery : NavRoute("gallery")

    @Deprecated("No such destination in AppNavigation.")
    object Community : NavRoute("community")

    @Deprecated("No such destination in AppNavigation.")
    object Messages : NavRoute("messages")

    @Deprecated("No such destination in AppNavigation.")
    object Profile : NavRoute("profile")
}