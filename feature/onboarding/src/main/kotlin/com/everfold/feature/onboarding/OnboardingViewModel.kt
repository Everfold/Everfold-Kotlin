package com.everfold.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.SHOWCASE,
    val selectedMode: ConnectionMode = ConnectionMode.LOCAL_ONLY,
    val profileName: String = "",
    val profileNote: String = "",
    val transport: RemoteTransportConfig = RemoteTransportConfig.Nats(),
    val isConnecting: Boolean = false,
    val errorCode: ConnectionErrorCode? = null,
    val preferences: AppPreferences = AppPreferences(),
)

private data class OnboardingDraft(
    val step: OnboardingStep = OnboardingStep.SHOWCASE,
    val selectedMode: ConnectionMode = ConnectionMode.LOCAL_ONLY,
    val profileName: String = "",
    val profileNote: String = "",
    val transport: RemoteTransportConfig = RemoteTransportConfig.Nats(),
    val errorCode: ConnectionErrorCode? = null,
)

class OnboardingViewModel(
    private val authorityGateway: AuthorityGateway,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val draft = MutableStateFlow(OnboardingDraft())

    val uiState: StateFlow<OnboardingUiState> =
        combine(
            draft,
            authorityGateway.connectionState,
            settingsStore.preferences,
        ) { form, connState, prefs ->
            OnboardingUiState(
                step = form.step,
                selectedMode = form.selectedMode,
                profileName = form.profileName,
                profileNote = form.profileNote,
                transport = form.transport,
                isConnecting = connState is ConnectionState.Connecting,
                errorCode = form.errorCode,
                preferences = prefs,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnboardingUiState(),
        )

    fun nextStep() {
        when (draft.value.step) {
            OnboardingStep.SHOWCASE -> {
                draft.update { it.copy(step = OnboardingStep.BASIC_SETTINGS) }
            }

            OnboardingStep.BASIC_SETTINGS -> {
                draft.update { it.copy(step = OnboardingStep.PERMISSIONS) }
            }

            OnboardingStep.PERMISSIONS -> {
                draft.update { it.copy(step = OnboardingStep.CONNECTION_MODE) }
            }

            OnboardingStep.CONNECTION_MODE -> {
                if (draft.value.selectedMode == ConnectionMode.LOCAL_ONLY) {
                    viewModelScope.launch {
                        authorityGateway.connectLocal()
                        settingsStore.markOnboardingCompleted()
                    }
                } else {
                    draft.update { it.copy(step = OnboardingStep.REMOTE_CONFIG, errorCode = null) }
                }
            }

            OnboardingStep.REMOTE_CONFIG -> {
                val form = draft.value
                draft.update { it.copy(errorCode = null) }
                viewModelScope.launch {
                    val ok =
                        authorityGateway.connectRemote(
                            transport = form.transport,
                            name = form.profileName,
                            note = form.profileNote,
                        )
                    if (ok) {
                        settingsStore.markOnboardingCompleted()
                    } else {
                        val failedCode =
                            (authorityGateway.connectionState.value as? ConnectionState.Error)?.errorCode
                                ?: ConnectionErrorCode.HANDSHAKE_FAILED
                        draft.update { it.copy(errorCode = failedCode) }
                    }
                }
            }
        }
    }

    fun previousStep() {
        val prev =
            when (draft.value.step) {
                OnboardingStep.SHOWCASE -> OnboardingStep.SHOWCASE
                OnboardingStep.BASIC_SETTINGS -> OnboardingStep.SHOWCASE
                OnboardingStep.PERMISSIONS -> OnboardingStep.BASIC_SETTINGS
                OnboardingStep.CONNECTION_MODE -> OnboardingStep.PERMISSIONS
                OnboardingStep.REMOTE_CONFIG -> OnboardingStep.CONNECTION_MODE
            }
        draft.update { it.copy(step = prev, errorCode = null) }
    }

    fun skipShowcase() {
        draft.update { it.copy(step = OnboardingStep.BASIC_SETTINGS) }
    }

    fun updateLanguage(language: AppLanguage) = settingsStore.updateLanguage(language)

    fun updateThemeMode(mode: ThemeMode) = settingsStore.updateThemeMode(mode)

    fun selectColor(
        mode: ThemeColorMode,
        preset: SeedColorPreset? = null,
    ) {
        settingsStore.updateThemeColorMode(mode)
        if (preset != null) settingsStore.updateSeedColor(preset)
    }

    fun selectMode(mode: ConnectionMode) {
        draft.update { it.copy(selectedMode = mode, errorCode = null) }
    }

    fun updateProfileName(value: String) {
        draft.update { it.copy(profileName = value, errorCode = null) }
    }

    fun updateProfileNote(value: String) {
        draft.update { it.copy(profileNote = value) }
    }

    fun updateTransport(value: RemoteTransportConfig) {
        draft.update { it.copy(transport = value, errorCode = null) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
