package com.arya.hisabwise.data.remote

import com.arya.hisabwise.data.local.ExpenseEntity
import com.arya.hisabwise.data.local.HisabEntity
import com.arya.hisabwise.data.local.MemberEntity
import com.arya.hisabwise.data.local.SettlementEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface FirebaseService {
    suspend fun syncHisabToFirestore(hisab: HisabEntity, participantPhones: List<String>)
    suspend fun syncMemberToFirestore(member: MemberEntity)
    suspend fun syncExpenseToFirestore(expense: ExpenseEntity)
    suspend fun syncSettlementToFirestore(settlement: SettlementEntity)
    
    fun listenToMyHisabs(userPhone: String, onHisabUpdated: (HisabEntity) -> Unit, onHisabDeleted: (Int) -> Unit)
    fun listenToHisabMembers(hisabId: Int, onMemberUpdated: (MemberEntity) -> Unit, onMemberDeleted: (Int) -> Unit)
    fun listenToHisabExpenses(hisabId: Int, onExpenseUpdated: (ExpenseEntity) -> Unit, onExpenseDeleted: (Int) -> Unit)
    fun listenToHisabSettlements(hisabId: Int, onSettlementUpdated: (SettlementEntity) -> Unit, onSettlementDeleted: (Int) -> Unit)
    suspend fun deleteMemberFromFirestore(hisabId: Int, memberId: Int)
    suspend fun deleteExpenseFromFirestore(hisabId: Int, expenseId: Int)
    suspend fun deleteHisabFromFirestore(hisabId: Int)
    suspend fun isUserRegistered(phone: String): Boolean
}

class FirebaseServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FirebaseService {

    override suspend fun syncHisabToFirestore(hisab: HisabEntity, participantPhones: List<String>) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(
            "hisab" to hisab,
            "participantPhones" to participantPhones
        )
        firestore.collection("hisabs")
            .document(hisab.id.toString())
            .set(data)
            .await()
    }

    override suspend fun syncMemberToFirestore(member: MemberEntity) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(member.hisabId.toString())
            .collection("members")
            .document(member.id.toString())
            .set(member)
            .await()
    }

    override suspend fun deleteMemberFromFirestore(hisabId: Int, memberId: Int) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(hisabId.toString())
            .collection("members")
            .document(memberId.toString())
            .delete()
            .await()
    }

    override suspend fun deleteExpenseFromFirestore(hisabId: Int, expenseId: Int) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(hisabId.toString())
            .collection("expenses")
            .document(expenseId.toString())
            .delete()
            .await()
    }

    override suspend fun deleteHisabFromFirestore(hisabId: Int) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(hisabId.toString())
            .delete()
            .await()
    }

    override suspend fun syncExpenseToFirestore(expense: ExpenseEntity) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(expense.hisabId.toString())
            .collection("expenses")
            .document(expense.id.toString())
            .set(expense)
            .await()
    }

    override suspend fun syncSettlementToFirestore(settlement: SettlementEntity) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .document(settlement.hisabId.toString())
            .collection("settlements")
            .document(settlement.id.toString())
            .set(settlement)
            .await()
    }

    override fun listenToMyHisabs(userPhone: String, onHisabUpdated: (HisabEntity) -> Unit, onHisabDeleted: (Int) -> Unit) {
        val user = auth.currentUser ?: return
        firestore.collection("hisabs")
            .whereArrayContains("participantPhones", userPhone)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    for (change in snapshot.documentChanges) {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val doc = change.document
                                try {
                                    val hisabData = doc.get("hisab") as? Map<String, Any>
                                    if (hisabData != null) {
                                        val hisab = HisabEntity(
                                            id = (hisabData["id"] as? Number)?.toInt() ?: 0,
                                            name = hisabData["name"] as? String ?: "",
                                            netAmount = (hisabData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                                            budgetPerPerson = (hisabData["budgetPerPerson"] as? Number)?.toDouble(),
                                            createdAt = (hisabData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                            syncState = "synced"
                                        )
                                        onHisabUpdated(hisab)
                                    }
                                } catch (e: Exception) {
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = change.document.id.toIntOrNull()
                                if (id != null) onHisabDeleted(id)
                            }
                        }
                    }
                }
            }
    }

    override fun listenToHisabMembers(hisabId: Int, onMemberUpdated: (MemberEntity) -> Unit, onMemberDeleted: (Int) -> Unit) {
        firestore.collection("hisabs").document(hisabId.toString()).collection("members")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            try {
                                val doc = change.document
                                val member = MemberEntity(
                                    id = (doc["id"] as? Number)?.toInt() ?: 0,
                                    hisabId = hisabId,
                                    name = doc["name"] as? String ?: "",
                                    phoneNumber = doc["phoneNumber"] as? String ?: "",
                                    syncState = "synced"
                                )
                                onMemberUpdated(member)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val id = change.document.id.toIntOrNull()
                            if (id != null) onMemberDeleted(id)
                        }
                    }
                }
            }
    }

    override fun listenToHisabExpenses(hisabId: Int, onExpenseUpdated: (ExpenseEntity) -> Unit, onExpenseDeleted: (Int) -> Unit) {
        firestore.collection("hisabs").document(hisabId.toString()).collection("expenses")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            try {
                                val doc = change.document
                                val rawList = doc["includedMemberIds"] as? List<*>
                                val includedIds = rawList?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

                                val exp = ExpenseEntity(
                                    id = (doc["id"] as? Number)?.toInt() ?: 0,
                                    hisabId = hisabId,
                                    payerId = (doc["payerId"] as? Number)?.toInt() ?: 0,
                                    receiverId = (doc["receiverId"] as? Number)?.toInt(),
                                    remark = doc["remark"] as? String ?: "",
                                    description = doc["description"] as? String ?: "",
                                    amount = (doc["amount"] as? Number)?.toDouble() ?: 0.0,
                                    timestamp = (doc["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    splitAmongIds = doc["splitAmongIds"] as? String ?: "",
                                    includedMemberIds = includedIds,
                                    syncState = "synced"
                                )
                                onExpenseUpdated(exp)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val id = change.document.id.toIntOrNull()
                            if (id != null) onExpenseDeleted(id)
                        }
                    }
                }
            }
    }

    override fun listenToHisabSettlements(hisabId: Int, onSettlementUpdated: (SettlementEntity) -> Unit, onSettlementDeleted: (Int) -> Unit) {
        firestore.collection("hisabs").document(hisabId.toString()).collection("settlements")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            try {
                                val doc = change.document
                                val set = SettlementEntity(
                                    id = (doc["id"] as? Number)?.toInt() ?: 0,
                                    hisabId = hisabId,
                                    debtorId = (doc["debtorId"] as? Number)?.toInt() ?: 0,
                                    creditorId = (doc["creditorId"] as? Number)?.toInt() ?: 0,
                                    amount = (doc["amount"] as? Number)?.toDouble() ?: 0.0,
                                    isCompleted = doc["isCompleted"] as? Boolean ?: false,
                                    syncState = "synced"
                                )
                                onSettlementUpdated(set)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val id = change.document.id.toIntOrNull()
                            if (id != null) onSettlementDeleted(id)
                        }
                    }
                }
            }
    }

    override suspend fun isUserRegistered(phone: String): Boolean {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}
