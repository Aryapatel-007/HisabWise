package com.arya.hisabwise.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hisabs")
data class HisabEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val netAmount: Double,
    val budgetPerPerson: Double?,
    val createdAt: Long,
    val syncState: String = "local_only"
)
