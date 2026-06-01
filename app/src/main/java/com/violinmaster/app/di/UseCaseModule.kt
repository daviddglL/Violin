package com.violinmaster.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing domain use cases.
 *
 * All use cases use @Inject constructor, so this module is initially
 * empty. Individual @Provides methods are added as use cases are
 * wired in via PR#4.
 *
 * REQ-ARCH-008-S2: All use cases provided via Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
  // Use cases use @Inject constructor — no explicit @Provides needed.
  // Placeholder for future @Binds or custom scoping.
}
