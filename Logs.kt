package drotlin

import android.content.pm.ApplicationInfo
import android.util.AndroidRuntimeException
import android.util.Log


//Debug==================================================
val isDebug: Boolean = (drotlin.applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

val <T : Any>T.LOG_TAG: String
    get() = this.javaClass.simpleName

fun log(tag: String, message: String?) {
    if (isDebug) {
        logReal(tag, message ?: "!null message!")
    }
}

fun log(tag: String, map: Map<String, Any?>) {
    if (isDebug) {
        logReal(tag, map.toString())
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

    sb.append(tableName)
            .append(" : ")
            .append(System.currentTimeMillis().formatDate("HH:mm:ss:SSS"))
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
    sb.append("===\n===\n")
    sb.append("┌")

    for (i in 1..(totalLength - 2)) {
        sb.append(line)
    }

    sb.append("┐")
    sb.append("\n")
    sb.append("│")
    val spaceNum = totalLength - titleLength - 2

    for (i in 1..spaceNum / 2) {
        sb.append(" ")
    }
    sb.append(title)

    //奇数会错位
    for (i in 1..spaceNum / 2 + (spaceNum and 1)) {
        sb.append(" ")
    }
    sb.append("│")
    sb.append("\n")

    sb.append("├")
    for (i in 1..maxKeyLength) {
        sb.append(line)
    }

    sb.append("┬")
    for (i in 1..maxClassLength) {
        sb.append(line)
    }

    sb.append("┬")
    for (i in 1..maxValueLength) {
        sb.append(line)
    }

    sb.append("┤")
    sb.append("\n")

    for (i in 0 until rows) {
        sb.append("│").append(keys[i])
        for (j in 0 until maxKeyLength - keys[i].length) {
            sb.append(" ")
        }

        sb.append("│").append(classs[i])

        for (j in 0 until maxClassLength - classs[i].length) {
            sb.append(" ")
        }

        sb.append("│").append(values[i])
        for (j in 0 until maxValueLength - values[i].length) {
            sb.append(" ")
        }

        sb.append("│").append("\n")

        val isLastLine = i == rows - 1


        sb.append(if (isLastLine) {
            "└"
        } else {
            "├"
        })

        for (j in 0 until maxKeyLength) {
            sb.append(line)
        }

        sb.append(if (isLastLine) {
            "┴"
        } else {
            "┼"
        })
        for (j in 0 until maxClassLength) {
            sb.append(line)
        }

        sb.append(if (isLastLine) {
            "┴"
        } else {
            "┼"
        })
        for (j in 0 until maxValueLength) {
            sb.append(line)
        }
        sb.append(if (isLastLine) {
            "┘"
        } else {
            "┤"
        })
        sb.append("\n")
    }
    logReal(tableName, sb.toString())
}