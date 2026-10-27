package com.example.moodle_usth_mobile_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodle_usth_mobile_app.reminders.ReminderReceiver
import com.example.moodle_usth_mobile_app.ui.AppDestination
import com.example.moodle_usth_mobile_app.ui.Formatters
import com.example.moodle_usth_mobile_app.ui.MainViewModel
import com.example.moodle_usth_mobile_app.ui.components.BottomNav
import com.example.moodle_usth_mobile_app.ui.dialogs.AppSnackbar
import com.example.moodle_usth_mobile_app.ui.dialogs.AssignmentDetailDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.ClassworkListDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.CourseInfoDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.GradeReportDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.NotificationsDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.PlannerDialog
import com.example.moodle_usth_mobile_app.ui.dialogs.SemesterOverviewDialog
import com.example.moodle_usth_mobile_app.ui.screens.CourseDetailScreen
import com.example.moodle_usth_mobile_app.ui.screens.CoursesScreen
import com.example.moodle_usth_mobile_app.ui.screens.HomeScreen
import com.example.moodle_usth_mobile_app.ui.screens.LoginScreen
import com.example.moodle_usth_mobile_app.ui.screens.ProfileScreen
import com.example.moodle_usth_mobile_app.ui.screens.TimetableScreen
import com.example.moodle_usth_mobile_app.ui.screens.TrackerScreen
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmber
import com.example.moodle_usth_mobile_app.ui.theme.AccentAmberLight
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoral
import com.example.moodle_usth_mobile_app.ui.theme.AccentCoralLight
import com.example.moodle_usth_mobile_app.ui.theme.MoodleusthmobileappTheme
import com.example.moodle_usth_mobile_app.ui.theme.PrimaryIndigo
import com.example.moodle_usth_mobile_app.ui.theme.SurfaceLight
import com.example.moodle_usth_mobile_app.ui.theme.TextPrimary
import com.example.moodle_usth_mobile_app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    // ViewModel giữ dữ liệu của cả app (không bị mất khi xoay màn hình).
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openAssignmentFrom(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE)
        )
        setContent {
            MoodleusthmobileappTheme(darkTheme = false) {
                MoodleAppRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    /** Chạm thông báo nhắc hạn nộp khi app đang mở (launchMode="singleTop"). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openAssignmentFrom(intent)
    }

    /** Nếu app được mở từ thông báo nhắc hạn nộp thì mở luôn bài tập đó. */
    private fun openAssignmentFrom(intent: Intent?) {
        if (intent == null) return
        val assignmentId = intent.getStringExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID) ?: return
        // Xoá đi để khi xoay màn hình không mở lại lần nữa.
        intent.removeExtra(ReminderReceiver.EXTRA_ASSIGNMENT_ID)
        viewModel.openAssignment(assignmentId)
    }
}

@Composable
fun MoodleAppRoot(viewModel: MainViewModel) {
    // Khung tối đa 430dp căn giữa khi mở trên màn hình lớn (tablet/desktop), vừa khít 100% trên điện thoại.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .fillMaxSize()
                .background(SurfaceLight)
        ) {
            if (viewModel.isLoggedIn) {
                SignedInApp(viewModel)
            } else {
                LoginScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInApp(viewModel: MainViewModel) {
    val currentDestination = viewModel.currentDestination
    val selectedCourse = viewModel.selectedCourse
    val data = viewModel.data
    val stateHolder = rememberSaveableStateHolder()

    // Mỗi khi ViewModel có thông báo mới → hiện snackbar, hiện xong thì xoá.
    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            AppSnackbar.state.showSnackbar(message)
            viewModel.message = null
        }
    }

    // Back: đóng trang chi tiết môn học trước, rồi quay lại tab trước đó / Home.
    BackHandler(enabled = selectedCourse != null || currentDestination != AppDestination.HOME) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SurfaceLight,
        // Khi đang mở dialog, AppModal tự hiển thị snackbar bên trong nên ẩn bản ở màn nền.
        snackbarHost = { if (viewModel.dialogName == "") SnackbarHost(AppSnackbar.state) },
        bottomBar = { BottomNav(viewModel) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (data != null && (viewModel.isOffline || viewModel.isDemoMode)) {
                val syncedAtMs = viewModel.lastSyncedAtMs
                StatusBanner(
                    isDemo = viewModel.isDemoMode,
                    syncedAtText = if (syncedAtMs != null) Formatters.syncedAt(syncedAtMs, viewModel.nowMs) else null,
                    onRetry = { viewModel.refresh() }
                )
            }

            val loadError = viewModel.loadError
            if (data == null && currentDestination == AppDestination.TRACKER && selectedCourse == null) {
                // Màn Tracker có giao diện Skeleton và Error riêng.
                TrackerScreen(viewModel = viewModel)
            } else if (data == null && loadError != null) {
                // Chưa có dữ liệu và tải bị lỗi
                ErrorState(
                    message = loadError,
                    onRetry = { viewModel.refresh(initial = true) },
                    onLogout = { viewModel.logout() }
                )
            } else if (data == null) {
                // Đang tải lần đầu
                LoadingState()
            } else {
                PullToRefreshBox(
                    isRefreshing = viewModel.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val screenKey = if (selectedCourse != null) "course:${selectedCourse.id}" else "tab:${currentDestination.name}"
                    stateHolder.SaveableStateProvider(screenKey) {
                        if (selectedCourse != null) {
                            CourseDetailScreen(viewModel = viewModel)
                        } else {
                            when (currentDestination) {
                                AppDestination.HOME -> HomeScreen(viewModel = viewModel)
                                AppDestination.COURSES -> CoursesScreen(viewModel = viewModel)
                                AppDestination.TIMETABLE -> TimetableScreen(viewModel = viewModel)
                                AppDestination.TRACKER -> TrackerScreen(viewModel = viewModel)
                                AppDestination.PROFILE -> ProfileScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }

        // Dialog đang mở (nếu có). dialogArg là id bài tập / id môn học / bộ lọc.
        val onDismiss = { viewModel.closeDialog() }
        val arg = viewModel.dialogArg
        when (viewModel.dialogName) {
            "assignment" -> AssignmentDetailDialog(arg, viewModel, onDismiss)
            "classwork" -> ClassworkListDialog(if (arg == "") "todo" else arg, viewModel, onDismiss)
            "notifications" -> NotificationsDialog(viewModel, onDismiss)
            "courseInfo" -> CourseInfoDialog(arg, viewModel, onDismiss)
            "planner" -> PlannerDialog(viewModel, onDismiss)
            "gradeReport" -> GradeReportDialog(viewModel, onDismiss)
            "semester" -> SemesterOverviewDialog(viewModel, onDismiss)
        }
    }
}

@Composable
private fun StatusBanner(isDemo: Boolean, syncedAtText: String?, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDemo) AccentAmberLight else AccentCoralLight)
            .clickable(enabled = !isDemo, onClick = onRetry)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isDemo) Icons.Default.Info else Icons.Default.CloudOff,
            contentDescription = null,
            tint = if (isDemo) AccentAmber else AccentCoral,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isDemo) "Chế độ xem thử · dữ liệu mẫu, không kết nối máy chủ"
            else if (syncedAtText != null) "Ngoại tuyến · dữ liệu lúc $syncedAtText. Chạm để thử lại."
            else "Ngoại tuyến · đang xem dữ liệu đã lưu. Chạm để thử lại.",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryIndigo)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Đang đồng bộ với Moodle…", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(AccentCoralLight, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Không tải được dữ liệu", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Thử lại", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Đăng xuất")
        }
    }
}
