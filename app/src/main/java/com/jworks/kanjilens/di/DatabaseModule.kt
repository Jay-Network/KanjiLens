package com.jworks.kanjilens.di

import android.content.Context
import androidx.room.Room
import com.jworks.kanjilens.data.local.JMDictDao
import com.jworks.kanjilens.data.local.JMDictDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JMDictDatabase {
        return Room.databaseBuilder(
            context,
            JMDictDatabase::class.java,
            "jmdict.db"
        ).createFromAsset("jmdict.db").build()
    }

    @Provides
    fun provideJMDictDao(database: JMDictDatabase): JMDictDao {
        return database.jmDictDao()
    }
}
