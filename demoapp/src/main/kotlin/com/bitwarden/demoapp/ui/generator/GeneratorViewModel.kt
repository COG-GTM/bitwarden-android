package com.bitwarden.demoapp.ui.generator

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import java.security.SecureRandom
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val NUMBER_CHARS = "0123456789"
private const val SYMBOL_CHARS = "!@#$%^&*"
private const val STRONG_LENGTH_THRESHOLD = 14
private const val GOOD_LENGTH_THRESHOLD = 10
private const val STRONG_VARIETY_THRESHOLD = 3
private const val GOOD_VARIETY_THRESHOLD = 2

/**
 * Manages state for the [GeneratorScreen] following the State-Action-Event pattern.
 */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<GeneratorState, GeneratorEvent, GeneratorAction>(
    initialState = savedStateHandle[KEY_STATE] ?: GeneratorState(),
) {
    private val random = SecureRandom()

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        if (state.password.isEmpty()) {
            trySendAction(GeneratorAction.GenerateClick)
        }
    }

    override fun handleAction(action: GeneratorAction) {
        when (action) {
            is GeneratorAction.LengthChange -> handleLengthChange(action)
            is GeneratorAction.ToggleUppercase -> handleToggleUppercase(action)
            is GeneratorAction.ToggleNumbers -> handleToggleNumbers(action)
            is GeneratorAction.ToggleSymbols -> handleToggleSymbols(action)
            GeneratorAction.GenerateClick -> handleGenerateClick()
            GeneratorAction.CopyClick -> handleCopyClick()
        }
    }

    private fun handleLengthChange(action: GeneratorAction.LengthChange) {
        mutableStateFlow.update { it.copy(length = action.length) }
    }

    private fun handleToggleUppercase(action: GeneratorAction.ToggleUppercase) {
        mutableStateFlow.update { it.copy(useUppercase = action.isEnabled) }
    }

    private fun handleToggleNumbers(action: GeneratorAction.ToggleNumbers) {
        mutableStateFlow.update { it.copy(useNumbers = action.isEnabled) }
    }

    private fun handleToggleSymbols(action: GeneratorAction.ToggleSymbols) {
        mutableStateFlow.update { it.copy(useSymbols = action.isEnabled) }
    }

    private fun handleGenerateClick() {
        val charPool = buildString {
            append(LOWERCASE_CHARS)
            if (state.useUppercase) append(UPPERCASE_CHARS)
            if (state.useNumbers) append(NUMBER_CHARS)
            if (state.useSymbols) append(SYMBOL_CHARS)
        }
        val password = (1..state.length)
            .map { charPool[random.nextInt(charPool.length)] }
            .joinToString(separator = "")
        mutableStateFlow.update { it.copy(password = password) }
    }

    private fun handleCopyClick() {
        sendEvent(GeneratorEvent.CopyToClipboard(password = state.password))
    }
}

/**
 * Models state for the generator screen.
 *
 * @property password The most recently generated password.
 * @property length The requested password length.
 * @property useUppercase Whether uppercase letters are included.
 * @property useNumbers Whether numbers are included.
 * @property useSymbols Whether symbols are included.
 */
@Parcelize
data class GeneratorState(
    val password: String = "",
    val length: Int = 14,
    val useUppercase: Boolean = true,
    val useNumbers: Boolean = true,
    val useSymbols: Boolean = false,
) : Parcelable {
    /**
     * A coarse strength rating derived from the current options.
     */
    val strength: PasswordStrength
        get() {
            val variety = 1 +
                (if (useUppercase) 1 else 0) +
                (if (useNumbers) 1 else 0) +
                (if (useSymbols) 1 else 0)
            return when {
                length >= STRONG_LENGTH_THRESHOLD &&
                    variety >= STRONG_VARIETY_THRESHOLD -> PasswordStrength.STRONG

                length >= GOOD_LENGTH_THRESHOLD &&
                    variety >= GOOD_VARIETY_THRESHOLD -> PasswordStrength.GOOD
                else -> PasswordStrength.WEAK
            }
        }
}

/**
 * A coarse password strength rating.
 */
enum class PasswordStrength {
    WEAK,
    GOOD,
    STRONG,
}

/**
 * Models actions for the generator screen.
 */
sealed class GeneratorAction {
    /**
     * The user changed the password length via the slider.
     *
     * @property length The new password length.
     */
    data class LengthChange(val length: Int) : GeneratorAction()

    /**
     * The user toggled the uppercase option.
     *
     * @property isEnabled Whether the option is now enabled.
     */
    data class ToggleUppercase(val isEnabled: Boolean) : GeneratorAction()

    /**
     * The user toggled the numbers option.
     *
     * @property isEnabled Whether the option is now enabled.
     */
    data class ToggleNumbers(val isEnabled: Boolean) : GeneratorAction()

    /**
     * The user toggled the symbols option.
     *
     * @property isEnabled Whether the option is now enabled.
     */
    data class ToggleSymbols(val isEnabled: Boolean) : GeneratorAction()

    /**
     * The user tapped the generate button.
     */
    data object GenerateClick : GeneratorAction()

    /**
     * The user tapped the copy button.
     */
    data object CopyClick : GeneratorAction()
}

/**
 * Models events emitted by the generator screen.
 */
sealed class GeneratorEvent {
    /**
     * Copy the given [password] to the system clipboard.
     *
     * @property password The password to copy.
     */
    data class CopyToClipboard(val password: String) : GeneratorEvent()
}
