package com.arya.hisabwise.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class OnboardingState {
    IDLE, LOADING, SUCCESS, ERROR
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val repository: UserPreferencesRepository
) : ViewModel() {

    private val _onboardingState = MutableStateFlow(OnboardingState.IDLE)
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun registerUser(name: String, phone: String, pass: String) {
        _onboardingState.value = OnboardingState.LOADING
        viewModelScope.launch {
            try {
                val email = "$phone@hisabwise.local"
                auth.createUserWithEmailAndPassword(email, pass).await()
                
                val user = auth.currentUser
                if (user != null) {
                    val userData = hashMapOf(
                        "name" to name,
                        "phone" to phone
                    )
                    firestore.collection("users").document(user.uid).set(userData).await()
                    
                    repository.updateAuthData(
                        loggedIn = true, 
                        type = "global", 
                        firstName = name, 
                        phone = phone
                    )
                    repository.saveOnboardingData(name = name, phone = phone)
                    
                    _onboardingState.value = OnboardingState.SUCCESS
                } else {
                    throw Exception("Failed to get current user")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Registration failed"
                _onboardingState.value = OnboardingState.ERROR
            }
        }
    }
}
