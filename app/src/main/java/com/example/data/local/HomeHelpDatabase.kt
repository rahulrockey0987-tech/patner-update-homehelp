package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OrderEntity::class,
        PartnerAccountEntity::class,
        WorkCategoryEntity::class,
        SessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HomeHelpDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun partnerAccountDao(): PartnerAccountDao
    abstract fun workCategoryDao(): WorkCategoryDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: HomeHelpDatabase? = null

        fun getDatabase(context: Context): HomeHelpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HomeHelpDatabase::class.java,
                    "homehelp_partner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
