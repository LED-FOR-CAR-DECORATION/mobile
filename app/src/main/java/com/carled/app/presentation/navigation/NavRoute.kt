package com.carled.app.presentation.navigation

sealed class NavRoute(val route: String) {
    object Main      : NavRoute("main")
    object Scan      : NavRoute("scan")
    object Control   : NavRoute("control")
    object Store     : NavRoute("store")
    object Login     : NavRoute("login")
    object Canvas    : NavRoute("canvas")
    object Marquee   : NavRoute("marquee")
    object Upload    : NavRoute("upload")
    object Gallery   : NavRoute("gallery")
    object Community : NavRoute("community")
    object Messages  : NavRoute("messages")
    object Profile   : NavRoute("profile")
}
