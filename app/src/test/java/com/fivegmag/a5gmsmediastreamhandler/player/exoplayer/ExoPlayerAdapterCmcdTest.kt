package com.fivegmag.a5gmsmediastreamhandler.player.exoplayer

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.upstream.CmcdConfiguration
import androidx.media3.ui.PlayerView
import com.fivegmag.a5gmscommonlibrary.cmcd.CmcdConfiguration as CmcdConfig
import com.fivegmag.a5gmscommonlibrary.cmcd.CmcdRequest
import com.fivegmag.a5gmscommonlibrary.cmcd.CmcdTransmissionMode
import com.fivegmag.a5gmscommonlibrary.cmcd.CmcdType
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.ArrayList

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ExoPlayerAdapterCmcdTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var playerView: PlayerView

    private lateinit var exoPlayerAdapter: ExoPlayerAdapter

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        exoPlayerAdapter = ExoPlayerAdapter()
        // We don't need to call initialize() for this test as we are testing createCmcdConfigurationFactory logic
        // which depends on setCmcdConfiguration
    }

    @Test
    fun `test createCmcdConfigurationFactory generates correct configuration`() {
        // 1. Setup Input Data
        val sessionId = "test-session-id"
        val contentId = "test-content-id"
        val allowedKey = "br"
        
        val cmcdConfig = CmcdConfig(CmcdType.REQUEST, ArrayList(listOf(allowedKey)))
        val cmcdRequest = CmcdRequest(
            cmcdConfigurations = ArrayList(listOf(cmcdConfig)),
            contentId = contentId,
            transmissionMode = CmcdTransmissionMode.HTTP_HEADER
        )

        // 2. Configure Adapter
        exoPlayerAdapter.setCmcdConfiguration(cmcdRequest, sessionId)

        // 3. Access private factory method using Reflection
        val createFactoryMethod = ExoPlayerAdapter::class.java.getDeclaredMethod("createCmcdConfigurationFactory")
        createFactoryMethod.isAccessible = true
        val factory = createFactoryMethod.invoke(exoPlayerAdapter) as CmcdConfiguration.Factory

        // 4. Create Configuration
        val mediaItem = mockk<MediaItem>()
        val config = factory.createCmcdConfiguration(mediaItem)

        // 5. Assertions
        assertNotNull("CmcdConfiguration should not be null", config)
        assertEquals("Session ID should match", sessionId, config.sessionId)
        assertEquals("Content ID should match", contentId, config.contentId)
        
        // precise mapping check for MODE_REQUEST_HEADER (value 0) vs MODE_QUERY_PARAMETER (value 1)
        // In ExoPlayer: MODE_REQUEST_HEADER = 0, MODE_QUERY_PARAMETER = 1
        assertEquals("Transmission mode should be HTTP_HEADER (0)", CmcdConfiguration.MODE_REQUEST_HEADER, config.dataTransmissionMode)

        // Key Allowance checks
        assertTrue("Session ID (sid) must always be allowed", config.requestConfig.isKeyAllowed("sid"))
        assertTrue("Whitelisted key (br) should be allowed", config.requestConfig.isKeyAllowed("br"))
        assertFalse("Non-whitelisted key (d) should NOT be allowed", config.requestConfig.isKeyAllowed("d"))
    }

    @Test
    fun `test createCmcdConfigurationFactory uses query parameter mode`() {
         // 1. Setup Input Data
        val sessionId = "test-session-id-2"
        val cmcdRequest = CmcdRequest(
             cmcdConfigurations = ArrayList(), // Empty config but valid request
             contentId = null,
             transmissionMode = CmcdTransmissionMode.QUERY_PARAMETER
        )

        // 2. Configure Adapter
        exoPlayerAdapter.setCmcdConfiguration(cmcdRequest, sessionId)

        // 3. Access private factory method using Reflection
        val createFactoryMethod = ExoPlayerAdapter::class.java.getDeclaredMethod("createCmcdConfigurationFactory")
        createFactoryMethod.isAccessible = true
        val factory = createFactoryMethod.invoke(exoPlayerAdapter) as CmcdConfiguration.Factory

        // 4. Create Configuration
        val mediaItem = mockk<MediaItem>()
        val config = factory.createCmcdConfiguration(mediaItem)

        // 5. Assertions
        assertEquals("Transmission mode should be QUERY_PARAMETER (1)", CmcdConfiguration.MODE_QUERY_PARAMETER, config.dataTransmissionMode)
    }
}
