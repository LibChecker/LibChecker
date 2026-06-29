package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.domain.app.FeatureInitializationRepository
import com.absinthe.libchecker.domain.app.FeatureInitializationState
import com.absinthe.libchecker.services.WorkerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkerFeatureInitializationRepository : FeatureInitializationRepository {
  override val state: Flow<FeatureInitializationState> =
    WorkerService.featureInitializationState.map { serviceState ->
      FeatureInitializationState(
        running = serviceState.running,
        completed = serviceState.completed
      )
    }

  override val isRunning: Boolean
    get() = WorkerService.featureInitializationState.value.running
}
