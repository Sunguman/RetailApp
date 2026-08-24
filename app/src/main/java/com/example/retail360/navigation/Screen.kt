package com.example.retail360.navigation

/**
 * Every destination + its route pattern. Screens that need an id use a
 * templated route and a helper to build the concrete path.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Dashboard : Screen("dashboard")
    data object CustomerList : Screen("customers")
    data object CustomerCreate : Screen("customers/create")
    data object SyncStatus : Screen("sync")
    data object Settings : Screen("settings")
    data object RoutePlan : Screen("route-plan")
    data object Inventory : Screen("inventory")
    data object CheckInCheckout : Screen("checkin-list")

    data object CustomerEdit : Screen("customers/{customerId}/edit") {
        fun path(customerId: String) = "customers/$customerId/edit"
    }

    data object CustomerDetails : Screen("customers/{customerId}") {
        fun path(customerId: String) = "customers/$customerId"
    }

    data object CheckIn : Screen("checkin/{customerId}") {
        fun path(customerId: String) = "checkin/$customerId"
    }

    data object ActiveVisit : Screen("visit/{visitId}") {
        fun path(visitId: String) = "visit/$visitId"
    }

    data object ProductScan : Screen("visit/{visitId}/scan") {
        fun path(visitId: String) = "visit/$visitId/scan"
    }

    data object Availability : Screen("visit/{visitId}/availability") {
        fun path(visitId: String) = "visit/$visitId/availability"
    }

    data object Sales : Screen("visit/{visitId}/sales") {
        fun path(visitId: String) = "visit/$visitId/sales"
    }

    data object CheckOut : Screen("visit/{visitId}/checkout") {
        fun path(visitId: String) = "visit/$visitId/checkout"
    }

    data object VisitSummary : Screen("visit/{visitId}/summary") {
        fun path(visitId: String) = "visit/$visitId/summary"
    }
}


