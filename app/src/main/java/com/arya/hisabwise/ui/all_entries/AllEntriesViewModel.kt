package com.arya.hisabwise.ui.all_entries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.local.ExpenseEntity
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.local.MemberEntity
import com.arya.hisabwise.data.repository.HisabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AllEntriesState(
    val hisab: HisabEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val allExpenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedExpense: ExpenseEntity? = null, // null for create, not-null for edit
    val isDialogOpen: Boolean = false
)

sealed class AllEntriesEvent {
    data class SearchQueryChanged(val query: String) : AllEntriesEvent()
    object OpenDialogForCreate : AllEntriesEvent()
    data class OpenDialogForEdit(val expense: ExpenseEntity) : AllEntriesEvent()
    object CloseDialog : AllEntriesEvent()
    data class SaveExpense(
        val expenseId: Int = 0,
        val payerId: Int,
        val receiverId: Int?,
        val remark: String,
        val description: String,
        val amount: Double,
        val splitAmongIds: String
    ) : AllEntriesEvent()
    data class DeleteExpense(val expense: ExpenseEntity) : AllEntriesEvent()
}

@HiltViewModel
class AllEntriesViewModel @Inject constructor(
    private val repository: HisabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hisabId: Int = checkNotNull(savedStateHandle["hisabId"])

    private val _state = MutableStateFlow(AllEntriesState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getHisabById(hisabId),
                repository.getMembersForHisab(hisabId),
                repository.getExpensesForHisab(hisabId),
                _state.map { it.searchQuery }.distinctUntilChanged()
            ) { hisab, members, expenses, query ->
                
                val memberMap = members.associateBy { it.id }
                
                val filtered = if (query.isBlank()) {
                    expenses
                } else {
                    expenses.filter { expense ->
                        val payerName = memberMap[expense.payerId]?.name ?: ""
                        val receiverName = expense.receiverId?.let { memberMap[it]?.name } ?: ""
                        
                        expense.remark.contains(query, ignoreCase = true) ||
                        expense.description.contains(query, ignoreCase = true) ||
                        payerName.contains(query, ignoreCase = true) ||
                        receiverName.contains(query, ignoreCase = true) ||
                        expense.amount.toString().contains(query)
                    }
                }

                _state.value.copy(
                    hisab = hisab,
                    members = members,
                    allExpenses = expenses,
                    filteredExpenses = filtered
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun onEvent(event: AllEntriesEvent) {
        when (event) {
            is AllEntriesEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            AllEntriesEvent.OpenDialogForCreate -> {
                _state.value = _state.value.copy(
                    selectedExpense = null,
                    isDialogOpen = true
                )
            }
            is AllEntriesEvent.OpenDialogForEdit -> {
                _state.value = _state.value.copy(
                    selectedExpense = event.expense,
                    isDialogOpen = true
                )
            }
            AllEntriesEvent.CloseDialog -> {
                _state.value = _state.value.copy(isDialogOpen = false)
            }
            is AllEntriesEvent.SaveExpense -> {
                viewModelScope.launch {
                    val expense = ExpenseEntity(
                        id = event.expenseId, // 0 for new, existing id for update
                        hisabId = hisabId,
                        payerId = event.payerId,
                        receiverId = event.receiverId,
                        remark = event.remark,
                        description = event.description,
                        amount = event.amount,
                        timestamp = if (event.expenseId == 0) System.currentTimeMillis() else _state.value.selectedExpense?.timestamp ?: System.currentTimeMillis(),
                        splitAmongIds = event.splitAmongIds
                    )
                    repository.insertExpense(expense)
                    _state.value = _state.value.copy(isDialogOpen = false)
                }
            }
            is AllEntriesEvent.DeleteExpense -> {
                viewModelScope.launch {
                    repository.deleteExpense(event.expense)
                    _state.value = _state.value.copy(isDialogOpen = false)
                }
            }
        }
    }
}
