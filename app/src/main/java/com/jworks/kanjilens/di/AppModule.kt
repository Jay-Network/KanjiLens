package com.jworks.kanjilens.di

import android.content.Context
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.jworks.kanjilens.data.preferences.SettingsDataStore
import com.jworks.kanjilens.data.remote.KuroshiroApi
import com.jworks.kanjilens.data.repository.FuriganaRepositoryImpl
import com.jworks.kanjilens.data.repository.SettingsRepositoryImpl
import com.jworks.kanjilens.domain.repository.FuriganaRepository
import com.jworks.kanjilens.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer {
        return TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build()
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // localhost from emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideKuroshiroApi(retrofit: Retrofit): KuroshiroApi {
        return retrofit.create(KuroshiroApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFuriganaRepository(impl: FuriganaRepositoryImpl): FuriganaRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository {
        return impl
    }
}
