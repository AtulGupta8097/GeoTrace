package com.geofencing.tracker.presentation.main_screen.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.geofencing.tracker.presentation.navigation.Navbar
import com.geofencing.tracker.presentation.navigation.Routes

@Composable
fun BottomBar(
    current: NavKey,
    onNavigate: (Routes) -> Unit
) {
    NavigationBar {
        Navbar.items.forEach { item ->
            val isSelected = current == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) {
                            item.activeIcon
                        } else {
                            item.inactiveIcon
                        },
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) }
            )
        }
    }
}
