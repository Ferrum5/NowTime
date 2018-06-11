package net.alpacaplayground.nowtime

import android.content.Context

fun Context.dip(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}