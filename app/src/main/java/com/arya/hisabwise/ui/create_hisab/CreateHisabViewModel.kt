package com.arya.hisabwise.ui.create_hisab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.local.MemberEntity
import com.arya.hisabwise.data.repository.HisabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore
import com.arya.hisabwise.data.UserPreferencesRepository
import javax.inject.Inject

data class CreateHisabState(
    val hisabName: String = "",
    val budgetPerPerson: String = "",
    val members: List<MemberEntity> = emptyList(),
    val newMemberName: String = "",
    val newMemberPhone: String = "",
    val isVerifyingMember: Boolean = false,
    val memberVerificationError: String? = null,
    val saveSuccess: Boolean = false,
    val editHisabId: Int? = null,
    val existingMembers: List<MemberEntity> = emptyList() // To track who was removed
)

@HiltViewModel
class CreateHisabViewModel @Inject constructor(
    private val repository: HisabRepository,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferencesRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    init {
        viewModelScope.launch {
            val hisabId = savedStateHandle.get<String>("hisabId")?.toIntOrNull()
            if (hisabId != null) {
                onEvent(CreateHisabEvent.LoadHisab(hisabId))
            } else {
                // Pre-populate with the current user
                val userName = userPreferences.userName.first()
                val userPhone = userPreferences.userPhone.first()
                if (userName.isNotBlank()) {
                    val selfMember = MemberEntity(
                        hisabId = 0,
                        name = userName,
                        phoneNumber = userPhone
                    )
                    _state.value = _state.value.copy(members = listOf(selfMember))
                }
            }
        }
    }

    private val _state = MutableStateFlow(CreateHisabState())
    val state: StateFlow<CreateHisabState> = _state.asStateFlow()

    fun onEvent(event: CreateHisabEvent) {
        when (event) {
            is CreateHisabEvent.HisabNameChanged -> {
                _state.value = _state.value.copy(hisabName = event.name)
            }
            is CreateHisabEvent.BudgetChanged -> {
                _state.value = _state.value.copy(budgetPerPerson = event.budget)
            }
            is CreateHisabEvent.NewMemberNameChanged -> {
                _state.value = _state.value.copy(newMemberName = event.name)
            }
            is CreateHisabEvent.NewMemberPhoneChanged -> {
                _state.value = _state.value.copy(newMemberPhone = event.phone)
            }
            is CreateHisabEvent.AddMember -> {
                val currentState = _state.value
                if (currentState.newMemberName.isNotBlank() && currentState.newMemberPhone.isNotBlank()) {
                    viewModelScope.launch {
                        _state.value = currentState.copy(isVerifyingMember = true, memberVerificationError = null)
                        
                        val authType = userPreferences.authType.first()
                        if (authType == "local") {
                            addMemberLocally(currentState)
                        } else {
                            verifyAndAddMemberGlobally(currentState)
                        }
                    }
                }
            }
            is CreateHisabEvent.ClearVerificationError -> {
                _state.value = _state.value.copy(memberVerificationError = null)
            }
            is CreateHisabEvent.RemoveMember -> {
                _state.value = _state.value.copy(
                    members = _state.value.members - event.member
                )
            }
            is CreateHisabEvent.SaveHisab -> {
                saveHisab()
            }
            is CreateHisabEvent.LoadHisab -> {
                loadHisab(event.hisabId)
            }
        }
    }

    private fun loadHisab(hisabId: Int) {
        viewModelScope.launch {
            val hisab = repository.getHisabById(hisabId).firstOrNull()
            val members = repository.getMembersForHisab(hisabId).firstOrNull() ?: emptyList()
            if (hisab != null) {
                _state.value = _state.value.copy(
                    hisabName = hisab.name,
                    budgetPerPerson = hisab.budgetPerPerson?.toString() ?: "",
                    members = members,
                    existingMembers = members,
                    editHisabId = hisabId
                )
            }
        }
    }

    private fun saveHisab() {
        val currentState = _state.value
        if (currentState.hisabName.isBlank()) return
        
        viewModelScope.launch {
            val budget = currentState.budgetPerPerson.toDoubleOrNull()
            if (currentState.editHisabId != null) {
                // UPDATE EXISTING HISAB
                val hisabId = currentState.editHisabId
                val hisab = repository.getHisabById(hisabId).firstOrNull()
                if (hisab != null) {
                    val updatedHisab = hisab.copy(
                        name = currentState.hisabName.trim(),
                        budgetPerPerson = budget
                    )

                    // Find removed members
                    val currentMemberIds = currentState.members.map { it.id }.toSet()
                    val removedMembers = currentState.existingMembers.filter { it.id !in currentMemberIds }
                    
                    // Retroactively remove them from all entries
                    if (removedMembers.isNotEmpty()) {
                        val expenses = repository.getExpensesForHisabList(hisabId)
                        val removedIds = removedMembers.map { it.id }.toSet()
                        
                        expenses.forEach { expense ->
                            var updated = false
                            
                            // Remove from splitAmongIds string
                            val splitIds = expense.splitAmongIds.split(",")
                                .filter { it.isNotBlank() }
                                .mapNotNull { it.toIntOrNull() }
                                .toMutableList()
                                
                            val origSplitSize = splitIds.size
                            splitIds.removeAll(removedIds)
                            if (origSplitSize != splitIds.size) updated = true
                            
                            // Remove from includedMemberIds list
                            val includedList = expense.includedMemberIds.toMutableList()
                            val origIncSize = includedList.size
                            includedList.removeAll(removedIds)
                            if (origIncSize != includedList.size) updated = true
                            
                            if (updated) {
                                repository.updateExpense(
                                    expense.copy(
                                        splitAmongIds = splitIds.joinToString(","),
                                        includedMemberIds = includedList
                                    )
                                )
                            }
                        }
                        
                        // Delete members from DB
                        removedMembers.forEach { member ->
                            repository.deleteMember(member)
                        }
                    }

                    // Insert newly added members
                    val newlyAdded = currentState.members.filter { it.id == 0 }
                    if (newlyAdded.isNotEmpty()) {
                        // Insert members with correct hisabId
                        repository.insertMembers(newlyAdded.map { it.copy(hisabId = hisabId) })
                    }

                    // Update Hisab AT THE END so that participantPhones includes the newly added members
                    repository.updateHisab(updatedHisab)
                }
            } else {
                // CREATE NEW HISAB
                val hisab = HisabEntity(
                    name = currentState.hisabName.trim(),
                    netAmount = 0.0,
                    budgetPerPerson = budget,
                    createdAt = System.currentTimeMillis()
                )
                repository.insertHisabWithMembers(hisab, currentState.members)
            }
            _state.value = _state.value.copy(saveSuccess = true)
        }
    }

    private fun addMemberLocally(currentState: CreateHisabState) {
        val phone = currentState.newMemberPhone.trim()
        val name = currentState.newMemberName.trim()
        
        if ((phone.isNotBlank() && currentState.members.any { it.phoneNumber == phone }) ||
            currentState.members.any { it.name.equals(name, ignoreCase = true) }
        ) {
            _state.value = _state.value.copy(
                isVerifyingMember = false,
                memberVerificationError = "Member with this name or phone number already exists."
            )
            return
        }
        
        val newMember = MemberEntity(
            hisabId = 0,
            name = name,
            phoneNumber = phone
        )
        _state.value = currentState.copy(
            members = currentState.members + newMember,
            newMemberName = "",
            newMemberPhone = "",
            isVerifyingMember = false
        )
    }

    private suspend fun verifyAndAddMemberGlobally(currentState: CreateHisabState) {
        try {
            val phone = currentState.newMemberPhone.trim()
            val name = currentState.newMemberName.trim()
            
            if ((phone.isNotBlank() && currentState.members.any { it.phoneNumber == phone }) ||
                currentState.members.any { it.name.equals(name, ignoreCase = true) }
            ) {
                _state.value = _state.value.copy(
                    isVerifyingMember = false,
                    memberVerificationError = "Member with this name or phone number already exists."
                )
                return
            }
            
            // Assume the user may or may not add +91 or country code, for simplicity we look up exact match.
            // Ideally we should format it exactly as stored in DB.
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                _state.value = _state.value.copy(
                    isVerifyingMember = false,
                    memberVerificationError = "User is not registered on Hisabwise."
                )
                return
            } else {
                val doc = querySnapshot.documents.first()
                val fullName = doc.getString("name") ?: ""
                val firstName = fullName.split(" ").firstOrNull() ?: fullName
                
                val newMember = MemberEntity(
                    hisabId = 0,
                    name = firstName.trim(),
                    phoneNumber = phone
                )
                
                _state.value = currentState.copy(
                    members = currentState.members + newMember,
                    newMemberName = "",
                    newMemberPhone = "",
                    isVerifyingMember = false
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isVerifyingMember = false,
                memberVerificationError = "Verification failed: ${e.message}"
            )
        }
    }
}

sealed class CreateHisabEvent {
    data class HisabNameChanged(val name: String) : CreateHisabEvent()
    data class BudgetChanged(val budget: String) : CreateHisabEvent()
    data class NewMemberNameChanged(val name: String) : CreateHisabEvent()
    data class NewMemberPhoneChanged(val phone: String) : CreateHisabEvent()
    object AddMember : CreateHisabEvent()
    object ClearVerificationError : CreateHisabEvent()
    data class RemoveMember(val member: MemberEntity) : CreateHisabEvent()
    object SaveHisab : CreateHisabEvent()
    data class LoadHisab(val hisabId: Int) : CreateHisabEvent()
}
