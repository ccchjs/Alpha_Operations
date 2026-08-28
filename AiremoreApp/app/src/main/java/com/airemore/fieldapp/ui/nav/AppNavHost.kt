package com.airemore.fieldapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.airemore.fieldapp.AiremoreApp
import com.airemore.fieldapp.ui.dashboard.DashboardScreen
import com.airemore.fieldapp.ui.install.InstallFormScreen
import com.airemore.fieldapp.ui.login.LoginScreen
import com.airemore.fieldapp.ui.pm.PmFormScreen
import com.airemore.fieldapp.ui.records.RecordListScreen
import com.airemore.fieldapp.ui.repair.RepairFormScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val RECORDS = "records/{module}"
    const val PM_FORM = "pm_form/{localId}"
    const val REPAIR_FORM = "repair_form/{localId}"
    const val INSTALL_FORM = "install_form/{localId}"

    fun records(module: String) = "records/$module"
    fun pmForm(localId: Long) = "pm_form/$localId"
    fun repairForm(localId: Long) = "repair_form/$localId"
    fun installForm(localId: Long) = "install_form/$localId"
}

@Composable
fun AppNavHost(app: AiremoreApp) {
    val navController: NavHostController = rememberNavController()
    val isLoggedIn by app.session.isLoggedInFlow.collectAsState(initial = null)

    // Wait for the first DataStore read (null) before deciding the start
    // screen, so a logged-in staff member never flashes the login screen.
    if (isLoggedIn == null) return

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn == true) Routes.DASHBOARD else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(app = app, onLoggedIn = {
                navController.navigate(Routes.DASHBOARD) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                app = app,
                onOpenRecords = { module -> navController.navigate(Routes.records(module)) },
                onNewPm = { navController.navigate(Routes.pmForm(-1)) },
                onNewRepair = { navController.navigate(Routes.repairForm(-1)) },
                onNewInstall = { navController.navigate(Routes.installForm(-1)) },
                onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
            )
        }
        composable(
            Routes.RECORDS,
            arguments = listOf(navArgument("module") { type = NavType.StringType }),
        ) { entry ->
            val module = entry.arguments?.getString("module") ?: "pm"
            RecordListScreen(
                app = app,
                module = module,
                onBack = { navController.popBackStack() },
                onOpenFailedRecord = { localId ->
                    when (module) {
                        "pm" -> navController.navigate(Routes.pmForm(localId))
                        "repair" -> navController.navigate(Routes.repairForm(localId))
                        else -> navController.navigate(Routes.installForm(localId))
                    }
                },
            )
        }
        composable(
            Routes.PM_FORM,
            arguments = listOf(navArgument("localId") { type = NavType.LongType }),
        ) { entry ->
            val localId = entry.arguments?.getLong("localId") ?: -1L
            PmFormScreen(app = app, localId = localId, onDone = { navController.popBackStack(Routes.DASHBOARD, false) })
        }
        composable(
            Routes.REPAIR_FORM,
            arguments = listOf(navArgument("localId") { type = NavType.LongType }),
        ) { entry ->
            val localId = entry.arguments?.getLong("localId") ?: -1L
            RepairFormScreen(app = app, localId = localId, onDone = { navController.popBackStack(Routes.DASHBOARD, false) })
        }
        composable(
            Routes.INSTALL_FORM,
            arguments = listOf(navArgument("localId") { type = NavType.LongType }),
        ) { entry ->
            val localId = entry.arguments?.getLong("localId") ?: -1L
            InstallFormScreen(app = app, localId = localId, onDone = { navController.popBackStack(Routes.DASHBOARD, false) })
        }
    }
}
