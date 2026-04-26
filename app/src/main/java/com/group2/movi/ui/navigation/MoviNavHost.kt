package com.group2.movi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.group2.movi.ui.auth.EmailVerifyScreen
import com.group2.movi.ui.auth.LoginScreen
import com.group2.movi.ui.auth.RegisterScreen
import com.group2.movi.ui.auth.SplashScreen
import com.group2.movi.ui.home.HomeScreen
import com.group2.movi.ui.home.TaskDetailScreen
import com.group2.movi.ui.post.PostTaskScreen
import com.group2.movi.ui.profile.EditProfileScreen
import com.group2.movi.ui.profile.EarningsScreen
import com.group2.movi.ui.profile.ProfileScreen
import com.group2.movi.ui.profile.RealNameVerifyScreen
import com.group2.movi.ui.profile.ReviewsScreen
import com.group2.movi.ui.profile.ScheduleScreen
import com.group2.movi.ui.tasks.ChatScreen
import com.group2.movi.ui.tasks.DeliveryConfirmScreen
import com.group2.movi.ui.tasks.MyTasksScreen
import com.group2.movi.ui.tasks.TaskProgressScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviNavHost(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) MoviBottomBar(navController = navController, currentRoute = currentRoute)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(if (showBottomBar) padding else PaddingValues(0.dp))
        ) {
            // Auth flow
            composable(Screen.Splash.route) {
                SplashScreen(
                    onLoggedInVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onLoggedInUnverified = {
                        navController.navigate(Screen.EmailVerify.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNotLoggedIn = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onLoginNeedsVerify = {
                        navController.navigate(Screen.EmailVerify.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onGoToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegistered = {
                        navController.navigate(Screen.EmailVerify.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBackToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.EmailVerify.route) {
                EmailVerifyScreen(
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.EmailVerify.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Bottom nav destinations
            composable(Screen.Home.route) {
                HomeScreen(
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.create(taskId)) }
                )
            }
            composable(Screen.Post.route) {
                PostTaskScreen(
                    onPosted = {
                        navController.navigate(Screen.MyTasks.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
            composable(Screen.MyTasks.route) {
                MyTasksScreen(
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskProgress.create(taskId)) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onRealNameVerify = { navController.navigate(Screen.RealNameVerify.route) },
                    onSchedule = { navController.navigate(Screen.Schedule.route) },
                    onEarnings = { navController.navigate(Screen.Earnings.route) },
                    onReviews = { navController.navigate(Screen.Reviews.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Detail screens
            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(navArgument(Screen.TaskDetail.ARG) { type = NavType.StringType })
            ) { entry ->
                val taskId = entry.arguments?.getString(Screen.TaskDetail.ARG) ?: return@composable
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAccepted = {
                        navController.navigate(Screen.TaskProgress.create(taskId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onChat = { navController.navigate(Screen.Chat.create(taskId)) },
                    vm = hiltViewModel(entry)
                )
            }

            composable(
                route = Screen.TaskProgress.route,
                arguments = listOf(navArgument(Screen.TaskProgress.ARG) { type = NavType.StringType })
            ) { entry ->
                val taskId = entry.arguments?.getString(Screen.TaskProgress.ARG) ?: return@composable
                TaskProgressScreen(
                    onBack = { navController.popBackStack() },
                    onChat = { navController.navigate(Screen.Chat.create(taskId)) },
                    onConfirmDelivery = { navController.navigate(Screen.DeliveryConfirm.create(taskId)) },
                    vm = hiltViewModel(entry)
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument(Screen.Chat.ARG) { type = NavType.StringType })
            ) { entry ->
                ChatScreen(
                    onBack = { navController.popBackStack() },
                    vm = hiltViewModel(entry)
                )
            }

            composable(
                route = Screen.DeliveryConfirm.route,
                arguments = listOf(navArgument(Screen.DeliveryConfirm.ARG) { type = NavType.StringType })
            ) { entry ->
                DeliveryConfirmScreen(
                    onDone = { navController.popBackStack() },
                    vm = hiltViewModel(entry)
                )
            }

            // Profile sub-screens
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onVerifyRealName = { navController.navigate(Screen.RealNameVerify.route) }
                )
            }
            composable(Screen.RealNameVerify.route) {
                RealNameVerifyScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Earnings.route) {
                EarningsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Reviews.route) {
                ReviewsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun MoviBottomBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        NavItem(Screen.Home.route, "Home", Icons.Outlined.Home),
        NavItem(Screen.Post.route, "Post", Icons.Outlined.Add),
        NavItem(Screen.MyTasks.route, "My Tasks", Icons.Outlined.Assignment),
        NavItem(Screen.Profile.route, "Profile", Icons.Outlined.Person)
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
