package com.example.retail360.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.retail360.util.Graph
import kotlinx.coroutines.launch

/**
 * Lets any top-level screen open the drawer without threading a lambda through
 * every screen signature. Screens read it in their TopAppBar menu button:
 *   IconButton(onClick = LocalDrawerOpener.current) { Icon(Icons.Default.Menu, ...) }
 */
val LocalDrawerOpener = staticCompositionLocalOf<() -> Unit> { {} }

enum class Role { REP, SUPERVISOR }

/** One sidebar entry. Add a module by adding a line to [drawerModules] + a route in NavGraph. */
data class DrawerModule(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val roles: Set<Role> = setOf(Role.REP, Role.SUPERVISOR)
)

private val drawerModules = listOf(
    DrawerModule(Screen.Dashboard, "Dashboard", Icons.Filled.Dashboard),
    DrawerModule(Screen.CustomerList, "Customers", Icons.Filled.Store),
    DrawerModule(Screen.Products, "Products", Icons.Filled.Inventory),
    DrawerModule(Screen.SyncStatus, "Sync", Icons.Filled.CloudSync),
    DrawerModule(Screen.Support, "Customer Support", Icons.Filled.SupportAgent),
    DrawerModule(Screen.Settings, "Settings", Icons.Filled.Settings)
)

/** Routes where the drawer is available (gesture + hamburger). Everything else is full-screen. */
private val topLevelRoutes = drawerModules.map { it.screen.route }.toSet()

/**
 * Wraps the whole NavHost. The drawer only opens on top-level routes, so a rep
 * mid-visit can't swipe out and abandon a half-recorded visit.
 */
@Composable
fun MainScaffold(
    navController: NavHostController,
    onLogout: () -> Unit,
    // TODO: replace with the signed-in user's real role once User carries one.
    currentRole: Role = Role.REP,
    content: @Composable () -> Unit
) {
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isTopLevel = currentRoute in topLevelRoutes

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevel,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                role = currentRole,
                onSelect = { screen ->
                    scope.launch { drawerState.close() }
                    if (screen.route != currentRoute) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            )
        }
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalDrawerOpener provides openDrawer) {
            content()
        }
    }
}

@Composable
private fun AppDrawer(
    currentRoute: String?,
    role: Role,
    onSelect: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = Graph.authRepository.currentUser()
    val initials = currentUser?.name?.take(1)?.uppercase() ?: "R"

    ModalDrawerSheet {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSelect(Screen.Profile) }
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = currentUser?.name ?: "Retail360 User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.email ?: "Field rep companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        drawerModules
            .filter { role in it.roles }
            .forEach { module ->
                NavigationDrawerItem(
                    icon = { Icon(module.icon, contentDescription = null) },
                    label = { Text(module.label) },
                    selected = module.screen.route == currentRoute,
                    onClick = { onSelect(module.screen) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = null) },
            label = { Text("Log out") },
            selected = false,
            onClick = onLogout,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(Modifier.height(12.dp))
    }
}
