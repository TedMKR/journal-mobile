package com.journal.core.data

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the data layer.
 * Repositories are @Singleton and injected via @Inject constructor —
 * no explicit @Provides needed here.
 * This module is a placeholder for future non-constructor bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule
