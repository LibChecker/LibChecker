package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object AndroidVersions {
  val versions = listOf(
    Triple(1, "1.0", null),
    Triple(2, "1.1", null),
    Triple(3, "Cupcake, 1.5", null),
    Triple(4, "Donut, 1.6", null),
    Triple(5, "Eclair, 2.0", null),
    Triple(6, "Eclair, 2.0.1", null),
    Triple(7, "Eclair, 2.1", null),
    Triple(8, "Froyo, 2.2.x", null),
    Triple(9, "Gingerbread, 2.3 - 2.3.2", null),
    Triple(10, "Gingerbread, 2.3.3 - 2.3.7", null),
    Triple(11, "Honeycomb, 3.0", null),
    Triple(12, "Honeycomb, 3.1", null),
    Triple(13, "Honeycomb, 3.2.x", null),
    Triple(14, "Ice Cream Sandwich, 4.0.1 - 4.0.2", R.drawable.ic_android_i),
    Triple(15, "Ice Cream Sandwich, 4.0.3 - 4.0.4", R.drawable.ic_android_i),
    Triple(16, "Jelly Bean, 4.1.x", R.drawable.ic_android_j),
    Triple(17, "Jelly Bean, 4.2.x", R.drawable.ic_android_j),
    Triple(18, "Jelly Bean, 4.3.x", R.drawable.ic_android_j),
    Triple(19, "KitKat, 4.4 - 4.4.4", R.drawable.ic_android_k),
    Triple(20, "KitKat, 4.4W", R.drawable.ic_android_k),
    Triple(21, "Lollipop, 5.0", R.drawable.ic_android_l),
    Triple(22, "Lollipop, 5.1", R.drawable.ic_android_l),
    Triple(23, "Marshmallow, 6.0", R.drawable.ic_android_m),
    Triple(24, "Nougat, 7.0", R.drawable.ic_android_n),
    Triple(25, "Nougat, 7.1", R.drawable.ic_android_n),
    Triple(26, "Oreo, 8.0.0", R.drawable.ic_android_o),
    Triple(27, "Oreo, 8.1.0", R.drawable.ic_android_o_mr1),
    Triple(28, "Pie, 9", R.drawable.ic_android_p),
    Triple(29, "Android 10", R.drawable.ic_android_q),
    Triple(30, "Android 11", R.drawable.ic_android_r),
    Triple(31, "Android 12", R.drawable.ic_android_s),
    Triple(32, "Android 12L, 12.1", R.drawable.ic_android_s),
    Triple(33, "Tiramisu, 13", R.drawable.ic_android_t)
  )

  /**
   * shorter version, for displaying in app list
   */
  val apiToAndroidVerMap = arrayOf(
    "",     //0
    "1.0",  //1
    "1.1",  //2
    "1.5",  //3
    "1.6",  //4
    "2.0",  //5
    "2.0.1",//6
    "2.1.x",//7
    "2.2.x",//8
    "2.3~2.3.2",//9
    "2.3.3~2.3.4",//10
    "3.0.x",//11
    "3.1.x",//12
    "3.2",//13
    "4.0~4.0.2",//14
    "4.0.3~4.0.4",//15
    "4.1~4.1.1",//16
    "4.2~4.2.2",//17
    "4.3",//18
    "4.4",//19
    "4.4W",//20
    "5.0",//21
    "5.1",//22
    "6.0",//23
    "7.0",//24
    "7.1~7.1.1",//25
    "8.0",//26
    "8.1",//27
    "9",//28
    "10",//29
    "11",//30
    "12",//31
    "12L",//32
    "13")//33
}
