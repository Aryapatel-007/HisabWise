package com.arya.hisabwise.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HisabEntity::class, 
        MemberEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class
    ], 
    version = 6, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hisabDao(): HisabDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN includedMemberIds TEXT NOT NULL DEFAULT '[]'")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove Member foreign keys from expenses
                database.execSQL("""
                    CREATE TABLE expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hisabId INTEGER NOT NULL,
                        payerId INTEGER NOT NULL,
                        receiverId INTEGER,
                        remark TEXT NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        splitAmongIds TEXT NOT NULL,
                        syncState TEXT NOT NULL,
                        includedMemberIds TEXT NOT NULL,
                        FOREIGN KEY(hisabId) REFERENCES hisabs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("INSERT INTO expenses_new SELECT * FROM expenses")
                database.execSQL("DROP TABLE expenses")
                database.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
                database.execSQL("CREATE INDEX index_expenses_hisabId ON expenses(hisabId)")
                database.execSQL("CREATE INDEX index_expenses_payerId ON expenses(payerId)")
                database.execSQL("CREATE INDEX index_expenses_receiverId ON expenses(receiverId)")

                // Remove Member foreign keys from settlements
                database.execSQL("""
                    CREATE TABLE settlements_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        hisabId INTEGER NOT NULL,
                        debtorId INTEGER NOT NULL,
                        creditorId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        syncState TEXT NOT NULL,
                        FOREIGN KEY(hisabId) REFERENCES hisabs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("INSERT INTO settlements_new SELECT * FROM settlements")
                database.execSQL("DROP TABLE settlements")
                database.execSQL("ALTER TABLE settlements_new RENAME TO settlements")
                database.execSQL("CREATE INDEX index_settlements_hisabId ON settlements(hisabId)")
                database.execSQL("CREATE INDEX index_settlements_debtorId ON settlements(debtorId)")
                database.execSQL("CREATE INDEX index_settlements_creditorId ON settlements(creditorId)")
            }
        }
    }
}
