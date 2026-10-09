package com.carled.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.screens.auth.LoginScreen
import com.carled.app.presentation.screens.auth.LoginViewModel
import com.carled.app.presentation.screens.control.ControlScreen
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.scan.ScanScreen
import com.carled.app.presentation.screens.scan.ScanViewModel
import com.carled.app.presentation.screens.store.StoreScreen
import com.carled.app.presentation.screens.store.StoreViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Scan.route
    ) {
        composable(NavRoute.Scan.route) {
            val viewModel = ScanViewModel(ServiceLocator.bleUseCases)
            ScanScreen(
                viewModel = viewModel,
                onNavigateToControl = {
                    navController.navigate(NavRoute.Control.route)
                }
            )
        }

        composable(NavRoute.Control.route) {
            val viewModel = ControlViewModel(ServiceLocator.bleUseCases)
            ControlScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStore = {
                    navController.navigate(NavRoute.Store.route)
                }
            )
        }

        composable(NavRoute.Store.route) {
            val viewModel = StoreViewModel(ServiceLocator.productRepository)
            StoreScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavRoute.Login.route) {
            val viewModel = LoginViewModel(ServiceLocator.loginUseCase)
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.Scan.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
