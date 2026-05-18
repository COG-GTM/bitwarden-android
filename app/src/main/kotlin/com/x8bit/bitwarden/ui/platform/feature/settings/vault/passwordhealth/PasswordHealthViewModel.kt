package com.x8bit.bitwarden.ui.platform.feature.settings.vault.passwordhealth

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.platform.util.isActive
import com.x8bit.bitwarden.data.vault.manager.CipherManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.security.MessageDigest
import javax.inject.Inject

/**
 * View model for the password health screen.
 */
@HiltViewModel
class PasswordHealthViewModel @Inject constructor(
    private val vaultSyncManager: VaultSyncManager,
    private val cipherManager: CipherManager,
) : BaseViewModel<PasswordHealthState, PasswordHealthEvent, PasswordHealthAction>(
    initialState = PasswordHealthState(),
) {
    private var computeJob: Job? = null

    init {
        vaultSyncManager
            .vaultDataStateFlow
            .map { dataState ->
                PasswordHealthAction.Internal.VaultDataReceived(dataState)
            }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: PasswordHealthAction): Unit = when (action) {
        PasswordHealthAction.BackClick -> handleBackClick()
        is PasswordHealthAction.Internal -> handleInternalAction(action)
    }

    private fun handleBackClick() {
        sendEvent(PasswordHealthEvent.NavigateBack)
    }

    private fun handleInternalAction(action: PasswordHealthAction.Internal) {
        when (action) {
            is PasswordHealthAction.Internal.VaultDataReceived -> handleVaultDataReceived(action)
            is PasswordHealthAction.Internal.ReusedGroupsComputed -> handleReusedGroupsComputed(
                action,
            )
        }
    }

    private fun handleVaultDataReceived(
        action: PasswordHealthAction.Internal.VaultDataReceived,
    ) {
        when (val dataState = action.dataState) {
            is DataState.Loaded -> {
                val cipherListViews = dataState.data
                    .decryptCipherListResult
                    .successes
                computeJob?.cancel()
                computeJob = viewModelScope.launch {
                    val groups = computeReusedPasswordGroups(cipherListViews)
                    sendAction(PasswordHealthAction.Internal.ReusedGroupsComputed(groups))
                }
            }

            is DataState.Loading -> {
                computeJob?.cancel()
                mutableStateFlow.value = state.copy(
                    viewState = PasswordHealthState.ViewState.Loading,
                )
            }

            is DataState.Error -> {
                computeJob?.cancel()
                mutableStateFlow.value = state.copy(
                    viewState = PasswordHealthState.ViewState.Content(emptyList()),
                )
            }

            is DataState.NoNetwork -> {
                computeJob?.cancel()
                mutableStateFlow.value = state.copy(
                    viewState = PasswordHealthState.ViewState.Content(emptyList()),
                )
            }

            is DataState.Pending -> {
                val cipherListViews = dataState.data
                    .decryptCipherListResult
                    .successes
                computeJob?.cancel()
                computeJob = viewModelScope.launch {
                    val groups = computeReusedPasswordGroups(cipherListViews)
                    sendAction(PasswordHealthAction.Internal.ReusedGroupsComputed(groups))
                }
            }
        }
    }

    private fun handleReusedGroupsComputed(
        action: PasswordHealthAction.Internal.ReusedGroupsComputed,
    ) {
        mutableStateFlow.value = state.copy(
            viewState = PasswordHealthState.ViewState.Content(action.groups),
        )
    }

    private suspend fun computeReusedPasswordGroups(
        cipherListViews: List<CipherListView>,
    ): List<ReusedPasswordGroup> {
        val loginCiphers = cipherListViews.filter { cipherListView ->
            cipherListView.type is CipherListViewType.Login &&
                cipherListView.id != null &&
                cipherListView.isActive
        }

        val cipherPasswords = mutableListOf<Pair<CipherListView, String>>()
        for (cipherListView in loginCiphers) {
            val cipherId = cipherListView.id ?: continue
            when (val result = cipherManager.getCipher(cipherId)) {
                is GetCipherResult.Success -> {
                    val password = result.cipherView.login?.password
                    if (!password.isNullOrEmpty()) {
                        cipherPasswords.add(cipherListView to password)
                    }
                }

                else -> Unit
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val grouped = mutableMapOf<String, MutableList<CipherListView>>()
        for ((cipherListView, password) in cipherPasswords) {
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            grouped.getOrPut(hash) { mutableListOf() }.add(cipherListView)
        }

        return grouped
            .filter { it.value.size >= 2 }
            .map { (hash, ciphers) ->
                ReusedPasswordGroup(
                    id = hash,
                    ciphers = ciphers.sortedBy { it.name.lowercase() },
                    count = ciphers.size,
                )
            }
            .sortedByDescending { it.count }
    }
}

/**
 * Represents a group of ciphers that share the same password.
 */
data class ReusedPasswordGroup(
    val id: String,
    val ciphers: List<CipherListView>,
    val count: Int,
)

/**
 * Models the state for the password health screen.
 */
@Parcelize
data class PasswordHealthState(
    @IgnoredOnParcel
    val viewState: ViewState = ViewState.Loading,
) : Parcelable {

    /**
     * Represents the specific view states for the password health screen.
     */
    sealed class ViewState {
        /**
         * Loading state.
         */
        data object Loading : ViewState()

        /**
         * Content state with the reused password groups.
         */
        data class Content(
            val groups: List<ReusedPasswordGroup>,
        ) : ViewState()
    }
}

/**
 * Models events for the password health screen.
 */
sealed class PasswordHealthEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : PasswordHealthEvent()
}

/**
 * Models actions for the password health screen.
 */
sealed class PasswordHealthAction {
    /**
     * User clicked back button.
     */
    data object BackClick : PasswordHealthAction()

    /**
     * Internal actions not performed by user interaction.
     */
    sealed class Internal : PasswordHealthAction() {
        /**
         * Vault data has been received.
         */
        data class VaultDataReceived(
            val dataState: DataState<com.x8bit.bitwarden.data.vault.repository.model.VaultData>,
        ) : Internal()

        /**
         * Reused password groups have been computed.
         */
        data class ReusedGroupsComputed(
            val groups: List<ReusedPasswordGroup>,
        ) : Internal()
    }
}
