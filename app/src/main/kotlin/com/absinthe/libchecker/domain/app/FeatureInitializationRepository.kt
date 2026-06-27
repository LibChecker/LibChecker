package com.absinthe.libchecker.domain.app

import kotlinx.coroutines.flow.Flow

interface FeatureInitializationRepository {
  val state: Flow<FeatureInitializationState>
  val isRunning: Boolean
}

data class FeatureInitializationState(
  val running: Boolean,
  val completed: Boolean
)
