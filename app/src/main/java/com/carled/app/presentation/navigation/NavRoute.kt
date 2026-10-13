package com.carled.app.presentation.navigation

sealed class NavRoute(val route: String) {
    object Scan    : NavRoute("scan")
    object Control : NavRoute("control")
    object Store   : NavRoute("store")
    object Login   : NavRoute("login")
    object Canvas  : NavRoute("canvas")
    object Marquee : NavRoute("marquee")
    object Upload  : NavRoute("upload")
}
