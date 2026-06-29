package com.absinthe.libchecker.domain.statistics.reference.presentation

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItem
import com.absinthe.libchecker.domain.statistics.reference.usecase.ComputeLibReferenceUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceConfigUseCase
import com.absinthe.libchecker.domain.statistics.reference.usecase.GetLibReferenceIconPackagesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class LibReferenceComputationController(
  private val scope: CoroutineScope,
  private val computeLibReferenceUseCase: ComputeLibReferenceUseCase,
  private val getLibReferenceIconPackagesUseCase: GetLibReferenceIconPackagesUseCase,
  private val getLibReferenceConfigUseCase: GetLibReferenceConfigUseCase,
  private val updateProgress: (Int) -> Unit
) {
  private val _libReference = MutableSharedFlow<List<LibReference>?>()
  val libReference = _libReference.asSharedFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = _savedRefList

  private var referenceIndex: ComputeLibReferenceUseCase.ReferenceIndex? = null
  var savedThreshold = getLibReferenceConfigUseCase.threshold

  private var computeLibReferenceJob: Job? = null
  private var matchingJob: Job? = null

  fun compute() {
    computeLibReferenceJob?.cancel()
    cancelMatchingJob()
    computeLibReferenceJob = scope.launch(Dispatchers.IO) {
      referenceIndex?.clear()
      referenceIndex = null
      _libReference.emit(null)
      val index = computeLibReferenceUseCase.buildIndex(
        getLibReferenceConfigUseCase.getReferenceConfig(),
        updateProgress
      ) ?: return@launch
      referenceIndex = index
      match(index)
    }
  }

  fun match() {
    val index = referenceIndex ?: run {
      compute()
      return
    }
    match(index)
  }

  fun cancelMatchingJob() {
    matchingJob?.cancel()
    matchingJob = null
  }

  fun refresh() = scope.launch(Dispatchers.IO) {
    _savedRefList?.let { ref ->
      val threshold = getLibReferenceConfigUseCase.threshold
      _libReference.emit(ref.filter { it.referredList.size >= threshold })
    }
  }

  private fun match(index: ComputeLibReferenceUseCase.ReferenceIndex) {
    matchingJob?.cancel()
    matchingJob = scope.launch(Dispatchers.IO) {
      try {
        val refList = computeLibReferenceUseCase.matchRules(
          index,
          getLibReferenceConfigUseCase.getMatchConfig(),
          updateProgress
        )?.map { it.toLibReference(index.packageInfoByName) } ?: return@launch

        _libReference.emit(refList)
        _savedRefList = refList
      } finally {
        if (referenceIndex === index) {
          referenceIndex = null
        }
        index.clear()
      }
    }
  }

  private fun LibReferenceItem.toLibReference(packageInfoByName: Map<String, PackageInfo>): LibReference {
    return LibReference(
      libName,
      rule,
      referredList,
      type,
      iconPackages = getLibReferenceIconPackagesUseCase(referredList, packageInfoByName)
    )
  }

  class Factory(
    private val computeLibReferenceUseCase: ComputeLibReferenceUseCase,
    private val getLibReferenceIconPackagesUseCase: GetLibReferenceIconPackagesUseCase,
    private val getLibReferenceConfigUseCase: GetLibReferenceConfigUseCase
  ) {
    fun create(
      scope: CoroutineScope,
      updateProgress: (Int) -> Unit
    ): LibReferenceComputationController {
      return LibReferenceComputationController(
        scope = scope,
        computeLibReferenceUseCase = computeLibReferenceUseCase,
        getLibReferenceIconPackagesUseCase = getLibReferenceIconPackagesUseCase,
        getLibReferenceConfigUseCase = getLibReferenceConfigUseCase,
        updateProgress = updateProgress
      )
    }
  }
}
