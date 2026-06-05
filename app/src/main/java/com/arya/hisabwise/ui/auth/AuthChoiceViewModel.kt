package com.arya.hisabwise.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.UserPreferencesRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class AuthState {
    IDLE, LOADING, SUCCESS, ERROR
}

@HiltViewModel
class AuthChoiceViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var verificationId: String? = null

    fun setLocalMode() {
        viewModelScope.launch {
            preferencesRepository.updateAuthData(loggedIn = false, type = "local", firstName = "", phone = "")
            // Also need to set onboarding done to skip page 1
            preferencesRepository.saveOnboardingData("", "")
        }
    }

    fun loginWithPassword(number: String, pass: String) {
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            try {
                val email = "$number@hisabwise.local"
                auth.signInWithEmailAndPassword(email, pass).await()
                
                val user = auth.currentUser
                if (user != null) {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    val name = doc.getString("name") ?: ""
                    
                    preferencesRepository.updateAuthData(
                        loggedIn = true, 
                        type = "global", 
                        firstName = name, 
                        phone = number
                    )
                    preferencesRepository.saveOnboardingData(name, number)
                    _authState.value = AuthState.SUCCESS
                } else {
                    throw Exception("User not found")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed"
                _authState.value = AuthState.ERROR
            }
        }
    }

    fun sendOtp(number: String, activity: Activity, onCodeSent: () -> Unit) {
        _authState.value = AuthState.LOADING
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval
                signInWithPhoneAuthCredential(credential, number)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _errorMessage.value = e.message ?: "Verification failed"
                _authState.value = AuthState.ERROR
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verId
                _authState.value = AuthState.IDLE
                onCodeSent()
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(if (number.startsWith("+")) number else "+91$number") // Assuming India for default, but in real app handle properly
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String, number: String) {
        _authState.value = AuthState.LOADING
        val currentVerificationId = verificationId
        if (currentVerificationId == null) {
            _errorMessage.value = "Verification ID is missing. Please request code again."
            _authState.value = AuthState.ERROR
            return
        }
        val credential = PhoneAuthProvider.getCredential(currentVerificationId, code)
        signInWithPhoneAuthCredential(credential, number)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, number: String) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                val user = auth.currentUser
                if (user != null) {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    val name = doc.getString("name") ?: ""
                    
                    preferencesRepository.updateAuthData(
                        loggedIn = true, 
                        type = "global", 
                        firstName = name, 
                        phone = number
                    )
                    preferencesRepository.saveOnboardingData(name, number)
                    _authState.value = AuthState.SUCCESS
                } else {
                    throw Exception("User not found")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Login failed"
                _authState.value = AuthState.ERROR
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.IDLE
        _errorMessage.value = null
    }
}
