package com.carled.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.screens.auth.LoginScreen
import com.carled.app.presentation.screens.auth.LoginViewModel
import com.carled.app.presentation.screens.canvas.CanvasScreen
import com.carled.app.presentation.screens.canvas.CanvasViewModel
import com.carled.app.presentation.screens.control.ControlViewModel
import com.carled.app.presentation.screens.main.MainContainerScreen
import com.carled.app.presentation.screens.marquee.MarqueeScreen
import com.carled.app.presentation.screens.marquee.MarqueeViewModel
import com.carled.app.presentation.screens.scan.DivoomScanScreen
import com.carled.app.presentation.screens.scan.ScanViewModel
import com.carled.app.presentation.screens.store.StoreScreen
import com.carled.app.presentation.screens.store.StoreViewModel
import com.carled.app.presentation.screens.upload.UploadScreen
import com.carled.app.presentation.screens.upload.UploadViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Shared ViewModels qua ServiceLocator
    val controlViewModel = ControlViewModel(ServiceLocator.bleUseCases)
    val scanViewModel = ScanViewModel(ServiceLocator.bleUseCases)
    val scanUiState by scanViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.route
    ) {
        // Màn hình chính phong cách Divoom (5 Tabs)
        composable(NavRoute.Main.route) {
            MainContainerScreen(
                controlViewModel = controlViewModel,
                connectionState = scanUiState.connectionState,
                onOpenScanner = { navController.navigate(NavRoute.Scan.route) },
                onOpenCanvas = { navController.navigate(NavRoute.Canvas.route) },
                onOpenMarquee = { navController.navigate(NavRoute.Marquee.route) },
                onOpenUpload = { navController.navigate(NavRoute.Upload.route) },
                onOpenStore = { navController.navigate(NavRoute.Store.route) },
                onLogout = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // Quét & Kết nối Bluetooth phong cách Divoom (Ảnh 2 & 3)
        composable(NavRoute.Scan.route) {
            DivoomScanScreen(
                viewModel = scanViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Bảng vẽ Live Canvas Pixel 64x32
        composable(NavRoute.Canvas.route) {
            val canvasViewModel = CanvasViewModel(ServiceLocator.bleUseCases)
            CanvasScreen(
                viewModel = canvasViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Bộ soạn thảo chữ LED chạy chữ (Marquee Text)
        composable(NavRoute.Marquee.route) {
            val marqueeViewModel = MarqueeViewModel(ServiceLocator.bleUseCases)
            MarqueeScreen(
                viewModel = marqueeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Nạp ảnh nhị phân Flash SPIFFS
        composable(NavRoute.Upload.route) {
            val uploadViewModel = UploadViewModel(ServiceLocator.bleUseCases)
            UploadScreen(
                viewModel = uploadViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Cửa hàng Car LED eStore (Flow 1 E-commerce)
        composable(NavRoute.Store.route) {
            val storeViewModel = StoreViewModel(ServiceLocator.productRepository)
            StoreScreen(
                viewModel = storeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Đăng nhập
        composable(NavRoute.Login.route) {
            val loginViewModel = LoginViewModel(ServiceLocator.loginUseCase)
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.Main.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
