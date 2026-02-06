package com.skigpxrecorder.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Navigation drawer content
 */
@Composable
fun AppDrawer(
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onOpenFile: () -> Unit = {}
) {
    val context = LocalContext.current

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // App Title
            Text(
                text = "SkiGPX Recorder",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items
            DrawerItem(
                icon = Icons.Default.Home,
                label = "New Recording",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate(Screen.Start.route) {
                            popUpTo(Screen.Start.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            )

            DrawerItem(
                icon = Icons.Default.List,
                label = "Session History",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate(Screen.SessionHistory.route)
                    }
                }
            )

            DrawerItem(
                icon = Icons.Default.Add,
                label = "Open File",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        onOpenFile()
                    }
                }
            )

            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = {
                    scope.launch {
                        drawerState.close()
                        navController.navigate(Screen.Settings.route)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // Support link
            DrawerItem(
                icon = Icons.Default.Info,
                label = "Support",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/pashol/SkiGPXRecorder"))
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
