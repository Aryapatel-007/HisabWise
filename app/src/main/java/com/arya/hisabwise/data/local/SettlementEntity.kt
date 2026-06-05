package com.arya.hisabwise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlements",
    foreignKeys = [
        ForeignKey(
            entity = HisabEntity::class,
            parentColumns = ["id"],
            childColumns = ["hisabId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hisabId"), Index("debtorId"), Index("creditorId")]
)
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hisabId: Int,
    val debtorId: Int, // The one who owes money
    val creditorId: Int, // The one who receives money
    val amount: Double,
    val isCompleted: Boolean = false,
    val syncState: String = "local_only"
)
