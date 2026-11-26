package com.carled.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.data.local.TextProfileStore
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.model.User
import com.carled.app.domain.model.ble.ConnectionState
import com.carled.app.presentation.screens.auth.*
import com.carled.app.presentation.screens.canvas.*
import com.carled.app.presentation.screens.claim.ClaimDeviceScreen
import com.carled.app.presentation.screens.claim.ClaimDeviceViewModel
import com.carled.app.presentation.screens.control.*
import com.carled.app.presentation.screens.main.HOME_TAB_COMMISSION
import com.carled.app.presentation.screens.main.HOME_TAB_CONTROL
import com.carled.app.presentation.screens.main.HOME_TAB_HOME
import com.carled.app.presentation.screens.main.HOME_TAB_LIBRARY
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
    val ownedDevices: OwnedDevicesViewModel = viewModel(factory = factory { OwnedDevicesViewModel(ServiceLocator.deviceRepository) })
    val userFlow = remember { ServiceLocator.authRepository.getCurrentUser() }
    val user by userFlow.collectAsState(initial = null)

    LaunchedEffect(user?.id) {
        if (user != null) ownedDevices.onUserLoggedIn()
    }

    val hasPersistedSession = remember {
        ServiceLocator.userPreferences.getAuthToken()?.isNotBlank() == true
    }
    val start = if (hasPersistedSession) NavRoute.Main.route else NavRoute.Login.route

    val scope = rememberCoroutineScope()

    val accountId = user?.id
    val marquee: MarqueeViewModel? = accountId?.let { id ->
        viewModel(
            key = "marquee:$id",
            factory = factory {
                // ServiceLocator.textProfilePlanProvider is shared between the store (so
                // TextProfileStore.plan() reflects the real subscription entitlement
                // instead of staying pinned to BASIC) and the ViewModel (so init{} can
                // call .refresh() on login and .reset() already runs on logout via
                // ServiceLocator.logoutUseCase) — same singleton, one cache, not two.
                MarqueeViewModel(
                    ServiceLocator.bleUseCases,
                    TextProfileStore(context, id, ServiceLocator.textProfilePlanProvider),
                    ServiceLocator.textProfilePlanProvider
                )
            }
        )
    }

    var homeTab by rememberSaveable { mutableIntStateOf(HOME_TAB_HOME) }

    // A same-account re-login reuses the retained MarqueeViewModel (same key), so its
    // init{} doesn't run again. Keying on the instance re-fetches the subscription plan
    // each time the ViewModel (re)appears after login.
    LaunchedEffect(marquee) { marquee?.refreshPlan() }

    val goBack: () -> Unit = { navController.popBackStack() }
    val goScan: () -> Unit = { navController.navigate(NavRoute.Scan.route) }
    val goConnectDevice: () -> Unit = {
        homeTab = HOME_TAB_CONTROL
        if (navController.currentDestination?.route != NavRoute.Main.route) {
            navController.navigate(NavRoute.Main.route) {
                launchSingleTop = true
            }
        }
    }
    val logout: () -> Unit = {
        scope.launch {
            ServiceLocator.logoutUseCase()
            homeTab = HOME_TAB_HOME
            navController.navigate(NavRoute.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    var wasLoggedIn by remember { mutableStateOf(hasPersistedSession) }
    LaunchedEffect(user) {
        if (user != null) {
            wasLoggedIn = true
        } else if (wasLoggedIn) {
            wasLoggedIn = false
            homeTab = HOME_TAB_HOME
            navController.navigate(NavRoute.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController, startDestination = start) {
        composable(NavRoute.Login.route) { entry ->
            val vm: LoginViewModel = viewModel(
                entry,
                factory = factory {
                    LoginViewModel(
                        loginUseCase = ServiceLocator.loginUseCase,
                        registerUseCase = ServiceLocator.registerUseCase,
                        verifyEmailUseCase = ServiceLocator.verifyEmailUseCase,
                        resendVerificationUseCase = ServiceLocator.resendVerificationUseCase,
                        googleAuthUseCase = ServiceLocator.googleAuthUseCase
                    )
                }
            )
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    ownedDevices.onUserLoggedIn()
                    homeTab = HOME_TAB_HOME
                    navController.navigate(NavRoute.Main.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NavRoute.Main.route) {
            HomeRoute(
                navController = navController,
                control = control,
                scanner = scanner,
                ownedDevices = ownedDevices,
                user = user,
                marquee = marquee,
                tab = homeTab,
                onTabChange = { homeTab = it },
                onLogout = logout,
                onOpenDeviceTab = goConnectDevice
            )
        }
        composable(NavRoute.Library.route) {
            HomeRoute(
                navController = navController,
                control = control,
                scanner = scanner,
                ownedDevices = ownedDevices,
                user = user,
                marquee = marquee,
                tab = HOME_TAB_LIBRARY,
                onTabChange = { homeTab = it },
                onLogout = logout,
                onOpenDeviceTab = goConnectDevice
            )
        }
        composable(NavRoute.Commission.route) {
            HomeRoute(
                navController = navController,
                control = control,
                scanner = scanner,
                ownedDevices = ownedDevices,
                user = user,
                marquee = marquee,
                tab = HOME_TAB_COMMISSION,
                onTabChange = { homeTab = it },
                onLogout = logout,
                onOpenDeviceTab = goConnectDevice
            )
        }
        composable(NavRoute.Scan.route) { MirageScanScreen(scanner) { goBack() } }
        composable(NavRoute.Effect.route) { MirageEffectScreen(control, goBack, goConnectDevice) }
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
                val characterIdArg = entry.arguments?.getString("id")
                var characterState by remember(characterIdArg) {
                    mutableStateOf<Resource<CustomerOwnedCharacterResponse>>(Resource.Loading)
                }
                LaunchedEffect(characterIdArg) {
                    val characterId = characterIdArg?.toLongOrNull()
                    characterState = if (characterId == null) {
                        Resource.Error("ID character không hợp lệ.")
                    } else {
                        when (val result = ServiceLocator.characterRepository.getOwnedCharacters()) {
                            is Resource.Success -> result.data.firstOrNull { it.characterId == characterId }
                                ?.let { Resource.Success(it) }
                                ?: Resource.Error("Không tìm thấy Character trong Inventory.")
                            is Resource.Error -> Resource.Error(result.message)
                            Resource.Loading -> Resource.Loading
                        }
                    }
                }
                when (val s = characterState) {
                    is Resource.Loading -> RouteMessage("Đang tải Character…")
                    is Resource.Error -> RouteMessage(s.message)
                    is Resource.Success -> {
                        val vm: UploadViewModel = viewModel(
                            entry,
                            factory = factory { UploadViewModel(ServiceLocator.bleUseCases) }
                        )
                        PhotoProjectionScreen(vm, goBack, goScan, s.data)
                    }
                }
            }
        }
        composable(NavRoute.Marquee.route) {
            if (marquee == null) {
                RouteMessage("Đang tải tài khoản…")
            } else {
                MirageMarqueeScreen(marquee, goBack)
            }
        }
        composable(NavRoute.PanelControl.route) { entry ->
            val id = entry.arguments?.getString("id")
            PanelControlScreen(control, id, goBack, goConnectDevice)
        }
        composable(NavRoute.ClaimDevice.route) { entry ->
            val vm: ClaimDeviceViewModel = viewModel(
                entry,
                factory = factory {
                    ClaimDeviceViewModel(
                        repository = ServiceLocator.deviceClaimRepository,
                        deviceRepository = ServiceLocator.deviceRepository,
                        bleUseCases = ServiceLocator.bleUseCases
                    )
                }
            )
            ClaimDeviceScreen(
                vm = vm,
                back = goBack,
                onFinished = { connected ->
                    ownedDevices.refresh()
                    if (connected) {
                        navController.navigate(NavRoute.PanelControl.create("current")) {
                            popUpTo(NavRoute.ClaimDevice.route) { inclusive = true }
                        }
                    } else {
                        goBack()
                    }
                }
            )
        }
        composable(NavRoute.DeviceInfo.route) { entry ->
            val id = entry.arguments?.getString("id")
            DeviceInfoScreen(
                vm = control,
                ownedDevices = ownedDevices,
                panelId = id,
                back = goBack,
                openPanelControl = { deviceId -> navController.navigate(NavRoute.PanelControl.create(deviceId)) },
                connectDevice = { mac -> scanner.connectDevice(mac) },
                disconnectDevice = { ServiceLocator.bleUseCases.disconnectDevice() }
            )
        }
    }
}

@Composable
private fun HomeRoute(
    navController: NavHostController,
    control: ControlViewModel,
    scanner: ScanViewModel,
    ownedDevices: OwnedDevicesViewModel,
    user: User?,
    marquee: MarqueeViewModel?,
    tab: Int,
    onTabChange: (Int) -> Unit,
    onLogout: () -> Unit,
    onOpenDeviceTab: () -> Unit
) {
    val controlState by control.uiState.collectAsState()
    val connected = controlState.connectionState is ConnectionState.Connected

    MirageHomeScreen(
        control = control,
        scanner = scanner,
        ownedDevices = ownedDevices,
        user = user,
        marquee = marquee,
        openScan = { navController.navigate(NavRoute.Scan.route) },
        openControl = {
            if (connected) {
                navController.navigate(NavRoute.PanelControl.create("current"))
            } else {
                onOpenDeviceTab()
            }
        },
        openCanvas = { navController.navigate(NavRoute.Studio.create(it)) },
        openMarquee = { navController.navigate(NavRoute.Marquee.route) },
        openUpload = { navController.navigate(NavRoute.Photo.route) },
        logout = onLogout,
        openEffect = { navController.navigate(NavRoute.Effect.route) },
        openClaimDevice = { navController.navigate(NavRoute.ClaimDevice.route) },
        openDeviceInfo = { id -> navController.navigate(NavRoute.DeviceInfo.create(id)) },
        selectedTab = tab,
        onTabSelected = onTabChange
    )
}

@Composable
private fun RouteMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text)
    }
}