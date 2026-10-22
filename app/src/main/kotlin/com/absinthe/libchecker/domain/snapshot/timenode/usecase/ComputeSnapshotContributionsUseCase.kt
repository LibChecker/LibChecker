package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.LocalDateCompat
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotPalette
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotUpdatedAppItem
import com.absinthe.libchecker.utils.extensions.getAppName
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class ComputeSnapshotContributionsUseCase(
  private val snapshotRepository: SnapshotRepository,
  private val installedAppRepository: InstalledAppRepository? = null
) {

  private data class MutableDayContribution(
    val date: LocalDateCompat,
    var updateCount: Int = 0,
    var snapshotTimestamp: Long? = null,
    var snapshotColor: Int? = null,
    var isSnapshotDay: Boolean = false,
    val updatedApps: MutableList<SnapshotUpdatedAppItem> = mutableListOf(),
    var previousSnapshotTimestamp: Long? = null,
    var currentSnapshotTimestamp: Long? = null
  )

  suspend operator fun invoke(
    timeStamps: List<TimeStampItem>,
    timeZone: TimeZone = TimeZone.getDefault()
  ): SnapshotContributionData = withContext(Dispatchers.Default) {
    val today = LocalDateCompat.now(timeZone)
    val rawStartDate = today.minusDays(RECENT_DAYS_COUNT - 1L)
    val startDayOfWeek = rawStartDate.dayOfWeek
    val startDate = rawStartDate.minusDays((startDayOfWeek - 1).toLong())
    val endDate = today

    if (timeStamps.isEmpty()) {
      val emptyDays = mutableMapOf<LocalDateCompat, DayContribution>()
      var curr = startDate
      while (!curr.isAfter(endDate)) {
        emptyDays[curr] = DayContribution(curr, 0, null, null, false)
        curr = curr.plusDays(1)
      }
      return@withContext SnapshotContributionData(
        days = emptyDays,
        startDate = startDate,
        endDate = endDate,
        snapshotColors = emptyMap(),
        totalUpdates = 0
      )
    }

    val sortedTimeStamps = timeStamps.sortedBy { it.timestamp }
    val snapshotColors = sortedTimeStamps.mapIndexed { index, item ->
      item.timestamp to SnapshotPalette.getColor(index)
    }.toMap()

    val dayContributions = mutableMapOf<LocalDateCompat, MutableDayContribution>()

    // Initialize all dates in range
    var iterDate = startDate
    while (!iterDate.isAfter(endDate)) {
      dayContributions[iterDate] = MutableDayContribution(iterDate)
      iterDate = iterDate.plusDays(1)
    }

    // Mark snapshot creation days
    sortedTimeStamps.forEach { item ->
      val date = LocalDateCompat.fromTimestamp(item.timestamp, timeZone)
      dayContributions[date]?.let { day ->
        day.isSnapshotDay = true
        day.snapshotTimestamp = item.timestamp
        day.snapshotColor = snapshotColors[item.timestamp]
      }
    }

    // Process each snapshot's interval [T_i, T_{i+1})
    for (i in sortedTimeStamps.indices) {
      coroutineContext.ensureActive()
      val current = sortedTimeStamps[i]
      val currentTimestamp = current.timestamp
      val color = snapshotColors[currentTimestamp]

      val intervalStartDate = LocalDateCompat.fromTimestamp(currentTimestamp, timeZone)
      val intervalEndDate = if (i < sortedTimeStamps.size - 1) {
        val nextTimestamp = sortedTimeStamps[i + 1].timestamp
        val nextDate = LocalDateCompat.fromTimestamp(nextTimestamp, timeZone)
        if (nextDate.isAfter(intervalStartDate)) {
          nextDate.minusDays(1)
        } else {
          intervalStartDate
        }
      } else {
        endDate
      }

      val prevTimestampForInterval = currentTimestamp
      val currTimestampForInterval = if (i < sortedTimeStamps.size - 1) {
        sortedTimeStamps[i + 1].timestamp
      } else {
        null
      }

      var currDate = maxOf(intervalStartDate, startDate)
      val visibleEndDate = minOf(intervalEndDate, endDate)
      while (!currDate.isAfter(visibleEndDate)) {
        dayContributions[currDate]?.let { day ->
          day.snapshotTimestamp = currentTimestamp
          day.snapshotColor = color
          day.previousSnapshotTimestamp = prevTimestampForInterval
          day.currentSnapshotTimestamp = currTimestampForInterval
        }
        currDate = currDate.plusDays(1)
      }

      if (i < sortedTimeStamps.size - 1) {
        val nextTimestamp = sortedTimeStamps[i + 1].timestamp
        val nextDate = LocalDateCompat.fromTimestamp(nextTimestamp, timeZone)
        if (nextDate.isBefore(startDate) || intervalStartDate.isAfter(endDate)) continue
        val updatedApps = runCatching {
          snapshotRepository.getSnapshotUpdatedApps(nextTimestamp)
        }.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyList())

        for (app in updatedApps) {
          if (app.lastUpdatedTime in (currentTimestamp + 1)..nextTimestamp) {
            val updateDate = LocalDateCompat.fromTimestamp(app.lastUpdatedTime, timeZone)
            dayContributions[updateDate]?.let { day ->
              if (day.updatedApps.none { it.packageName == app.packageName }) {
                day.updatedApps.add(
                  SnapshotUpdatedAppItem(
                    packageName = app.packageName,
                    label = app.label,
                    previousSnapshotTimestamp = currentTimestamp,
                    currentSnapshotTimestamp = nextTimestamp
                  )
                )
                day.updateCount++
              }
            }
          }
        }
      } else {
        val installedApps = runCatching {
          installedAppRepository?.getApplicationList()
        }.onFailure { if (it is CancellationException) throw it }.getOrNull().orEmpty()

        for (pkg in installedApps) {
          if (pkg.lastUpdateTime > currentTimestamp) {
            val updateDate = LocalDateCompat.fromTimestamp(pkg.lastUpdateTime, timeZone)
            dayContributions[updateDate]?.let { day ->
              if (day.updatedApps.none { it.packageName == pkg.packageName }) {
                val label = pkg.getAppName(SystemServices.packageManager) ?: pkg.packageName
                day.updatedApps.add(
                  SnapshotUpdatedAppItem(
                    packageName = pkg.packageName,
                    label = label,
                    previousSnapshotTimestamp = currentTimestamp,
                    currentSnapshotTimestamp = null
                  )
                )
                day.updateCount++
              }
            }
          }
        }
      }
    }

    // Also include updates recorded on the first snapshot's creation day
    val firstTimestamp = sortedTimeStamps.first().timestamp
    val firstDate = LocalDateCompat.fromTimestamp(firstTimestamp, timeZone)
    val firstSnapshotUpdates = if (firstDate in startDate..endDate) {
      runCatching {
        snapshotRepository.getSnapshotUpdatedApps(firstTimestamp)
      }.onFailure { if (it is CancellationException) throw it }.getOrDefault(emptyList())
    } else {
      emptyList()
    }
    for (app in firstSnapshotUpdates) {
      val updateDate = LocalDateCompat.fromTimestamp(app.lastUpdatedTime, timeZone)
      if (updateDate == firstDate) {
        dayContributions[updateDate]?.let { day ->
          if (day.updatedApps.none { it.packageName == app.packageName }) {
            day.updatedApps.add(
              SnapshotUpdatedAppItem(
                packageName = app.packageName,
                label = app.label,
                previousSnapshotTimestamp = null,
                currentSnapshotTimestamp = firstTimestamp
              )
            )
            day.updateCount++
          }
        }
      }
    }

    var totalUpdates = 0
    val finalDays = dayContributions.mapValues { (_, mutable) ->
      totalUpdates += mutable.updateCount
      DayContribution(
        date = mutable.date,
        updateCount = mutable.updateCount,
        snapshotTimestamp = mutable.snapshotTimestamp,
        snapshotColor = mutable.snapshotColor,
        isSnapshotDay = mutable.isSnapshotDay,
        updatedApps = mutable.updatedApps.toList(),
        previousSnapshotTimestamp = mutable.previousSnapshotTimestamp,
        currentSnapshotTimestamp = mutable.currentSnapshotTimestamp
      )
    }

    SnapshotContributionData(
      days = finalDays,
      startDate = startDate,
      endDate = endDate,
      snapshotColors = snapshotColors,
      totalUpdates = totalUpdates
    )
  }

  companion object {
    const val RECENT_DAYS_COUNT = 120
  }
}
