package com.carled.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Shared ViewModels qua ServiceLocator
    val controlViewModel = remember { ControlViewModel(ServiceLocator.bleUseCases) }
    val scanViewModel = remember { ScanViewModel(ServiceLocator.bleUseCases) }
    val scanUiState by scanViewModel.uiState.collectAsState()
    val currentUser by ServiceLocator.authRepository.getCurrentUser().collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Khởi tạo điểm bắt đầu dựa trên trạng thái đã đăng nhập hay chưa
    val startDestination = if (ServiceLocator.userPreferences.getAuthToken().isNullOrBlank()) {
        NavRoute.Login.route
    } else {
        NavRoute.Main.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Màn hình chính phong cách Divoom (5 Tabs)
        composable(NavRoute.Main.route) {
            MainContainerScreen(
                controlViewModel = controlViewModel,
                connectionState = scanUiState.connectionState,
                currentUser = currentUser,
                onOpenScanner = { navController.navigate(NavRoute.Scan.route) },
                onOpenCanvas = { navController.navigate(NavRoute.Canvas.route) },
                onOpenMarquee = { navController.navigate(NavRoute.Marquee.route) },
                onOpenUpload = { navController.navigate(NavRoute.Upload.route) },
                onOpenStore = { navController.navigate(NavRoute.Store.route) },
                onLogout = {
                    coroutineScope.launch {
                        ServiceLocator.logoutUseCase()
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Quét & Kết nối Bluetooth phong cách Divoom
        composable(NavRoute.Scan.route) {
            DivoomScanScreen(
                viewModel = scanViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Bảng vẽ Live Canvas Pixel 64x32
        composable(NavRoute.Canvas.route) { backStackEntry ->
            val canvasViewModel = remember(backStackEntry) {
                CanvasViewModel(ServiceLocator.bleUseCases)
            }
            CanvasScreen(
                viewModel = canvasViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Bộ soạn thảo chữ LED chạy chữ (Marquee Text)
        composable(NavRoute.Marquee.route) { backStackEntry ->
            val marqueeViewModel = remember(backStackEntry) {
                MarqueeViewModel(ServiceLocator.bleUseCases)
            }
            MarqueeScreen(
                viewModel = marqueeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Nạp ảnh nhị phân Flash SPIFFS
        composable(NavRoute.Upload.route) { backStackEntry ->
            val uploadViewModel = remember(backStackEntry) {
                UploadViewModel(ServiceLocator.bleUseCases)
            }
            UploadScreen(
                viewModel = uploadViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Cửa hàng Car LED eStore (Flow 1 E-commerce)
        composable(NavRoute.Store.route) { backStackEntry ->
            val storeViewModel = remember(backStackEntry) {
                StoreViewModel(ServiceLocator.productRepository)
            }
            StoreScreen(
                viewModel = storeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Màn hình Đăng nhập & Đăng ký phong cách Retro Pixel (Figma LEDSCREEN)
        composable(NavRoute.Login.route) { backStackEntry ->
            val loginViewModel = remember(backStackEntry) {
                LoginViewModel(
                    loginUseCase = ServiceLocator.loginUseCase,
                    registerUseCase = ServiceLocator.registerUseCase
                )
            }
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
