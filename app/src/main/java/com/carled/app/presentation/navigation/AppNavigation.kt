package com.carled.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.local.MockCharacterInventory
import com.carled.app.data.local.TextProfileStore
import com.carled.app.domain.model.ConnectionState
import com.carled.app.domain.model.User
import com.carled.app.presentation.screens.auth.*
import com.carled.app.presentation.screens.canvas.*
import com.carled.app.presentation.screens.control.*
import com.carled.app.presentation.screens.main.MirageHomeScreen
import com.carled.app.presentation.screens.marquee.*
import com.carled.app.presentation.screens.scan.*
import com.carled.app.presentation.screens.upload.*
import kotlinx.coroutines.launch

private fun <T : ViewModel> factory(create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    val control: ControlViewModel = viewModel(factory = factory { ControlViewModel(ServiceLocator.bleUseCases) })
    val scanner: ScanViewModel = viewModel(factory = factory { ScanViewModel(ServiceLocator.bleUseCases) })
    val userFlow = remember { ServiceLocator.authRepository.getCurrentUser() }
    val user by userFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val start = NavRoute.Login.route

    val goBack: () -> Unit = { navController.popBackStack() }
    val goScan: () -> Unit = { navController.navigate(NavRoute.Scan.route) }
    val logout: () -> Unit = {
        scope.launch {
            ServiceLocator.logoutUseCase()
            navController.navigate(NavRoute.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController, startDestination = start) {
        composable(NavRoute.Login.route) { entry ->
            val vm: LoginViewModel = viewModel(
                entry,
                factory = factory { LoginViewModel(ServiceLocator.loginUseCase, ServiceLocator.registerUseCase) }
            )
            MirageLoginScreen(vm) {
                navController.navigate(NavRoute.Main.route) {
                    popUpTo(NavRoute.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable(NavRoute.Main.route) {
            HomeRoute(navController, control, scanner, user, initialTab = 0, onLogout = logout)
        }
        composable(NavRoute.Library.route) {
            HomeRoute(navController, control, scanner, user, initialTab = 1, onLogout = logout)
        }
        composable(NavRoute.Scan.route) { MirageScanScreen(scanner) { goBack() } }
        composable(NavRoute.Effect.route) { MirageEffectScreen(control, goBack, goScan) }
        composable(NavRoute.Studio.route) { entry ->
            val id = entry.arguments?.getString("draft")?.takeIf { it.isNotBlank() }
            val vm: StudioViewModel = viewModel(
                entry,
                factory = factory { StudioViewModel(ServiceLocator.bleUseCases, context, id) }
            )
            StudioScreen(vm, goBack)
        }
        composable(NavRoute.Photo.route) { entry ->
            val vm: UploadViewModel = viewModel(entry, factory = factory { UploadViewModel(ServiceLocator.bleUseCases) })
            PhotoProjectionScreen(vm, goBack, goScan)
        }
        composable(NavRoute.Character.route) { entry ->
            val account = user
            if (account == null) {
                RouteMessage("Đang tải tài khoản…")
            } else {
                val characterId = entry.arguments?.getString("id")
                val character = remember(account.id, characterId) {
                    MockCharacterInventory().listOwned(account.id).firstOrNull { it.characterId == characterId }
                }
                if (character == null) {
                    RouteMessage("Không tìm thấy Character trong Inventory.")
                } else {
                    val vm: UploadViewModel = viewModel(
                        entry,
                        factory = factory { UploadViewModel(ServiceLocator.bleUseCases) }
                    )
                    PhotoProjectionScreen(vm, goBack, goScan, character)
                }
            }
        }
        composable(NavRoute.Marquee.route) { entry ->
            val accountId = user?.id
            if (accountId == null) {
                RouteMessage("Đang tải tài khoản…")
            } else {
                val vm: MarqueeViewModel = viewModel(
                    entry,
                    key = "marquee:$accountId",
                    factory = factory {
                        MarqueeViewModel(ServiceLocator.bleUseCases, TextProfileStore(context, accountId))
                    }
                )
                MirageMarqueeScreen(vm, goBack)
            }
        }
        composable(NavRoute.Control.route) { entry ->
            val accountId = user?.id
            if (accountId == null) {
                RouteMessage("Đang tải tài khoản…")
            } else {
                MirageControlScreen(
                    vm = control,
                    back = goBack,
                    scanner = goScan,
                    openPanelControl = { panelId -> navController.navigate(NavRoute.PanelControl.create(panelId)) },
                    userId = accountId
                )
            }
        }
        composable(NavRoute.PanelControl.route) { entry ->
            val id = entry.arguments?.getString("id")
            PanelControlScreen(control, id, goBack, goScan)
        }
    }
}

@Composable
private fun HomeRoute(
    navController: NavHostController,
    control: ControlViewModel,
    scanner: ScanViewModel,
    user: User?,
    initialTab: Int,
    onLogout: () -> Unit
) {
    val controlState by control.uiState.collectAsState()
    val connected = controlState.connectionState is ConnectionState.Connected

    MirageHomeScreen(
        control = control,
        scanner = scanner,
        user = user,
        openScan = { navController.navigate(NavRoute.Scan.route) },
        openControl = {
            if (connected) {
                navController.navigate(NavRoute.PanelControl.create("current"))
            } else {
                navController.navigate(NavRoute.Control.route)
            }
        },
        openCanvas = { navController.navigate(NavRoute.Studio.create(it)) },
        openMarquee = { navController.navigate(NavRoute.Marquee.route) },
        openUpload = { navController.navigate(NavRoute.Photo.route) },
        logout = onLogout,
        openEffect = { navController.navigate(NavRoute.Effect.route) },
        openCharacter = { navController.navigate(NavRoute.Character.create(it)) },
        openDeviceManager = { navController.navigate(NavRoute.Control.route) },
        initialTab = initialTab
    )
}

@Composable
private fun RouteMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text)
    }
}