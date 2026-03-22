package com.sosring.android.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sosring.android.ui.screens.DashboardScreen
import com.sosring.android.ui.screens.ContactsScreen

@Composable
fun SosRingApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(vm = vm, onManageContacts = { nav.navigate("contacts") })
        }
        composable("contacts") {
            ContactsScreen(onBack = { nav.popBackStack() })
        }
    }
}
