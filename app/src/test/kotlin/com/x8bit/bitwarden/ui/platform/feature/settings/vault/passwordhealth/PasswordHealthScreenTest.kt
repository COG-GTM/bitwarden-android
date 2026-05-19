package com.x8bit.bitwarden.ui.platform.feature.settings.vault.passwordhealth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordHealthScreenTest : BitwardenComposeTest() {

    private var onNavigateBackCalled = false
    private val mutableEventFlow = bufferedMutableSharedFlow<PasswordHealthEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)

    private val viewModel = mockk<PasswordHealthViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            PasswordHealthScreen(
                onNavigateBack = { onNavigateBackCalled = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `in Loading state should not show empty or content elements`() {
        mutableStateFlow.update {
            it.copy(viewState = PasswordHealthState.ViewState.Loading)
        }
        composeTestRule
            .onNodeWithTag("ReusedPasswordsTitle")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag("ReusedPasswordsHeader")
            .assertDoesNotExist()
    }

    @Test
    fun `in empty Content state should show empty message and icon`() {
        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHealthState.ViewState.Content(groups = emptyList()),
            )
        }
        composeTestRule
            .onNodeWithTag("NoReusedPasswordsIcon")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("ReusedPasswordsTitle")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("NoReusedPasswordsMessage")
            .assertIsDisplayed()
    }

    @Test
    fun `in Content state with groups should show group headers and cipher names`() {
        val cipherAlpha = createMockCipherListView(
            number = 1,
            name = "Alpha Site",
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1, username = "user@alpha.com"),
            ),
        )
        val cipherBeta = createMockCipherListView(
            number = 2,
            name = "Beta Site",
            type = CipherListViewType.Login(
                createMockLoginListView(number = 2, username = "user@beta.com"),
            ),
        )
        val group = ReusedPasswordGroup(
            id = "test-hash",
            ciphers = listOf(cipherAlpha, cipherBeta),
            count = 2,
        )
        mutableStateFlow.update {
            it.copy(
                viewState = PasswordHealthState.ViewState.Content(groups = listOf(group)),
            )
        }
        composeTestRule
            .onNodeWithTag("ReusedPasswordsHeader")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("2 accounts use this password")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Alpha Site")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Beta Site")
            .assertIsDisplayed()
    }

    @Test
    fun `in Error state should show error content`() {
        mutableStateFlow.update {
            it.copy(viewState = PasswordHealthState.ViewState.Error)
        }
        composeTestRule
            .onNodeWithTag("ReusedPasswordsTitle")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag("NoReusedPasswordsMessage")
            .assertDoesNotExist()
    }

    @Test
    fun `on back click should send BackClick action`() {
        every { viewModel.trySendAction(PasswordHealthAction.BackClick) } just runs
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(PasswordHealthAction.BackClick) }
    }

    @Test
    fun `NavigateBack event should call onNavigateBack`() {
        mutableEventFlow.tryEmit(PasswordHealthEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }
}

private val DEFAULT_STATE = PasswordHealthState(
    viewState = PasswordHealthState.ViewState.Loading,
)
