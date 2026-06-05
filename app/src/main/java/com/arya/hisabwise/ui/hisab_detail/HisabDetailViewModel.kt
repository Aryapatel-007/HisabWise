package com.arya.hisabwise.ui.hisab_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arya.hisabwise.data.local.ExpenseEntity
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.local.MemberEntity
import com.arya.hisabwise.data.local.SettlementEntity
import com.arya.hisabwise.data.repository.HisabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.min

data class MemberBalance(
    val memberId: Int,
    val name: String,
    val netBalance: Double
)

data class ProposedSettlement(
    val debtorId: Int,
    val debtorName: String,
    val creditorId: Int,
    val creditorName: String,
    val amount: Double,
    val isCompleted: Boolean,
    val settlementId: Int? // if it exists in DB
)

data class HisabDetailState(
    val hisab: HisabEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val totalSpent: Double = 0.0,
    val memberBalances: List<MemberBalance> = emptyList(),
    val finalSettlements: List<ProposedSettlement> = emptyList(),
    val totalEntries: Int = 0
)

@HiltViewModel
class HisabDetailViewModel @Inject constructor(
    private val repository: HisabRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hisabId: Int = checkNotNull(savedStateHandle["hisabId"])

    private val _state = MutableStateFlow(HisabDetailState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getHisabById(hisabId),
                repository.getMembersForHisab(hisabId),
                repository.getExpensesForHisab(hisabId),
                repository.getSettlementsForHisab(hisabId)
            ) { hisab, members, expenses, dbSettlements ->
                if (hisab == null) return@combine HisabDetailState()

                val totalSpent = expenses.sumOf { it.amount }

                // 1. Calculate Net Balances
                // For now, assuming EQUAL split among all members
                val memberMap = members.associateBy { it.id }
                
                // Map of MemberId to NetBalance
                val balances = mutableMapOf<Int, Double>()
                members.forEach { balances[it.id] = 0.0 }

                expenses.forEach { expense ->
                    // The person who paid gets positive balance added
                    balances[expense.payerId] = (balances[expense.payerId] ?: 0.0) + expense.amount
                    
                    if (expense.receiverId != null) {
                        // Internal transfer: receiver owes the full amount
                        balances[expense.receiverId] = (balances[expense.receiverId] ?: 0.0) - expense.amount
                    } else {
                        // Group expense: split equally among members in splitAmongIds
                        val splitIds = expense.splitAmongIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                        val actualSplitIds = if (splitIds.isEmpty()) members.map { it.id } else splitIds
                        
                        val splitAmount = if (actualSplitIds.isNotEmpty()) expense.amount / actualSplitIds.size else 0.0
                        actualSplitIds.forEach { memberId ->
                            balances[memberId] = (balances[memberId] ?: 0.0) - splitAmount
                        }
                    }
                }

                val rawBalancesMap = mutableMapOf<Int, Double>()
                members.forEach { rawBalancesMap[it.id] = 0.0 }
                
                expenses.forEach { expense ->
                    rawBalancesMap[expense.payerId] = (rawBalancesMap[expense.payerId] ?: 0.0) + expense.amount
                    if (expense.receiverId != null) {
                        rawBalancesMap[expense.receiverId] = (rawBalancesMap[expense.receiverId] ?: 0.0) - expense.amount
                    }
                }

                val memberBalances = rawBalancesMap.mapNotNull { (id, balance) ->
                    memberMap[id]?.let { member ->
                        MemberBalance(id, member.name, balance)
                    }
                }

                // 2. Minimum Transactions (Debt Simplification)
                // Creditors (positive balance) vs Debtors (negative balance)
                val creditors = balances.filter { it.value > 0.01 }.map { it.key to it.value }.toMutableList()
                val debtors = balances.filter { it.value < -0.01 }.map { it.key to it.value }.toMutableList()

                // Sort by amount descending to greedily match largest debts
                creditors.sortByDescending { it.second }
                debtors.sortBy { it.second } // Most negative first (lowest value)

                val proposedSettlements = mutableListOf<ProposedSettlement>()

                var i = 0 // Creditors index
                var j = 0 // Debtors index

                while (i < creditors.size && j < debtors.size) {
                    val creditor = creditors[i]
                    val debtor = debtors[j]

                    val creditAmount = creditor.second
                    val debtAmount = debtor.second.absoluteValue

                    val settleAmount = min(creditAmount, debtAmount)

                    if (settleAmount > 0.01) { // Floating point precision check
                        // Check if this settlement exists in DB
                        val dbMatch = dbSettlements.find { 
                            it.debtorId == debtor.first && 
                            it.creditorId == creditor.first && 
                            kotlin.math.abs(it.amount - settleAmount) < 0.01 
                        }

                        proposedSettlements.add(
                            ProposedSettlement(
                                debtorId = debtor.first,
                                debtorName = memberMap[debtor.first]?.name ?: "Unknown",
                                creditorId = creditor.first,
                                creditorName = memberMap[creditor.first]?.name ?: "Unknown",
                                amount = settleAmount,
                                isCompleted = dbMatch?.isCompleted ?: false,
                                settlementId = dbMatch?.id
                            )
                        )
                    }

                    creditors[i] = creditor.first to (creditAmount - settleAmount)
                    debtors[j] = debtor.first to (debtor.second + settleAmount)

                    if (creditors[i].second < 0.01) i++
                    if (debtors[j].second > -0.01) j++
                }

                HisabDetailState(
                    hisab = hisab,
                    members = members,
                    totalSpent = totalSpent,
                    memberBalances = memberBalances,
                    finalSettlements = proposedSettlements,
                    totalEntries = expenses.size
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun toggleSettlement(settlement: ProposedSettlement) {
        viewModelScope.launch {
            if (settlement.settlementId != null) {
                // Update existing
                repository.updateSettlementStatus(settlement.settlementId, !settlement.isCompleted)
            } else {
                // Create new
                val newSettlement = SettlementEntity(
                    hisabId = hisabId,
                    debtorId = settlement.debtorId,
                    creditorId = settlement.creditorId,
                    amount = settlement.amount,
                    isCompleted = true // Toggled from false to true
                )
                repository.insertSettlement(newSettlement)
            }
        }
    }
}
