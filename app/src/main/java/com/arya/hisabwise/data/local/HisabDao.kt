package com.arya.hisabwise.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHisab(hisab: HisabEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @androidx.room.Transaction
    suspend fun insertHisabWithMembers(hisab: HisabEntity, members: List<MemberEntity>): Long {
        val hisabId = insertHisab(hisab)
        val membersWithHisabId = members.map { it.copy(hisabId = hisabId.toInt()) }
        insertMembers(membersWithHisabId)
        return hisabId
    }

    @Delete
    suspend fun deleteHisab(hisab: HisabEntity)

    @Query("UPDATE hisabs SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Int, newName: String)

    @Query("SELECT h.id, h.name, COALESCE(SUM(e.amount), 0.0) as netAmount, h.budgetPerPerson, h.createdAt, h.syncState FROM hisabs h LEFT JOIN expenses e ON h.id = e.hisabId GROUP BY h.id ORDER BY h.createdAt DESC")
    fun getAllHisabs(): Flow<List<HisabEntity>>

    @Query("SELECT h.id, h.name, COALESCE(SUM(e.amount), 0.0) as netAmount, h.budgetPerPerson, h.createdAt, h.syncState FROM hisabs h LEFT JOIN expenses e ON h.id = e.hisabId WHERE h.syncState IN (:syncStates) GROUP BY h.id ORDER BY h.createdAt DESC")
    fun getHisabsBySyncStates(syncStates: List<String>): Flow<List<HisabEntity>>

    @Query("SELECT * FROM members WHERE hisabId = :hisabId")
    fun getMembersForHisab(hisabId: Int): Flow<List<MemberEntity>>

    @Query("SELECT * FROM hisabs WHERE id = :hisabId LIMIT 1")
    fun getHisabById(hisabId: Int): Flow<HisabEntity?>

    @Query("SELECT * FROM expenses WHERE hisabId = :hisabId ORDER BY timestamp DESC")
    fun getExpensesForHisab(hisabId: Int): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM settlements WHERE hisabId = :hisabId")
    fun getSettlementsForHisab(hisabId: Int): Flow<List<SettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    @Query("UPDATE settlements SET isCompleted = :isCompleted WHERE id = :settlementId")
    suspend fun updateSettlementStatus(settlementId: Int, isCompleted: Boolean)

    @Query("SELECT * FROM settlements WHERE id = :settlementId LIMIT 1")
    suspend fun getSettlementById(settlementId: Int): SettlementEntity?

    @Query("DELETE FROM settlements WHERE hisabId = :hisabId")
    suspend fun clearSettlementsForHisab(hisabId: Int)

    @androidx.room.Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @androidx.room.Update
    suspend fun updateHisab(hisab: HisabEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("SELECT * FROM members WHERE hisabId = :hisabId")
    suspend fun getMembersForHisabList(hisabId: Int): List<MemberEntity>

    @Query("SELECT * FROM expenses WHERE hisabId = :hisabId")
    suspend fun getExpensesForHisabList(hisabId: Int): List<ExpenseEntity>

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Int)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Int)

    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlementById(settlementId: Int)
}
