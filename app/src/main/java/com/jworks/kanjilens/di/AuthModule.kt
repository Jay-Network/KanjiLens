package com.jworks.kanjilens.di

import com.jworks.kanjilens.data.auth.AuthRepository
import com.jworks.kanjilens.data.auth.SupabaseConfig
import com.jworks.kanjilens.data.feedback.FeedbackRepositoryImpl
import com.jworks.kanjilens.data.jcoin.JCoinClient
import com.jworks.kanjilens.domain.repository.FeedbackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /** KanjiLens auth Supabase client */
    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.SUPABASE_URL,
            supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
        }
    }

    /** Shared J Coin backend Supabase client (inygcrdhfmoerborxehq) */
    @Provides
    @Singleton
    @Named("jcoin")
    fun provideJCoinSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = SupabaseConfig.JCOIN_SUPABASE_URL,
            supabaseKey = SupabaseConfig.JCOIN_SUPABASE_ANON_KEY
        ) {
            install(Functions)
        }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(@Named("auth") supabaseClient: SupabaseClient): AuthRepository {
        return AuthRepository(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideJCoinClient(@Named("jcoin") supabaseClient: SupabaseClient): JCoinClient {
        return JCoinClient(supabaseClient)
    }

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        @Named("jcoin") supabaseClient: SupabaseClient
    ): FeedbackRepository {
        return FeedbackRepositoryImpl(supabaseClient)
    }
}
