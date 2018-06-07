@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package androidy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.util.AndroidRuntimeException
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass


@SuppressLint("StaticFieldLeak")
lateinit var xContext: Context
var xDebugMode = false
inline fun Application.initY(init: () -> Unit = {}) {
    xContext = this
    xDebugMode = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (currentProcessName == packageName) {
        init()
    }
}

val currentProcessName: String?
    get() {
        val pid = android.os.Process.myPid()
        val activityManager = xContext
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses.forEach {
            if (it.pid == pid) {
                return it.processName
            }
        }
        return null
    }

//Test====================================================================================================
fun Exception.throwWhenDebug() {
    if (xDebugMode) {
        throw AndroidRuntimeException(this)
    }
}

inline fun <T> T.applyWhenDebug(block: T.() -> Unit): T {
    if (xDebugMode) {
        block()
    }
    return this
}

val Int.valueWhenDebug: Int
    get() = if (xDebugMode) {
        this
    } else 0
val String.valueWhenDebug: String?
    get() = if (xDebugMode) {
        this
    } else null


fun xLog(tag: Any?, msg: Any?) {
    if (xDebugMode) {
        val tagS = (tag as? String) ?: if (tag != null) {
            tag::class.simpleName
        } else {
            null
        } ?: "TAG"
        val msgS = """
            |Log->${"yyyy-MM-dd HH:mm:ss.SSS".formatDate(System.currentTimeMillis())} [${xContext.packageName}]
            |TAG->$tagS
            |MSG->${when (msg) {
            is CharSequence -> msg
            is Throwable -> "${msg.message}\n${msg.stackTrace.joinToString("\n    ->")}"
            is Any -> msg::class.simpleName
            else -> null
        } ?: "NNN null object NNN"}
            """.trimMargin()
        android.util.Log.d(tagS, msgS)
        //System.out.println(msgS)
    }
}


//UI====================================================================================================


fun Context.color(@ColorRes id: Int): Int = ResourcesCompat.getColor(resources, id, null)
fun Context.drawable(@DrawableRes id: Int): Drawable? = ResourcesCompat.getDrawable(resources, id, null)
fun Context.integer(@IntegerRes id: Int): Int = resources.getInteger(id)
fun View.color(@ColorRes id: Int): Int = context.color(id)
fun View.drawable(@DrawableRes id: Int): Drawable? = context.drawable(id)
fun View.integer(@IntegerRes id: Int): Int = context.integer(id)
fun View.string(@StringRes id: Int): String = context.getString(id)

fun noGetter(): Nothing = throw AndroidRuntimeException("No getter")

var TextView.hintTextColorResource: Int
    get() = noGetter()
    set(value) {
        setHintTextColor(context.color(value))
    }

val Int.measureSpecExactly
    get() = View.MeasureSpec.makeMeasureSpec(this, View.MeasureSpec.EXACTLY)

var View.xBackground: Drawable?
    get() = background
    set(value) = ViewCompat.setBackground(this, value)

fun ViewGroup.inflate(layoutId: Int, attach: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attach)
}

fun Context.inflate(layoutId: Int, parent: ViewGroup? = null, attach: Boolean = false): View {
    return LayoutInflater.from(this).inflate(layoutId, parent, attach)
}

fun Bitmap.save2File(file: File): File? {
    try {
        val fOut = FileOutputStream(file)
        compress(Bitmap.CompressFormat.PNG, 100, fOut)
        fOut.flush()
        fOut.close()
        return file
    } catch (e: Exception) {
        e.throwWhenDebug()
    }
    return null
}

fun TextView.enableUnderline() {
    paint.flags = paint.flags or Paint.UNDERLINE_TEXT_FLAG
}

fun Activity.enableImmersionMode() {
    if (SDK_INT >= 19) {
        if (SDK_INT >= 21) {
            window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }
}

val View.statusBarHeight: Int
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            try {
                return resources.getDimensionPixelSize(resourceId)
            } catch (e: Exception) {
                e.throwWhenDebug()
            }
        }
        return 0
    }

