package com.skigpxrecorder.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.skigpxrecorder.data.model.DataSource
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.data.model.SessionMetadata
import com.skigpxrecorder.data.model.SessionStats
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.data.repository.GpxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

/**
 * Unit tests for FileImporter
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FileImporterTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var gpxRepository: GpxRepository

    private lateinit var fileImporter: FileImporter

    private val sampleGpx = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1">
    <trk>
        <name>Test Ski Day</name>
        <trkseg>
            <trkpt lat="45.0" lon="6.0">
                <ele>1000</ele>
                <time>2024-01-01T10:00:00Z</time>
            </trkpt>
            <trkpt lat="45.001" lon="6.001">
                <ele>900</ele>
                <time>2024-01-01T10:01:00Z</time>
            </trkpt>
        </trkseg>
    </trk>
</gpx>""".trimIndent()

    private val emptyGpx = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1">
    <trk>
        <name>Empty Track</name>
        <trkseg>
        </trkseg>
    </trk>
</gpx>""".trimIndent()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(context.contentResolver).thenReturn(contentResolver)
        // Use real SessionAnalyzer since it's a pure function
        fileImporter = FileImporter(context, SessionAnalyzer, gpxRepository)
    }

    private fun createMockUri(path: String): Uri {
        val uri = Mockito.mock(Uri::class.java)
        Mockito.`when`(uri.path).thenReturn(path)
        return uri
    }

    @Test
    fun `import valid GPX file saves to database`() = runTest {
        // Arrange
        val uri = createMockUri("/test/ski_day.gpx")
        val inputStream = ByteArrayInputStream(sampleGpx.toByteArray())
        Mockito.`when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)

        // Act
        val result = fileImporter.importFile(uri)

        // Assert
        assertTrue(result is FileImporter.ImportResult.Success)
        val successResult = result as FileImporter.ImportResult.Success
        // Session name falls back to filename when GPX metadata doesn't contain name
        assertEquals("ski_day", successResult.gpxData.metadata.sessionName)
        assertEquals(2, successResult.gpxData.trackPoints.size)
        assertEquals(DataSource.IMPORTED_GPX, successResult.gpxData.source)
        Mockito.verify(gpxRepository).createImportedSession(successResult.gpxData, "ski_day.gpx")
    }

    @Test
    fun `import empty GPX file returns error`() = runTest {
        // Arrange
        val uri = createMockUri("/test/empty.gpx")
        val inputStream = ByteArrayInputStream(emptyGpx.toByteArray())
        Mockito.`when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)

        // Act
        val result = fileImporter.importFile(uri)

        // Assert
        assertTrue(result is FileImporter.ImportResult.Error)
        val errorResult = result as FileImporter.ImportResult.Error
        assertTrue(errorResult.message.contains("No track points", ignoreCase = true))
        Mockito.verifyNoInteractions(gpxRepository)
    }

    @Test
    fun `import unsupported file type returns error`() = runTest {
        // Arrange
        val uri = createMockUri("/test/activity.fit")
        val inputStream = ByteArrayInputStream(byteArrayOf())
        Mockito.`when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)

        // Act
        val result = fileImporter.importFile(uri)

        // Assert
        assertTrue(result is FileImporter.ImportResult.Error)
        val errorResult = result as FileImporter.ImportResult.Error
        assertTrue(errorResult.message.contains("FIT", ignoreCase = true))
        Mockito.verifyNoInteractions(gpxRepository)
    }
}
