package com.arya.hisabwise.data.repository

import com.arya.hisabwise.data.UserPreferencesRepository
import com.arya.hisabwise.data.local.HisabDao
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.local.MemberEntity
import com.arya.hisabwise.data.local.ExpenseEntity
import com.arya.hisabwise.data.local.SettlementEntity
import com.arya.hisabwise.data.remote.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class HisabRepository @Inject constructor(
    private val hisabDao: HisabDao,
    private val userPreferences: UserPreferencesRepository,
    private val firebaseService: FirebaseService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private suspend fun getSyncState(): String {
        val authType = userPreferences.authType.first()
        return if (authType == "global") "pending_sync" else "local_only"
    }

    fun getAllHisabs(): Flow<List<HisabEntity>> {
        return hisabDao.getAllHisabs()
    }

    fun getHisabsByMode(isGlobal: Boolean): Flow<List<HisabEntity>> {
        val states = if (isGlobal) listOf("pending_sync", "synced") else listOf("local_only")
        return hisabDao.getHisabsBySyncStates(states)
    }

    suspend fun insertHisab(hisab: HisabEntity) {
        val state = getSyncState()
        val updatedHisab = hisab.copy(syncState = state)
        val newId = hisabDao.insertHisab(updatedHisab)
        if (state == "pending_sync") {
            val myPhone = userPreferences.userPhone.first()
            scope.launch {
                try {
                    val hisabWithId = updatedHisab.copy(id = newId.toInt())
                    firebaseService.syncHisabToFirestore(hisabWithId, listOf(myPhone))
                    hisabDao.updateHisab(hisabWithId.copy(syncState = "synced"))
                } catch (e: Exception) {
                    // Handle sync failure
                }
            }
        }
    }

    suspend fun insertHisabWithMembers(hisab: HisabEntity, members: List<MemberEntity>) {
        val state = getSyncState()
        val updatedHisab = hisab.copy(syncState = state)
        val updatedMembers = members.map { it.copy(syncState = state) }
        
        val newHisabId = hisabDao.insertHisabWithMembers(updatedHisab, updatedMembers)
        
        if (state == "pending_sync") {
            val myPhone = userPreferences.userPhone.first()
            val participantPhones = updatedMembers.map { it.phoneNumber } + myPhone
            scope.launch {
                try {
                    val hisabWithId = updatedHisab.copy(id = newHisabId.toInt())
                    firebaseService.syncHisabToFirestore(hisabWithId, participantPhones.distinct())
                    hisabDao.insertHisab(hisabWithId.copy(syncState = "synced"))
                    
                    // Fetch newly inserted members from DB to get their generated IDs
                    val savedMembers = hisabDao.getMembersForHisabList(newHisabId.toInt())
                    savedMembers.forEach { member ->
                        firebaseService.syncMemberToFirestore(member.copy(syncState = "synced"))
                        hisabDao.insertMembers(listOf(member.copy(syncState = "synced")))
                    }
                } catch (e: Exception) {
                    // Handle sync failure
                }
            }
        }
    }

    fun getMembersForHisab(hisabId: Int): Flow<List<MemberEntity>> {
        return hisabDao.getMembersForHisab(hisabId)
    }

    suspend fun deleteHisab(hisab: HisabEntity) {
        hisabDao.deleteHisab(hisab)
        val state = getSyncState()
        if (state == "pending_sync" || state == "synced") {
            scope.launch {
                try {
                    firebaseService.deleteHisabFromFirestore(hisab.id)
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun updateHisabName(id: Int, newName: String) {
        hisabDao.updateName(id, newName)
        // Note: Full sync logic would re-fetch the hisab and sync it. 
        // We'll keep it simple for now as requested by foundational changes.
    }

    fun getHisabById(hisabId: Int): Flow<HisabEntity?> {
        return hisabDao.getHisabById(hisabId)
    }

    fun getExpensesForHisab(hisabId: Int): Flow<List<ExpenseEntity>> {
        return hisabDao.getExpensesForHisab(hisabId)
    }

    suspend fun insertExpense(expense: ExpenseEntity) {
        val state = getSyncState()
        val updatedExpense = expense.copy(syncState = state)
        val newId = hisabDao.insertExpense(updatedExpense)
        
        if (state == "pending_sync") {
            scope.launch {
                try {
                    val expenseWithId = updatedExpense.copy(id = newId.toInt())
                    firebaseService.syncExpenseToFirestore(expenseWithId)
                    hisabDao.insertExpense(expenseWithId.copy(syncState = "synced"))
                } catch (e: Exception) {
                    // Sync failed
                }
            }
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        hisabDao.deleteExpense(expense)
        val state = getSyncState()
        if (state == "pending_sync") {
            scope.launch {
                try {
                    firebaseService.deleteExpenseFromFirestore(expense.hisabId, expense.id)
                } catch (e: Exception) {
                }
            }
        }
    }

    fun getSettlementsForHisab(hisabId: Int): Flow<List<SettlementEntity>> {
        return hisabDao.getSettlementsForHisab(hisabId)
    }

    suspend fun insertSettlement(settlement: SettlementEntity) {
        val state = getSyncState()
        val updatedSettlement = settlement.copy(syncState = state)
        val newId = hisabDao.insertSettlement(updatedSettlement)
        
        if (state == "pending_sync") {
            scope.launch {
                try {
                    val settlementWithId = updatedSettlement.copy(id = newId.toInt())
                    firebaseService.syncSettlementToFirestore(settlementWithId)
                    hisabDao.insertSettlement(settlementWithId.copy(syncState = "synced"))
                } catch (e: Exception) {
                    // Sync failed
                }
            }
        }
    }

    suspend fun insertSettlements(settlements: List<SettlementEntity>) {
        val state = getSyncState()
        val updatedSettlements = settlements.map { it.copy(syncState = state) }
        hisabDao.insertSettlements(updatedSettlements)
        
        if (state == "pending_sync") {
            scope.launch {
                try {
                    updatedSettlements.forEach { settlement ->
                        firebaseService.syncSettlementToFirestore(settlement)
                    }
                } catch (e: Exception) {
                    // Sync failed
                }
            }
        }
    }

    suspend fun updateSettlementStatus(settlementId: Int, isCompleted: Boolean) {
        hisabDao.updateSettlementStatus(settlementId, isCompleted)
        val state = getSyncState()
        if (state == "pending_sync") {
            scope.launch {
                try {
                    val settlement = hisabDao.getSettlementById(settlementId)
                    if (settlement != null) {
                        val updatedSettlement = settlement.copy(syncState = "synced")
                        firebaseService.syncSettlementToFirestore(updatedSettlement)
                        // Note: we're ignoring updating the local syncState for simplicity,
                        // but it will be synced and the listener will pull it back.
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun clearSettlementsForHisab(hisabId: Int) {
        hisabDao.clearSettlementsForHisab(hisabId)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        val state = getSyncState()
        val updatedExpense = expense.copy(syncState = state)
        hisabDao.updateExpense(updatedExpense)
        if (state == "pending_sync") {
            scope.launch {
                try {
                    firebaseService.syncExpenseToFirestore(updatedExpense)
                    hisabDao.updateExpense(updatedExpense.copy(syncState = "synced"))
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun updateHisab(hisab: HisabEntity) {
        val state = getSyncState()
        val updatedHisab = hisab.copy(syncState = state)
        hisabDao.updateHisab(updatedHisab)
        if (state == "pending_sync") {
            val members = hisabDao.getMembersForHisabList(hisab.id)
            val myPhone = userPreferences.userPhone.first()
            val participantPhones = members.map { it.phoneNumber } + myPhone
            scope.launch {
                try {
                    firebaseService.syncHisabToFirestore(updatedHisab, participantPhones.distinct())
                    hisabDao.updateHisab(updatedHisab.copy(syncState = "synced"))
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun deleteMember(member: MemberEntity) {
        hisabDao.deleteMember(member)
        val state = getSyncState()
        if (state == "pending_sync") {
            scope.launch {
                try {
                    firebaseService.deleteMemberFromFirestore(member.hisabId, member.id)
                    
                    val hisab = hisabDao.getHisabById(member.hisabId).first()
                    if (hisab != null) {
                        val members = hisabDao.getMembersForHisabList(hisab.id)
                        val myPhone = userPreferences.userPhone.first()
                        val participantPhones = members.map { it.phoneNumber } + myPhone
                        firebaseService.syncHisabToFirestore(hisab, participantPhones.distinct())
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun getMembersForHisabList(hisabId: Int): List<MemberEntity> {
        return hisabDao.getMembersForHisabList(hisabId)
    }

    suspend fun getExpensesForHisabList(hisabId: Int): List<ExpenseEntity> {
        return hisabDao.getExpensesForHisabList(hisabId)
    }

    suspend fun insertMembers(members: List<MemberEntity>) {
        if (members.isEmpty()) return
        val hisabId = members.first().hisabId
        val existingMembers = hisabDao.getMembersForHisabList(hisabId)
        
        val uniqueNames = mutableMapOf<String, Int>() // maps name to memberId
        val uniquePhones = mutableMapOf<String, Int>() // maps phone to memberId
        val membersToDelete = mutableListOf<MemberEntity>()

        for (member in existingMembers) {
            val nameKey = member.name.trim().lowercase()
            val phoneKey = member.phoneNumber.trim()
            
            val duplicateByName = uniqueNames[nameKey]
            val duplicateByPhone = if (phoneKey.isNotEmpty()) uniquePhones[phoneKey] else null
            
            if ((duplicateByName != null && duplicateByName != member.id) || 
                (duplicateByPhone != null && duplicateByPhone != member.id)) {
                membersToDelete.add(member)
            } else {
                uniqueNames[nameKey] = member.id
                if (phoneKey.isNotEmpty()) uniquePhones[phoneKey] = member.id
            }
        }

        if (membersToDelete.isNotEmpty()) {
            membersToDelete.forEach { dup ->
                hisabDao.deleteMember(dup)
                try {
                    firebaseService.deleteMemberFromFirestore(hisabId, dup.id)
                } catch (e: Exception) {}
            }
        }
        
        val membersToInsert = mutableListOf<MemberEntity>()
        for (member in members) {
            val nameKey = member.name.trim().lowercase()
            val phoneKey = member.phoneNumber.trim()
            
            val duplicateByName = uniqueNames[nameKey]
            val duplicateByPhone = if (phoneKey.isNotEmpty()) uniquePhones[phoneKey] else null
            
            if ((duplicateByName != null && duplicateByName != member.id) || 
                (duplicateByPhone != null && duplicateByPhone != member.id)) {
                // it is a duplicate of a DIFFERENT member, ignore
                continue
            } else {
                membersToInsert.add(member)
                uniqueNames[nameKey] = member.id
                if (phoneKey.isNotEmpty()) uniquePhones[phoneKey] = member.id
            }
        }
        
        if (membersToInsert.isEmpty()) return

        val state = getSyncState()
        val updatedMembers = membersToInsert.map { it.copy(syncState = state) }
        val newIds = hisabDao.insertMembers(updatedMembers)
        
        if (state == "pending_sync" || state == "synced") {
            scope.launch {
                try {
                    val savedMembers = updatedMembers.mapIndexed { index, member -> 
                        // If the member already had an ID > 0, insertMembers returns that ID or we can just use the existing one,
                        // but newIds[index] is always reliable for the primary key.
                        member.copy(id = newIds[index].toInt(), syncState = "synced")
                    }
                    savedMembers.forEach { member ->
                        firebaseService.syncMemberToFirestore(member)
                        hisabDao.insertMembers(listOf(member))
                    }
                    
                    if (savedMembers.isNotEmpty()) {
                        val hisab = hisabDao.getHisabById(hisabId).first()
                        if (hisab != null) {
                            val allMembers = hisabDao.getMembersForHisabList(hisabId)
                            val myPhone = userPreferences.userPhone.first()
                            val participantPhones = allMembers.map { it.phoneNumber } + myPhone
                            firebaseService.syncHisabToFirestore(hisab, participantPhones.distinct())
                        }
                    }
                } catch (e: Exception) {
                    // Handle sync failure
                }
            }
        }
    }

    private val activeHisabListeners = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private var isRealtimeSyncStarted = false

    fun startRealtimeSync() {
        if (isRealtimeSyncStarted) return
        isRealtimeSyncStarted = true
        
        scope.launch {
            val authMode = userPreferences.authType.first()
            if (authMode != "global") {
                isRealtimeSyncStarted = false
                return@launch
            }
            val myPhone = userPreferences.userPhone.first()
            if (myPhone.isBlank()) {
                isRealtimeSyncStarted = false
                return@launch
            }
            
            firebaseService.listenToMyHisabs(myPhone, { hisab ->
                scope.launch {
                    val existing = hisabDao.getHisabById(hisab.id).first()
                    if (existing == null) {
                        hisabDao.insertHisab(hisab)
                    } else {
                        hisabDao.updateHisab(hisab)
                    }
                    
                    if (!activeHisabListeners.contains(hisab.id)) {
                        activeHisabListeners.add(hisab.id)
                        
                        firebaseService.listenToHisabMembers(hisab.id, { member ->
                            scope.launch { 
                                val existingMembers = hisabDao.getMembersForHisabList(hisab.id)
                                val duplicate = existingMembers.find { 
                                    it.id != member.id && 
                                    (it.name.equals(member.name, ignoreCase = true) || 
                                    (member.phoneNumber.isNotBlank() && it.phoneNumber == member.phoneNumber))
                                }
                                if (duplicate != null) {
                                    try {
                                        firebaseService.deleteMemberFromFirestore(hisab.id, member.id)
                                    } catch (e: Exception) {}
                                } else {
                                    hisabDao.insertMembers(listOf(member)) 
                                }
                            }
                        }, { memberId ->
                            scope.launch { hisabDao.deleteMemberById(memberId) }
                        })
                        
                        firebaseService.listenToHisabExpenses(hisab.id, { expense ->
                            scope.launch {
                                val existExp = hisabDao.getExpensesForHisabList(hisab.id).find { it.id == expense.id }
                                if (existExp == null) {
                                    hisabDao.insertExpense(expense)
                                } else {
                                    hisabDao.updateExpense(expense)
                                }
                            }
                        }, { expId ->
                            scope.launch { hisabDao.deleteExpenseById(expId) }
                        })
                        
                        firebaseService.listenToHisabSettlements(hisab.id, { settlement ->
                            scope.launch {
                                val existSet = hisabDao.getSettlementsForHisab(hisab.id).first().find { it.id == settlement.id }
                                if (existSet == null) {
                                    hisabDao.insertSettlements(listOf(settlement))
                                }
                            }
                        }, { setId ->
                            scope.launch { hisabDao.deleteSettlementById(setId) }
                        })
                    }
                }
            }, { deletedHisabId ->
                scope.launch {
                    val hisab = hisabDao.getHisabById(deletedHisabId).first()
                    if (hisab != null) {
                        hisabDao.deleteHisab(hisab)
                    }
                }
            })
        }
    }
}
