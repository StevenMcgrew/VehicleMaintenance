package com.example.vehiclemaintenance

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vehiclemaintenance.vehicles.VehicleFormScreen
import com.example.vehiclemaintenance.vehicles.VehicleListScreen

private const val VEHICLE_ID_ARG = "vehicleId"

object Routes {
    const val VEHICLES = "vehicles"
    const val NEW_VEHICLE = "vehicles/new"
    const val EDIT_VEHICLE = "vehicles/{$VEHICLE_ID_ARG}/edit"

    fun editVehicle(vehicleId: String): String = "vehicles/$vehicleId/edit"
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
                onEditVehicle = { navController.navigate(Routes.editVehicle(it)) },
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
    }
}
