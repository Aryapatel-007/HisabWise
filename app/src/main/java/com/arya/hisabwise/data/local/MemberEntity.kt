package com.arya.hisabwise.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = HisabEntity::class,
            parentColumns = ["id"],
            childColumns = ["hisabId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hisabId")]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hisabId: Int,
    val name: String,
    val phoneNumber: String,
    val syncState: String = "local_only"
)
