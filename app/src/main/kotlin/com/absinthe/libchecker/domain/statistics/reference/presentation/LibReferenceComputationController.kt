package com.absinthe.libchecker.domain.statistics.reference.presentation

import android.content.pm.PackageInfo
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_MAP_RESULT
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_SUBMIT_RESULT
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItem
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceLoadingState
import com.absinthe.libchecker.domain.statistics.reference.repository.PermissionLabelResolver
import com.absinthe.libchecker.domain.statistics.reference.traceReferenceSuspendSection
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceConfigUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceIconPackagesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LibReferenceComputationController(
  private val scope: CoroutineScope,
  private val computeLibReferenceUseCase: ComputeLibReferenceUseCase,
  private val getLibReferenceIconPackagesUseCase: GetLibReferenceIconPackagesUseCase,
  private val getLibReferenceConfigUseCase: GetLibReferenceConfigUseCase,
  private val permissionLabelResolver: PermissionLabelResolver,
  private val updateLoadingState: (LibReferenceLoadingState) -> Unit
) {
  private val _libReference = MutableStateFlow<List<LibReference>?>(null)
  val libReference = _libReference.asStateFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = synchronized(requestLock) { _savedRefList }

  var savedThreshold = getLibReferenceConfigUseCase.threshold

  private val requestLock = Any()
  private val computationMutex = Mutex()
  private var generation = 0L
  private var computationJob: Job? = null

  fun compute() = synchronized(requestLock) {
    computationJob?.cancel()
    val request = ++generation
    updateLoadingState(LibReferenceLoadingState.Preparing)
    _libReference.value = null
    val referenceConfig = getLibReferenceConfigUseCase.getReferenceConfig()
    computationJob = scope.launch(Dispatchers.IO) {
      computationMutex.withLock {
        currentCoroutineContext().ensureActive()
        val index = computeLibReferenceUseCase.buildIndex(referenceConfig) {
          publish(request) { updateLoadingState(LibReferenceLoadingState.Scanning(it)) }
        } ?: return@withLock
        try {
          currentCoroutineContext().ensureActive()
          publish(request) { updateLoadingState(LibReferenceLoadingState.Matching()) }
          val items = computeLibReferenceUseCase.matchRules(
            index,
            getLibReferenceConfigUseCase.getMatchConfig(),
            onProgress = { progress ->
              publish(request) { updateLoadingState(LibReferenceLoadingState.Matching(progress)) }
            }
          ) ?: return@withLock
          currentCoroutineContext().ensureActive()
          publish(request) { updateLoadingState(LibReferenceLoadingState.Organizing()) }
          val refList = traceReferenceSuspendSection(TRACE_REFERENCE_MAP_RESULT) {
            items.mapIndexed { position, item ->
              currentCoroutineContext().ensureActive()
              val reference = item.toLibReference(index.packageInfoByName)
              currentCoroutineContext().ensureActive()
              val progress = ((position + 1).toLong() * 100 / items.size).toInt()
              publish(request) { updateLoadingState(LibReferenceLoadingState.Organizing(progress)) }
              reference
            }
          }
          currentCoroutineContext().ensureActive()
          traceReferenceSuspendSection(TRACE_REFERENCE_SUBMIT_RESULT) {
            publish(request) {
              _savedRefList = refList
              _libReference.value = refList
            }
          }
        } finally {
          index.clear()
        }
      }
    }
  }

  // Completed indexes are released; changing the match config requires a fresh scan.
  fun match() = compute()

  fun refresh() = scope.launch(Dispatchers.IO) {
    val snapshot = synchronized(requestLock) { generation to _savedRefList }
    snapshot.second?.let { ref ->
      val threshold = getLibReferenceConfigUseCase.threshold
      val filtered = ref.filter { it.referredList.size >= threshold }
      currentCoroutineContext().ensureActive()
      publish(snapshot.first) {
        if (_savedRefList === ref && _libReference.value != null) {
          _libReference.value = filtered
        }
      }
    }
  }

  private inline fun publish(request: Long, block: () -> Unit) {
    synchronized(requestLock) {
      if (request == generation) {
        block()
      }
    }
  }

  private fun LibReferenceItem.toLibReference(packageInfoByName: Map<String, PackageInfo>): LibReference {
    return LibReference(
      libName,
      rule,
      referredList,
      type,
      iconPackages = getLibReferenceIconPackagesUseCase(referredList, packageInfoByName),
      resolvedLabel = if (type == PERMISSION) permissionLabelResolver.resolve(libName) else null
    )
  }

  class Factory(
    private val computeLibReferenceUseCase: ComputeLibReferenceUseCase,
    private val getLibReferenceIconPackagesUseCase: GetLibReferenceIconPackagesUseCase,
    private val getLibReferenceConfigUseCase: GetLibReferenceConfigUseCase,
    private val permissionLabelResolver: PermissionLabelResolver
  ) {
    fun create(
      scope: CoroutineScope,
      updateLoadingState: (LibReferenceLoadingState) -> Unit
    ): LibReferenceComputationController {
      return LibReferenceComputationController(
        scope = scope,
        computeLibReferenceUseCase = computeLibReferenceUseCase,
        getLibReferenceIconPackagesUseCase = getLibReferenceIconPackagesUseCase,
        getLibReferenceConfigUseCase = getLibReferenceConfigUseCase,
        permissionLabelResolver = permissionLabelResolver,
        updateLoadingState = updateLoadingState
      )
    }
  }
}
