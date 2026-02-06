package com.skigpxrecorder.ui.session.analysis

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.ui.charts.SpeedHistogram
import com.skigpxrecorder.ui.session.trackview.StatCard
import com.skigpxrecorder.util.UnitConverter

/**
 * Analysis view showing performance metrics and distributions
 */
@Composable
fun AnalysisView(
    gpxData: GPXData,
    unitSystem: UserPreferences.UnitSystem,
    modifier: Modifier = Modifier
) {
    val stats = gpxData.stats

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Score
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PerformanceScoreCircle(score = stats.performanceScore)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score based on runs, speed, and vertical metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // Speed Distribution
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Speed Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SpeedHistogram(
                        speedBuckets = stats.speedDistribution.ifEmpty { listOf(0, 0, 0, 0, 0, 0) },
                        bucketLabels = listOf("0-10", "10-20", "20-30", "30-40", "40-50", "50+")
                    )
                }
            }
        }

        // Time Distribution
        item {
            TimeDistributionBar(
                movingTime = stats.movingTime,
                stationaryTime = stats.stationaryTime,
                ascendingTime = stats.ascendingTime,
                descendingTime = stats.descendingTime
            )
        }

        // Elevation Analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Elevation Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Ski Vertical",
                            value = UnitConverter.formatElevation(stats.skiVertical, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Total Ascent",
                            value = UnitConverter.formatElevation(stats.totalAscent, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Descent",
                            value = UnitConverter.formatElevation(stats.totalDescent, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Altitude Range",
                            value = "${UnitConverter.formatElevation(stats.minAltitude, unitSystem)} - ${UnitConverter.formatElevation(stats.maxAltitude, unitSystem)}",
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Run Analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Run Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Runs",
                            value = "${gpxData.runs.size}",
                            unit = "runs",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Avg Run Distance",
                            value = if (gpxData.runs.isNotEmpty()) {
                                UnitConverter.formatDistance(
                                    stats.skiDistance / gpxData.runs.size,
                                    unitSystem
                                )
                            } else "0",
                            unit = if (gpxData.runs.isNotEmpty()) {
                                UnitConverter.getDistanceUnit(unitSystem, stats.skiDistance / gpxData.runs.size)
                            } else {
                                UnitConverter.getDistanceUnit(unitSystem, 0f)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Avg Run Vertical",
                            value = if (gpxData.runs.isNotEmpty()) {
                                UnitConverter.formatElevation(
                                    stats.skiVertical / gpxData.runs.size,
                                    unitSystem
                                )
                            } else "0",
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Avg Ski Speed",
                            value = UnitConverter.formatSpeed(stats.avgSkiSpeed, unitSystem),
                            unit = UnitConverter.getSpeedUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Heart Rate Zones (if available)
        if (stats.avgHeartRate != null && stats.maxHeartRate != null) {
            item {
                HeartRateZones(
                    avgHeartRate = stats.avgHeartRate,
                    maxHeartRate = stats.maxHeartRate
                )
            }
        }
    }
}
