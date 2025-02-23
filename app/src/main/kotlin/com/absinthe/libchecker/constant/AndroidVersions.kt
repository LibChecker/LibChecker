package com.absinthe.libchecker.constant

import android.os.Build
import com.absinthe.libchecker.R
import java.util.Calendar
import java.util.Date

object AndroidVersions {
  val versions = listOf(
    Node(Build.VERSION_CODES.CUR_DEVELOPMENT, "Developing", "", null, Date()),
    Node(1, "Base", "1.0", null, getDate(2008, 9)),
    Node(2, "Base MR1", "1.1", null, getDate(2009, 2)),
    Node(3, "Cupcake", "1.5", null, getDate(2009, 4)),
    Node(4, "Donut", "1.6", null, getDate(2009, 9)),
    Node(5, "Eclair", "2.0", null, getDate(2009, 10)),
    Node(6, "Eclair", "2.0.1", null, getDate(2009, 12)),
    Node(7, "Eclair MR1", "2.1", null, getDate(2010, 1)),
    Node(8, "Froyo", "2.2", null, getDate(2010, 5)),
    Node(9, "Gingerbread", "2.3 - 2.3.2", null, getDate(2010, 12)),
    Node(10, "Gingerbread MR1", "2.3.3 - 2.3.7 ", null, getDate(2011, 2)),
    Node(11, "Honeycomb", "3.0", null, getDate(2011, 2)),
    Node(12, "Honeycomb MR1", "3.1", null, getDate(2011, 5)),
    Node(13, "Honeycomb MR2", "3.2.x", null, getDate(2011, 7)),
    Node(14, "Ice Cream Sandwich", "4.0.1 - 4.0.2", R.drawable.ic_android_i, getDate(2011, 10)),
    Node(15, "Ice Cream Sandwich MR1", "4.0.3 - 4.0.4", R.drawable.ic_android_i, getDate(2011, 12)),
    Node(16, "Jelly Bean", "4.1.x", R.drawable.ic_android_j, getDate(2012, 7)),
    Node(17, "Jelly Bean MR1", "4.2.x", R.drawable.ic_android_j, getDate(2012, 11)),
    Node(18, "Jelly Bean MR2", "4.3.x", R.drawable.ic_android_j, getDate(2013, 7)),
    Node(19, "KitKat", "4.4 - 4.4.4", R.drawable.ic_android_k, getDate(2013, 10)),
    Node(20, "KitKat Watch", "4.4W", R.drawable.ic_android_k, getDate(2014, 6)),
    Node(21, "Lollipop", "5.0", R.drawable.ic_android_l, getDate(2014, 11)),
    Node(22, "Lollipop MR1", "5.1", R.drawable.ic_android_l, getDate(2015, 3)),
    Node(23, "Marshmallow", "6.0", R.drawable.ic_android_m, getDate(2015, 10)),
    Node(24, "Nougat", "7.0", R.drawable.ic_android_n, getDate(2016, 8)),
    Node(25, "Nougat MR1", "7.1", R.drawable.ic_android_n, getDate(2016, 10)),
    Node(26, "Oreo", "8.0", R.drawable.ic_android_o, getDate(2017, 8)),
    Node(27, "Oreo MR1", "8.1", R.drawable.ic_android_o_mr1, getDate(2017, 12)),
    Node(28, "Pie", "9", R.drawable.ic_android_p, getDate(2018, 8)),
    Node(29, "Android 10", "10", R.drawable.ic_android_q, getDate(2019, 9)),
    Node(30, "Android 11", "11", R.drawable.ic_android_r, getDate(2020, 9)),
    Node(31, "Android 12", "12", R.drawable.ic_android_s, getDate(2021, 10)),
    Node(32, "Android 12L", "12.1", R.drawable.ic_android_s, getDate(2022, 3)),
    Node(33, "Tiramisu", "13", R.drawable.ic_android_t, getDate(2022, 10)),
    Node(34, "UpsideDownCake", "14", R.drawable.ic_android_u, getDate(2023, 10)),
    Node(35, "Vanilla Ice Cream", "15", R.drawable.ic_android_v, getDate(2024, 10)),
    Node(36, "Baklava", "16", R.drawable.ic_android_baklava, getDate(2025, 5))
  )

  val simpleVersions = versions.associate {
    it.version to it.versionName.takeWhile { c -> c != ' ' }
  }

  private fun getDate(year: Int, month: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)
    return calendar.time
  }

  data class Node(
    val version: Int,
    val codeName: String,
    val versionName: String,
    val iconRes: Int?,
    val releaseDate: Date
  )
}
