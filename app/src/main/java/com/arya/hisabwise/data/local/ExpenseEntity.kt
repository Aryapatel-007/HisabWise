package com.arya.hisabwise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = HisabEntity::class,
            parentColumns = ["id"],
            childColumns = ["hisabId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hisabId"), Index("payerId"), Index("receiverId")]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hisabId: Int,
    val payerId: Int,
    val receiverId: Int? = null,
    val remark: String,
    val description: String,
    val amount: Double,
    val timestamp: Long,
    val splitAmongIds: String, // Comma-separated list of member IDs, e.g., "1,2,3"
    val syncState: String = "local_only",
    val includedMemberIds: List<Int> = emptyList()
)
