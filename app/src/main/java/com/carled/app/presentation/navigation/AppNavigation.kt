package com.carled.app.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.carled.app.core.di.ServiceLocator
import com.carled.app.presentation.screens.auth.*
import com.carled.app.presentation.screens.canvas.*
import com.carled.app.presentation.screens.control.*
import com.carled.app.presentation.screens.main.MirageHomeScreen
import com.carled.app.presentation.screens.marquee.*
import com.carled.app.presentation.screens.scan.*
import kotlinx.coroutines.launch
import com.carled.app.presentation.screens.upload.*
import com.carled.app.data.local.MockCharacterInventory

private fun <T : ViewModel> factory(create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current.applicationContext
    // Activity-scoped shared models; route models below are scoped to their back-stack entry.
    val control: ControlViewModel = viewModel(factory = factory { ControlViewModel(ServiceLocator.bleUseCases) })
    val scanner: ScanViewModel = viewModel(factory = factory { ScanViewModel(ServiceLocator.bleUseCases) })
    val user by ServiceLocator.authRepository.getCurrentUser().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val start = remember { if (ServiceLocator.userPreferences.getAuthToken().isNullOrBlank()) "login" else "main" }
    fun openCanvas(id: String? = null) { navController.navigate("studio?draft=${id ?: ""}") }
    NavHost(navController, startDestination = start) {
        composable("login") { entry ->
            val vm: LoginViewModel = viewModel(entry, factory = factory { LoginViewModel(ServiceLocator.loginUseCase, ServiceLocator.registerUseCase) })
            MirageLoginScreen(vm) { navController.navigate("main") { popUpTo("login") { inclusive = true }; launchSingleTop = true } }
        }
        composable("main") {
            MirageHomeScreen(control, scanner, user,
                openScan = { navController.navigate("scan") }, openControl = { navController.navigate("control") },
                openCanvas = { openCanvas(it) }, openMarquee = { navController.navigate("marquee") },
                openUpload = { navController.navigate("image") }, openEffect = { navController.navigate("effect") }, openCharacter = { navController.navigate("character/$it") }, logout = {
                    scope.launch { ServiceLocator.logoutUseCase(); navController.navigate("login") { popUpTo(0) { inclusive = true } } }
                })
        }
        composable("scan") { MirageScanScreen(scanner) { navController.popBackStack() } }
        composable("control") {
            MirageControlScreen(control, { navController.popBackStack() }, { navController.navigate("scan") })
        }
        composable("effect") {
            MirageEffectScreen(control, { navController.popBackStack() }, { navController.navigate("scan") })
        }
        composable("library") {
            MirageHomeScreen(control, scanner, user,
                openScan = { navController.navigate("scan") }, openControl = { navController.navigate("control") },
                openCanvas = { openCanvas(it) }, openMarquee = { navController.navigate("marquee") }, openUpload = { navController.navigate("image") }, openEffect = { navController.navigate("effect") }, openCharacter = { navController.navigate("character/$it") },
                logout = { scope.launch { ServiceLocator.logoutUseCase(); navController.navigate("login") { popUpTo(0) { inclusive = true } } } }, initialTab = 1)
        }
        composable("studio?draft={draft}") { entry ->
            val id = entry.arguments?.getString("draft")?.takeIf { it.isNotBlank() }
            val vm: StudioViewModel = viewModel(entry, factory = factory { StudioViewModel(ServiceLocator.bleUseCases, context, id) })
            StudioScreen(vm, { navController.popBackStack() })
        }
        composable("image") { entry ->
            val vm: UploadViewModel = viewModel(entry, factory = factory { UploadViewModel(ServiceLocator.bleUseCases) })
            PhotoProjectionScreen(vm, { navController.popBackStack() }, { navController.navigate("scan") })
        }
        composable("character/{id}") { entry ->
            val character = MockCharacterInventory().listOwned(user?.id ?: "").firstOrNull { it.characterId == entry.arguments?.getString("id") }
            val vm: UploadViewModel = viewModel(entry, factory = factory { UploadViewModel(ServiceLocator.bleUseCases) })
            if (character != null) PhotoProjectionScreen(vm, { navController.popBackStack() }, { navController.navigate("scan") }, character)
            else androidx.compose.material3.Text("Không tìm thấy Character trong Inventory.")
        }
        composable("marquee") { entry ->
            val accountId = user?.id
            if (accountId == null) androidx.compose.material3.Text("Đang tải tài khoản…")
            else {
                val vm: MarqueeViewModel = viewModel(entry, key = "marquee:$accountId", factory = factory {
                    MarqueeViewModel(ServiceLocator.bleUseCases, com.carled.app.data.local.TextProfileStore(context, accountId))
                })
                MirageMarqueeScreen(vm, { navController.popBackStack() })
            }
        }
    }
}
