package com.budgetmanager.app.data.di

import com.budgetmanager.app.sms.DefaultInboxScanner
import com.budgetmanager.app.sms.InboxScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsModule {
    @Binds
    @Singleton
    abstract fun bindInboxScanner(impl: DefaultInboxScanner): InboxScanner
}
