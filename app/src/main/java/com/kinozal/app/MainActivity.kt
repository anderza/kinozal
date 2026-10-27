package com.kinozal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kinozal.app.ui.catalog.CatalogScreen
import com.kinozal.app.ui.catalog.CatalogViewModel
import com.kinozal.app.ui.catalog.MovieDetailsScreen
import com.kinozal.app.ui.catalog.MovieDetailsViewModel
import com.kinozal.app.ui.free.FreeDetailsScreen
import com.kinozal.app.ui.free.FreeDetailsViewModel
import com.kinozal.app.ui.free.FreeScreen
import com.kinozal.app.ui.free.FreeViewModel
import com.kinozal.app.ui.settings.SettingsScreen
import com.kinozal.app.ui.theme.KinozalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KinozalApp
        setContent {
            KinozalTheme { KinozalNavigation(app) }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Catalog("catalog", "Каталог", Icons.Filled.LocalMovies),
    Free("free", "Бесплатно", Icons.Filled.PlayCircle),
    Settings("settings", "Настройки", Icons.Filled.Settings),
}

@Composable
private fun KinozalNavigation(app: KinozalApp) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val tabRoutes = setOf("catalog", "free?q={q}", "settings")
    val showBar = currentRoute == null || currentRoute in tabRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute?.substringBefore('?') == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "catalog",
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        ) {
            composable("catalog") {
                val vm = viewModel { CatalogViewModel(app.tmdb, app.settings) }
                Screen(withStatusBar = true) {
                    CatalogScreen(
                        viewModel = vm,
                        onMovieClick = { id -> nav.navigate("movie/$id") },
                        onOpenSettings = { nav.switchTab("settings") },
                    )
                }
            }
            composable(
                "free?q={q}",
                arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val query = entry.arguments?.getString("q")
                val vm = viewModel { FreeViewModel(app.archive, query) }
                Screen(withStatusBar = true) {
                    FreeScreen(vm, onMovieClick = { id -> nav.navigate("free/item/${android.net.Uri.encode(id)}") })
                }
            }
            composable("settings") {
                Screen(withStatusBar = true) { SettingsScreen(app.settings) }
            }
            composable("movie/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                val id = entry.arguments!!.getInt("id")
                val vm = viewModel { MovieDetailsViewModel(app.tmdb, id) }
                MovieDetailsScreen(
                    viewModel = vm,
                    onBack = { nav.popBackStack() },
                    onFindFree = { title -> nav.navigate("free?q=${android.net.Uri.encode(title)}") },
                )
            }
            composable("free/item/{id}") { entry ->
                val id = entry.arguments!!.getString("id")!!
                val vm = viewModel { FreeDetailsViewModel(app.archive, id) }
                FreeDetailsScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun Screen(withStatusBar: Boolean, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(if (withStatusBar) Modifier.statusBarsPadding() else Modifier) {
        content()
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
