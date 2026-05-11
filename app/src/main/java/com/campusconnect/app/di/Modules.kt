package com.campusconnect.app.di

import android.content.Context
import androidx.room.Room
import com.campusconnect.app.data.local.database.CampusDatabase
import com.campusconnect.app.data.remote.CloudinaryApi
import com.campusconnect.app.data.remote.CloudinaryService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = 50L * 1024 * 1024
        }
    }

    @Provides @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = Firebase.messaging
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CampusDatabase =
        Room.databaseBuilder(ctx, CampusDatabase::class.java, CampusDatabase.DATABASE_NAME)
            .addMigrations(
                CampusDatabase.MIGRATION_1_2,
                CampusDatabase.MIGRATION_2_3   // FIX: register the new migration
            )

            .build()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideCloudinaryApi(okHttpClient: OkHttpClient): CloudinaryApi =
        Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)

    @Provides @Singleton
    fun provideCloudinaryService(
        api: CloudinaryApi,
        @ApplicationContext context: Context
    ): CloudinaryService = CloudinaryService(api, context)
}