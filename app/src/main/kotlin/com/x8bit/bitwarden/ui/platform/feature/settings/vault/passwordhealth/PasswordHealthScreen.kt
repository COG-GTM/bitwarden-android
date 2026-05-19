package com.x8bit.bitwarden.ui.platform.feature.settings.vault.passwordhealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.row.BitwardenTextRow
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.data.autofill.util.login

/**
 * Displays the password health screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordHealthScreen(
    onNavigateBack: () -> Unit,
    viewModel: PasswordHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PasswordHealthEvent.NavigateBack -> onNavigateBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.password_health),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = {
                    viewModel.trySendAction(PasswordHealthAction.BackClick)
                },
            )
        },
    ) {
        when (val viewState = state.viewState) {
            is PasswordHealthState.ViewState.Loading -> {
                BitwardenLoadingContent(
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is PasswordHealthState.ViewState.Content -> {
                if (viewState.groups.isEmpty()) {
                    PasswordHealthEmptyContent(
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    PasswordHealthContent(
                        groups = viewState.groups,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is PasswordHealthState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.generic_error_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PasswordHealthEmptyContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(id = BitwardenDrawable.ic_check_mark),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.secondary,
            modifier = Modifier
                .size(50.dp)
                .testTag("NoReusedPasswordsIcon"),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = BitwardenString.reused_passwords),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.testTag("ReusedPasswordsTitle"),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = BitwardenString.no_reused_passwords),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("NoReusedPasswordsMessage"),
        )
    }
}

@Composable
private fun PasswordHealthContent(
    groups: List<ReusedPasswordGroup>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = BitwardenString.reused_passwords),
            style = BitwardenTheme.typography.titleSmall,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier
                .standardHorizontalMargin()
                .testTag("ReusedPasswordsHeader"),
        )
        Spacer(modifier = Modifier.height(16.dp))
        groups.forEach { group ->
            ReusedPasswordGroupSection(group = group)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun ReusedPasswordGroupSection(
    group: ReusedPasswordGroup,
) {
    Column {
        Text(
            text = stringResource(
                id = BitwardenString.x_accounts_use_this_password,
                group.count,
            ),
            style = BitwardenTheme.typography.labelSmall,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .standardHorizontalMargin()
                .padding(bottom = 6.dp)
                .testTag("ReusedPasswordGroupHeader"),
        )
        group.ciphers.forEachIndexed { index, cipher ->
            val cardStyle = when {
                group.ciphers.size == 1 -> CardStyle.Full
                index == 0 -> CardStyle.Top()
                index == group.ciphers.lastIndex -> CardStyle.Bottom
                else -> CardStyle.Middle()
            }
            BitwardenTextRow(
                text = cipher.name,
                onClick = { },
                withDivider = false,
                cardStyle = cardStyle,
                modifier = Modifier
                    .testTag("ReusedPasswordCipherItem")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            ) {
                val username = cipher.login?.username
                if (!username.isNullOrEmpty()) {
                    Text(
                        text = username,
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
