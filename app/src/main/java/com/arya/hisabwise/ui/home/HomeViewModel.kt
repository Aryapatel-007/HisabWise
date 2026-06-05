package com.arya.hisabwise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.repository.HisabRepository
import com.arya.hisabwise.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val hisabRepository: HisabRepository,
    private val userPreferences: UserPreferencesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _workspaceMode = MutableStateFlow("local")
    val workspaceMode: StateFlow<String> = _workspaceMode.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.authType.collect { type ->
                _workspaceMode.value = type
                if (type == "global") {
                    hisabRepository.startRealtimeSync()
                }
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val hisabs: StateFlow<List<HisabEntity>> = _workspaceMode
        .flatMapLatest { mode ->
            hisabRepository.getHisabsByMode(mode == "global")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isLoggedIn: StateFlow<Boolean> = userPreferences.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
        
    val userFirstName: StateFlow<String> = userPreferences.userFirstName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun onToggleWorkspace(toGlobal: Boolean, onRequireAuth: () -> Unit) {
        viewModelScope.launch {
            if (toGlobal) {
                if (isLoggedIn.value) {
                    _workspaceMode.value = "global"
                    userPreferences.updateAuthMode("global")
                    hisabRepository.startRealtimeSync()
                } else {
                    onRequireAuth()
                }
            } else {
                _workspaceMode.value = "local"
                userPreferences.updateAuthMode("local")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            userPreferences.clearAuthData()
            onSuccess()
        }
    }

    fun duplicateHisab(hisab: HisabEntity) {
        viewModelScope.launch {
            val duplicate = HisabEntity(
                name = "${hisab.name}(copy)",
                netAmount = 0.0,
                budgetPerPerson = hisab.budgetPerPerson,
                createdAt = System.currentTimeMillis()
            )
            hisabRepository.insertHisab(duplicate)
        }
    }

    fun deleteHisab(hisab: HisabEntity) {
        viewModelScope.launch {
            hisabRepository.deleteHisab(hisab)
        }
    }

    fun renameHisab(id: Int, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            hisabRepository.updateHisabName(id, newName)
        }
    }

    // A method to create a new Hisab - just to support the "+ Add Hisab" flow if needed
    // The instructions say "Clicking this triggers onNavigateToAddHisab() callback", 
    // so creating is handled elsewhere (or we can just add a placeholder).
    // The instruction doesn't specify adding dummy data, just navigating.
}
