package com.example.vehiclemaintenance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.vehiclemaintenance.maintenance.MaintenanceItemFormScreen
import com.example.vehiclemaintenance.maintenance.VehicleDetailScreen
import com.example.vehiclemaintenance.servicelog.ServiceHistoryScreen
import com.example.vehiclemaintenance.servicelog.ServiceLogFormScreen
import com.example.vehiclemaintenance.vehicles.VehicleFormScreen
import com.example.vehiclemaintenance.vehicles.VehicleListScreen

/** The reminder notification opens a vehicle through this scheme; MainActivity declares it. */
const val VEHICLE_DEEP_LINK_PREFIX = "vehiclemaintenance://vehicles"

private const val VEHICLE_ID_ARG = "vehicleId"
private const val ITEM_ID_ARG = "itemId"

object Routes {
    const val VEHICLES = "vehicles"
    const val NEW_VEHICLE = "vehicles/new"
    const val EDIT_VEHICLE = "vehicles/{$VEHICLE_ID_ARG}/edit"
    const val VEHICLE_DETAIL = "vehicles/{$VEHICLE_ID_ARG}"
    const val NEW_ITEM = "vehicles/{$VEHICLE_ID_ARG}/items/new"
    const val EDIT_ITEM = "vehicles/{$VEHICLE_ID_ARG}/items/{$ITEM_ID_ARG}/edit"
    const val LOG_SERVICE = "vehicles/{$VEHICLE_ID_ARG}/items/{$ITEM_ID_ARG}/log"
    const val LOG_REPAIR = "vehicles/{$VEHICLE_ID_ARG}/repairs/new"
    const val VEHICLE_HISTORY = "vehicles/{$VEHICLE_ID_ARG}/history"

    fun editVehicle(vehicleId: String): String = "vehicles/$vehicleId/edit"

    fun vehicleDetail(vehicleId: String): String = "vehicles/$vehicleId"

    fun newItem(vehicleId: String): String = "vehicles/$vehicleId/items/new"

    fun editItem(vehicleId: String, itemId: String): String =
        "vehicles/$vehicleId/items/$itemId/edit"

    fun logService(vehicleId: String, itemId: String): String =
        "vehicles/$vehicleId/items/$itemId/log"

    fun logRepair(vehicleId: String): String = "vehicles/$vehicleId/repairs/new"

    fun vehicleHistory(vehicleId: String): String = "vehicles/$vehicleId/history"
}

@Composable
fun VehicleMaintenanceApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.VEHICLES,
        modifier = modifier,
    ) {
        composable(Routes.VEHICLES) {
            VehicleListScreen(
                onAddVehicle = { navController.navigate(Routes.NEW_VEHICLE) },
                onOpenVehicle = { navController.navigate(Routes.vehicleDetail(it)) },
            )
        }
        composable(Routes.NEW_VEHICLE) {
            VehicleFormScreen(
                vehicleId = null,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT_VEHICLE,
            arguments = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            VehicleFormScreen(
                vehicleId = backStackEntry.arguments?.getString(VEHICLE_ID_ARG),
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.VEHICLE_DETAIL,
            arguments = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "$VEHICLE_DEEP_LINK_PREFIX/{$VEHICLE_ID_ARG}" },
            ),
        ) { backStackEntry ->
            val vehicleId = backStackEntry.requireVehicleId()
            VehicleDetailScreen(
                vehicleId = vehicleId,
                onEditVehicle = { navController.navigate(Routes.editVehicle(vehicleId)) },
                onAddItem = { navController.navigate(Routes.newItem(vehicleId)) },
                onEditItem = { navController.navigate(Routes.editItem(vehicleId, it)) },
                onLogService = { navController.navigate(Routes.logService(vehicleId, it)) },
                onLogRepair = { navController.navigate(Routes.logRepair(vehicleId)) },
                onViewHistory = { navController.navigate(Routes.vehicleHistory(vehicleId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.VEHICLE_HISTORY,
            arguments = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            ServiceHistoryScreen(
                vehicleId = backStackEntry.requireVehicleId(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.NEW_ITEM,
            arguments = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            MaintenanceItemFormScreen(
                vehicleId = backStackEntry.requireVehicleId(),
                itemId = null,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.LOG_REPAIR,
            arguments = listOf(navArgument(VEHICLE_ID_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            ServiceLogFormScreen(
                vehicleId = backStackEntry.requireVehicleId(),
                itemId = null,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.LOG_SERVICE,
            arguments = listOf(
                navArgument(VEHICLE_ID_ARG) { type = NavType.StringType },
                navArgument(ITEM_ID_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            ServiceLogFormScreen(
                vehicleId = backStackEntry.requireVehicleId(),
                itemId = backStackEntry.requireItemId(),
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT_ITEM,
            arguments = listOf(
                navArgument(VEHICLE_ID_ARG) { type = NavType.StringType },
                navArgument(ITEM_ID_ARG) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            MaintenanceItemFormScreen(
                vehicleId = backStackEntry.requireVehicleId(),
                itemId = backStackEntry.arguments?.getString(ITEM_ID_ARG),
                onDone = { navController.popBackStack() },
            )
        }
    }
}

private fun androidx.navigation.NavBackStackEntry.requireVehicleId(): String =
    checkNotNull(arguments?.getString(VEHICLE_ID_ARG)) { "vehicleId is a required route argument" }

private fun androidx.navigation.NavBackStackEntry.requireItemId(): String =
    checkNotNull(arguments?.getString(ITEM_ID_ARG)) { "itemId is a required route argument" }
