package com.group2.movi.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object EmailVerify : Screen("email_verify")

    data object Home : Screen("home")
    data object Post : Screen("post")
    data object MyTasks : Screen("my_tasks")
    data object Profile : Screen("profile")

    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun create(taskId: String) = "task_detail/$taskId"
        const val ARG = "taskId"
    }

    data object TaskProgress : Screen("task_progress/{taskId}") {
        fun create(taskId: String) = "task_progress/$taskId"
        const val ARG = "taskId"
    }

    data object Chat : Screen("chat/{taskId}") {
        fun create(taskId: String) = "chat/$taskId"
        const val ARG = "taskId"
    }

    data object DeliveryConfirm : Screen("delivery_confirm/{taskId}") {
        fun create(taskId: String) = "delivery_confirm/$taskId"
        const val ARG = "taskId"
    }

    data object EditProfile : Screen("edit_profile")
    data object RealNameVerify : Screen("real_name_verify")
    data object Schedule : Screen("schedule")
    data object Earnings : Screen("earnings")
    data object Reviews : Screen("reviews")
}

/** Top-level destinations with bottom nav. */
val bottomNavRoutes = listOf(Screen.Home.route, Screen.Post.route, Screen.MyTasks.route, Screen.Profile.route)
