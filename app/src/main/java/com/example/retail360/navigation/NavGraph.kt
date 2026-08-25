package com.example.retail360.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.retail360.ui.theme.screens.*

@Composable
fun NavGraph(navController: NavHostController, startDestination: String) {

    MainScaffold(
        navController = navController,
        onLogout = {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    ) {
        NavHost(navController = navController, startDestination = startDestination) {

            composable(Screen.Splash.route) {
                SplashScreen(
                    onNext = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Auth.route) {
                AuthScreen(onAuthed = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenCustomers = { navController.navigate(Screen.CustomerList.route) },
                    onOpenSync = { navController.navigate(Screen.SyncStatus.route) },
                    onOpenRoutePlan = { navController.navigate(Screen.RoutePlan.route) },
                    onOpenCheckIn = { navController.navigate(Screen.CheckInCheckout.route) },
                    onOpenInventory = { navController.navigate(Screen.Inventory.route) },
                    onOpenProducts = { navController.navigate(Screen.ProductList.route) },
                    onLoggedOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.CustomerList.route) {
                CustomerListScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(Screen.CustomerCreate.route) },
                    onOpen = { id: String -> navController.navigate(Screen.CustomerDetails.path(id)) }
                )
            }

            composable(Screen.CustomerCreate.route) {
                CustomerCreationScreen(onDone = {
                    navController.navigate(Screen.CustomerList.route) {
                        popUpTo(Screen.CustomerList.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.CustomerEdit.route) { entry ->
                val id = entry.arguments?.getString("customerId").orEmpty()
                CustomerCreationScreen(
                    onDone = { navController.popBackStack() },
                    customerId = id
                )
            }

            composable(Screen.CustomerDetails.route) { entry ->
                val id = entry.arguments?.getString("customerId").orEmpty()
                CustomerDetailsScreen(
                    customerId = id,
                    onBack = { navController.popBackStack() },
                    onCheckIn = { navController.navigate(Screen.CheckIn.path(id)) },
                    onEdit = { navController.navigate(Screen.CustomerEdit.path(id)) }
                )
            }

            composable(Screen.CheckIn.route) { entry ->
                val id = entry.arguments?.getString("customerId").orEmpty()
                CheckInScreen(
                    customerId = id,
                    onBack = { navController.popBackStack() },
                    onCheckedIn = { visitId: String ->
                        navController.navigate(Screen.ActiveVisit.path(visitId)) {
                            popUpTo(Screen.CustomerDetails.route)
                        }
                    }
                )
            }

            composable(Screen.ActiveVisit.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                ActiveVisitScreen(
                    visitId = visitId,
                    onBack = { navController.popBackStack() },
                    onScan = { navController.navigate(Screen.ProductScan.path(visitId)) },
                    onAvailability = { navController.navigate(Screen.Availability.path(visitId)) },
                    onSales = { navController.navigate(Screen.Sales.path(visitId)) },
                    onCheckOut = { navController.navigate(Screen.CheckOut.path(visitId)) }
                )
            }

            composable(Screen.ProductScan.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                ProductScanScreen(visitId = visitId, onBack = { navController.popBackStack() })
            }

            composable(Screen.Availability.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                AvailabilityScreen(visitId = visitId, onBack = { navController.popBackStack() })
            }

            composable(Screen.Sales.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                SalesScreen(visitId = visitId, onBack = { navController.popBackStack() })
            }

            composable(Screen.CheckOut.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                CheckOutScreen(
                    visitId = visitId,
                    onBack = { navController.popBackStack() },
                    onCheckedOut = { navController.navigate(Screen.VisitSummary.path(visitId)) }
                )
            }

            composable(Screen.VisitSummary.route) { entry ->
                val visitId = entry.arguments?.getString("visitId").orEmpty()
                VisitSummaryScreen(
                    visitId = visitId,
                    onDone = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SyncStatus.route) {
                SyncStatusScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.RoutePlan.route) {
                RoutePlanScreen(
                    onBack = { navController.popBackStack() },
                    onCheckIn = { customerId: String -> navController.navigate(Screen.CheckIn.path(customerId)) }
                )
            }

            composable(Screen.AddProduct.route) {
                AddProductScreen(onDone = { navController.popBackStack() })
            }

            composable(Screen.Inventory.route) {
                InventoryScreen(
                    onBack = { navController.popBackStack() },
                    onAddProduct = { navController.navigate(Screen.AddProduct.route) }
                )
            }

            composable(Screen.ProductList.route) {
                ProductListScreen(
                    onBack = { navController.popBackStack() },
                    onAddProduct = { navController.navigate(Screen.AddProduct.route) }
                )
            }

            composable(Screen.CheckInCheckout.route) {
                CustomerListScreen(
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(Screen.CustomerCreate.route) },
                    onOpen = { customerId: String -> navController.navigate(Screen.CheckIn.path(customerId)) }
                )
            }
        }
    }
}



