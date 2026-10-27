package com.example.moodle_usth_mobile_app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.theme.CardBorderLight
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

private data class BottomNavTab(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector
)

private val BOTTOM_NAV_TABS = listOf(
    BottomNavTab(AppDestination.HOME, "Home", Icons.Default.Home),
    BottomNavTab(AppDestination.COURSES, "Courses", Icons.Default.Book),
    BottomNavTab(AppDestination.TIMETABLE, "Lịch học", Icons.Default.CalendarToday),
    BottomNavTab(AppDestination.TRACKER, "Tracker", Icons.Default.Analytics),
    BottomNavTab(AppDestination.PROFILE, "Profile", Icons.Default.Person)
)

/** Thanh điều hướng 5 tab ở dưới cùng màn hình, lấy trạng thái trực tiếp từ [viewModel]. */
@Composable
fun BottomNav(viewModel: MainViewModel) {
    val currentDestination = viewModel.currentDestination
    val selectedCourse = viewModel.selectedCourse

    Column {
        HorizontalDivider(color = CardBorderLight)
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            BOTTOM_NAV_TABS.forEach { item ->
                // Trang chi tiết môn học thuộc tab Courses (như thiết kế).
                val selected = if (selectedCourse != null) item.destination == AppDestination.COURSES
                else currentDestination == item.destination
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) PrimaryIndigo else TextSecondary
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) PrimaryIndigo else TextSecondary
                        )
                    },
                    selected = selected,
                    onClick = { viewModel.navigateTo(item.destination) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFFEEF2FF)
                    )
                )
            }
        }
    }
}
