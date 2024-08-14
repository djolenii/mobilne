package com.example.rmasprojekat.ui


import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rmasprojekat.model.Canteen
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel


@Composable
fun MainScreen(
    isLocationEnabled: Boolean,
    canteenViewModel: CanteenViewModel,
    onShowMapClicked: () -> Unit,
    authViewModel: AuthViewModel
) {
    var selectedCanteen by remember { mutableStateOf<Canteen?>(null) }
    val navController = rememberNavController()

    if (selectedCanteen != null) {
        CanteenDetailsScreen(
            canteen = selectedCanteen!!,
            onDismiss = { selectedCanteen = null },
            canteenViewModel = canteenViewModel,
            authViewModel = authViewModel
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val items = listOf(
                        Triple("menza", "Menza", Icons.Filled.Home),
                        Triple("statusi", "Statusi", Icons.AutoMirrored.Filled.List),
                        Triple("objavi", "Objavi", Icons.Filled.Add),
                        Triple("profil", "Profil", Icons.Filled.Person)
                    )
                    items.forEach { (route, title, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = "menza", Modifier.padding(innerPadding)) {
                composable("menza") {
                    WelcomeScreen(
                        canteenViewModel = canteenViewModel,
                        onShowMapClicked = onShowMapClicked,
                        onCanteenClicked = { canteen -> selectedCanteen = canteen },
                        isLocationEnabled = isLocationEnabled
                        )
                }
                composable("statusi") { StatusScreen(canteenViewModel = canteenViewModel, authViewModel = authViewModel) }
                composable("objavi") { ObjavaScreen(canteenViewModel = canteenViewModel, authViewModel = authViewModel) }
                composable("profil") { ProfilScreen(authViewModel = authViewModel) }
            }

        }
    }
}