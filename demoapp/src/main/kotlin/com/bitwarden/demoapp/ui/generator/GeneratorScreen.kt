package com.bitwarden.demoapp.ui.generator

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.demoapp.R
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.slider.BitwardenSlider
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch

private const val MIN_PASSWORD_LENGTH = 5
private const val MAX_PASSWORD_LENGTH = 40

/**
 * The password generator screen, the single screen of the Password Playground demo app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    EventsEffect(viewModel) { event ->
        when (event) {
            is GeneratorEvent.CopyToClipboard -> {
                clipboardManager.setText(AnnotatedString(event.password))
                Toast
                    .makeText(context, R.string.password_copied, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    GeneratorScreenContent(
        state = state,
        onAction = viewModel::trySendAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun GeneratorScreenContent(
    state: GeneratorState,
    onAction: (GeneratorAction) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    BitwardenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.generator_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = null,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            BitwardenTextField(
                label = stringResource(id = R.string.generated_password),
                value = state.password,
                onValueChange = { },
                readOnly = true,
                supportingText = state.strength.displayText(),
                cardStyle = CardStyle.Full,
                textFieldTestTag = "GeneratedPasswordField",
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenListHeaderText(
                label = stringResource(id = R.string.options),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenSlider(
                value = state.length,
                range = MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH,
                onValueChange = { value, _ ->
                    onAction(GeneratorAction.LengthChange(length = value))
                },
                cardStyle = CardStyle.Top(),
                sliderTag = "LengthSlider",
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )

            BitwardenSwitch(
                label = stringResource(id = R.string.include_uppercase),
                isChecked = state.useUppercase,
                onCheckedChange = {
                    onAction(GeneratorAction.ToggleUppercase(isEnabled = it))
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("UppercaseSwitch"),
            )

            BitwardenSwitch(
                label = stringResource(id = R.string.include_numbers),
                isChecked = state.useNumbers,
                onCheckedChange = {
                    onAction(GeneratorAction.ToggleNumbers(isEnabled = it))
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("NumbersSwitch"),
            )

            BitwardenSwitch(
                label = stringResource(id = R.string.include_symbols),
                isChecked = state.useSymbols,
                onCheckedChange = {
                    onAction(GeneratorAction.ToggleSymbols(isEnabled = it))
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("SymbolsSwitch"),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BitwardenFilledButton(
                label = stringResource(id = R.string.generate),
                onClick = { onAction(GeneratorAction.GenerateClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("GenerateButton"),
            )

            Spacer(modifier = Modifier.height(8.dp))

            BitwardenOutlinedButton(
                label = stringResource(id = R.string.copy),
                onClick = { onAction(GeneratorAction.CopyClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .testTag("CopyButton"),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Maps a [PasswordStrength] to its user-facing label.
 */
@Composable
private fun PasswordStrength.displayText(): String =
    when (this) {
        PasswordStrength.WEAK -> stringResource(id = R.string.strength_weak)
        PasswordStrength.GOOD -> stringResource(id = R.string.strength_good)
        PasswordStrength.STRONG -> stringResource(id = R.string.strength_strong)
    }
