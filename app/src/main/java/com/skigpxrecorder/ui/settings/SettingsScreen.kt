package com.skigpxrecorder.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.skigpxrecorder.BuildConfig
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.ui.theme.AppCardDefaults

/**
 * Settings screen for language and unit preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val unitSystem by viewModel.unitSystem.collectAsState()
    val language by viewModel.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PREFERENCES SECTION
            item {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            // Unit System Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Unit System",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var unitExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = unitSystem.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                UserPreferences.UnitSystem.values().forEach { system ->
                                    DropdownMenuItem(
                                        text = { Text(system.name) },
                                        onClick = {
                                            viewModel.setUnitSystem(system)
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Language",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var languageExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = languageExpanded,
                            onExpandedChange = { languageExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = when (language) {
                                    UserPreferences.Language.EN -> "English"
                                    UserPreferences.Language.IT -> "Italiano"
                                    UserPreferences.Language.DE -> "Deutsch"
                                    UserPreferences.Language.FR -> "Français"
                                },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false }
                            ) {
                                UserPreferences.Language.values().forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (lang) {
                                                    UserPreferences.Language.EN -> "English"
                                                    UserPreferences.Language.IT -> "Italiano"
                                                    UserPreferences.Language.DE -> "Deutsch"
                                                    UserPreferences.Language.FR -> "Français"
                                                }
                                            )
                                        },
                                        onClick = {
                                            viewModel.setLanguage(lang)
                                            languageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // RECORDING SECTION
            item {
                Text(
                    text = "RECORDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            // Auto-pause Switch (Placeholder)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Auto-pause",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Coming soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = {},
                            enabled = false
                        )
                    }
                }
            }

            // GPS Filtering Switch (Placeholder)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "GPS Filtering",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Coming soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Switch(
                            checked = false,
                            onCheckedChange = {},
                            enabled = false
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ABOUT SECTION
            item {
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }

            // Support
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "GitHub Repository",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/pashol/SkiGPXRecorder")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // App Version
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = AppCardDefaults.elevation,
                    shape = AppCardDefaults.shape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
