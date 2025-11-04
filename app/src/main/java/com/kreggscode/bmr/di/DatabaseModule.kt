package com.kreggscode.bmr.di

import android.content.Context
import androidx.room.Room
import com.kreggscode.bmr.data.local.BMRDatabase
import com.kreggscode.bmr.data.local.dao.*
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
    fun provideBMRDatabase(
        @ApplicationContext context: Context
    ): BMRDatabase {
        return Room.databaseBuilder(
            context,
            BMRDatabase::class.java,
            BMRDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: BMRDatabase): UserDao = database.userDao()
    
    @Provides
    @Singleton
    fun provideBMRDao(database: BMRDatabase): BMRDao = database.bmrDao()
    
    @Provides
    @Singleton
    fun provideFoodDao(database: BMRDatabase): FoodDao = database.foodDao()
    
    @Provides
    @Singleton
    fun provideDietPlanDao(database: BMRDatabase): DietPlanDao = database.dietPlanDao()
    
    @Provides
    @Singleton
    fun provideSleepDao(database: BMRDatabase): SleepDao = database.sleepDao()
    
    @Provides
    @Singleton
    fun provideWaterDao(database: BMRDatabase): WaterDao = database.waterDao()
}