inline fun <T : View> T.flparams(
        width: Int = android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        gravity: Int = Gravity.NO_GRAVITY,
        init: FrameLayout.LayoutParams.() -> Unit = {}
): T {
    val layoutParams = FrameLayout.LayoutParams(width, height, gravity)
    layoutParams.init()
    this@flparams.layoutParams = layoutParams
    return this
}

inline fun <T : View> T.llparams(
        width: Int = android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        height: Int = android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        gravity: Int = Gravity.NO_GRAVITY,
        weight: Float = 0f,
        init: LinearLayout.LayoutParams.() -> Unit = {}
): T {
    val layoutParams = LinearLayout.LayoutParams(width, height, weight)
    layoutParams.gravity = gravity
    layoutParams.init()
    this@llparams.layoutParams = layoutParams
    return this
}


//Thread====================================================================================================
val mainHandler by lazy { Handler(Looper.getMainLooper()) }
val isMainThread: Boolean
    get() = Thread.currentThread() === Looper.getMainLooper().thread

fun xRunOnUiThread(delay: Long = 0, run: () -> Unit) {
    if (0L == delay && isMainThread) {
        run()
    }
    mainHandler.postDelayed(run, delay)
}

//string====================================================================================================
private val dateFormatLocal = ThreadLocal<SimpleDateFormat>()

fun getDateFormat(pattern: String): SimpleDateFormat {
    val df = dateFormatLocal.get()
    return if (df != null) {
        df.applyPattern(pattern)
        df
    } else {
        val df2 = SimpleDateFormat(pattern, Locale.CHINA)
        dateFormatLocal.set(df2)
        df2
    }
}

fun String.parseDate(timeString: String): Date {
    return getDateFormat(this).parse(timeString)
}

fun String.formatDate(date: Any): String {
    return getDateFormat(this).format(date)
}

fun String?.safeFloat(def: Float = 0f): Float {
    try {
        if (this != null) {
            return toFloat()
        }
    } catch (e: Exception) {
        e.throwWhenDebug()
    }
    return def
}

operator fun StringBuilder.plus(str: String): StringBuilder {
    return append(str)
}


//context====================================================================================================
inline fun Context.xStartActivity(target: KClass<out Activity>, intent: Intent.() -> Unit = {}) {
    startActivity(Intent(this, target.java).apply(intent))
}

inline fun View.xStartActivity(target: KClass<out Activity>, intent: Intent.() -> Unit = {}) {
    context.xStartActivity(target, intent)
}

inline fun Fragment.xStartActivity(target: KClass<out Activity>, intent: Intent.() -> Unit = {}) {
    startActivity(Intent(context ?: xContext, target.java).apply(intent))
}

fun <S> String.getSystemService(): S {
    return xContext.getSystemService(this) as S
}

val String.PERMISSION_GRANTED: Boolean
    get() = ContextCompat.checkSelfPermission(xContext, this) == PackageManager.PERMISSION_GRANTED

val Array<String>.PERMISSION_GRANTED: Boolean
    get() {
        if (Version.SDK_INT >= 23) {
            forEach {
                if (!it.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

//Other==================================================


inline fun loopDo(count: Int, block: (Int) -> Unit) {
    for (i in 0 until count) {
        block(i)
    }
}

inline fun <T> T.loopApply(count: Int, block: T.(Int) -> T): T {
    var t = this
    for (i in 0 until count) {
        t = block(t, i)
    }
    return t
}

val File.isDirectoryOrCreate: Boolean
    get() {
        if (!exists()) {
            mkdirs()
        }
        return isDirectory
    }

fun File.toUri(): Uri {
    return Uri.fromFile(this)
}

fun Uri.toFile(): File {
    return File(path ?: toString())
}

inline fun Any?.applyWhenNull(run: () -> Unit) {
    if (this == null) {
        run()
    }
}
