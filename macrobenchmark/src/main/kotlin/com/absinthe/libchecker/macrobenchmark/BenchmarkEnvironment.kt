package com.absinthe.libchecker.macrobenchmark

import android.app.KeyguardManager
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry

internal fun MacrobenchmarkScope.requireUncontaminatedBenchmarkEnvironment() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val keyguardManager = context.getSystemService(KeyguardManager::class.java)
  check(!keyguardManager.isDeviceLocked && !keyguardManager.isKeyguardLocked) {
    "Benchmark device is locked. Unlock it before collecting performance data."
  }

  val mirrorPid = device.executeShellCommand("pidof $ANDROMELD_MIRROR_PROCESS").trim()
  check(mirrorPid.isEmpty()) {
    "AndroMeld mirror is running (pid=$mirrorPid). Close the mirror before collecting performance data."
  }
}

private const val ANDROMELD_MIRROR_PROCESS = "com.catchingnow.andfiles.helper:mirror"
