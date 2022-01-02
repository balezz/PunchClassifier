package com.punchlab.punchclassifier.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.punchlab.punchclassifier.data.Person
import com.punchlab.punchclassifier.data.Punch
import com.punchlab.punchclassifier.data.VideoSample

@Database(entities = [Person::class, VideoSample::class, Punch::class], version = 5, exportSchema = false)
@TypeConverters(PersonConverters::class)
abstract class PersonPunchDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun punchDao(): PunchDao
    abstract fun videoSampleDao(): VideoSampleDao

    companion object {
        @Volatile
        private var INSTANCE: PersonPunchDatabase? = null

        fun getDatabase(context: Context): PersonPunchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                PersonPunchDatabase::class.java,
                "person_punch_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}