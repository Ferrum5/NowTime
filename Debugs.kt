package drotlin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.AndroidRuntimeException
import android.util.Log


var isDebug: Boolean = false

fun setDebugEnable(context: Context) {
    isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

val <T : Any>T.LOG_TAG: String
    get() = this.javaClass.simpleName

fun log(tag: String, message: Any?) {
    if (isDebug) {
        logReal(tag, message as? String ?: message?.toString() ?: "!null message!")
    }
}

private fun logReal(tag: String, msg: String) {
    Log.d(tag, msg)
}

fun throwException(e: Exception) {
    if (isDebug) {
        runOnUiThread { throw AndroidRuntimeException(e) }
    }
}

fun logException(e: Exception, tag: String = e.javaClass.simpleName, message: String = e.message
        ?: e.javaClass.name) {
    if (isDebug) {
        Log.e(tag, message, e)
    }
}

inline fun <T> T.applyWhenTest(runnable: T.() -> Unit): T {
    if (isDebug) {
        runnable()
    }
    return this
}

inline fun <T> T.assertApply(assertBlock: T.() -> Boolean): T {
    if (isDebug) {
        if (!assertBlock(this)) {
            throw AndroidRuntimeException("Assert check failed")
        }
    }
    return this
}

fun logT(tableName: String, map: Map<String, Any?>) {
    if (!isDebug) return

    val sb = StringBuilder()

    sb + tableName + " : " + System.currentTimeMillis().formatDate("HH:mm:ss:SSS")
    val rows = map.size
    val ks = arrayOfNulls<String>(rows)
    val vs = arrayOfNulls<String>(rows)
    val cs = arrayOfNulls<String>(rows)
    var index = 0
    for ((k, v) in map) {
        ks[index] = k
        vs[index] = v?.toString() ?: "!null value!"
        cs[index++] = v?.javaClass?.simpleName ?: "!null class!"
    }
    val keys = ks.filterNotNull()
    val values = vs.filterNotNull()
    val classs = cs.filterNotNull()

    var maxKeyLength = 0
    var maxValueLength = 0
    var maxClassLength = 0
    keys.forEach { maxKeyLength = Math.max(maxKeyLength, it.length) }
    values.forEach { maxValueLength = Math.max(maxValueLength, it.length) }
    classs.forEach { maxClassLength = Math.max(maxClassLength, it.length) }

    val title = sb.toString()
    var titleLength = title.length + 2
    var totalLength = maxKeyLength + maxValueLength + maxClassLength + 4
    if (titleLength > totalLength) {
        totalLength = titleLength
    }

    if (totalLength > maxKeyLength + maxValueLength + maxClassLength + 4) {
        maxValueLength = totalLength - (maxKeyLength + maxClassLength + 4)
    }

    val line = "─"

    titleLength -= 2

    sb.clear()
    sb + "==="
    sb.nextLine()
    sb + "==="
    sb.nextLine()

    //表头上边
    sb + "┌"
    sb.appendMultiTimes(totalLength - 2,line)
    sb + "┐"
    sb.nextLine()
    //表头
    sb + "│"
    val spaceNum = totalLength - titleLength - 2
    sb.space(spaceNum / 2)
    sb + title
    //奇数会错位
    sb.space(spaceNum / 2 + (spaceNum and 1))
    sb + "│"
    //表头下边
    sb.nextLine()
    sb + "├"
    sb.appendMultiTimes(maxKeyLength,line)
    sb + "┬"
    sb.appendMultiTimes(maxClassLength,line)
    sb + "┬"
    sb.appendMultiTimes(maxValueLength,line)
    sb + "┤"
    sb.nextLine()

    for (i in 0 until rows) {
        sb + "│" + keys[i]
        sb.space(maxKeyLength - keys[i].length)

        sb + "│" + classs[i]
        sb.space(maxClassLength - classs[i].length)

        sb + "│" + values[i]
        sb.space(maxValueLength - values[i].length)

        sb + "│"
        sb.nextLine()

        val isLastLine = i == rows - 1

        sb + if (isLastLine) {
            "└"
        } else {
            "├"
        }

        sb.appendMultiTimes(maxKeyLength, line)


        sb + if (isLastLine) {
            "┴"
        } else {
            "┼"
        }

        sb.appendMultiTimes(maxClassLength, line)

        sb + if (isLastLine) {
            "┴"
        } else {
            "┼"
        }
        sb.appendMultiTimes(maxValueLength, line)


        sb + if (isLastLine) {
            "┘"
        } else {
            "┤"
        }
        sb.nextLine()
    }
    logReal(tableName, sb.toString())
}