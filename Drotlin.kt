package drotlin

import android.app.Activity
import android.app.Fragment
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment as FragmentV4
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Handler
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.File
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

typealias GroupLayoutParams = android.view.ViewGroup.LayoutParams
typealias MarginLayoutParams = android.view.ViewGroup.MarginLayoutParams
typealias LinearLayoutParams = android.widget.LinearLayout.LayoutParams
typealias FrameLayoutParams = android.widget.FrameLayout.LayoutParams
typealias Permissions = android.Manifest.permission

fun initDrotlin(context: Context) {
    drotlin.applicationContext = context.applicationContext
}

//Context==================================================
lateinit var applicationContext: Context

fun Activity.alert(message: String) {
    AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage(message)
            .setPositiveButton("ok", { d, _ -> d.dismiss() })
            .show()
}

fun Fragment.alert(message: String) {
    activity?.alert(message)
}

fun FragmentV4.alert(message: String) {
    activity?.alert(message)
}

inline fun <T : Activity> Context.startActivity(clazz: KClass<T>, handler: Intent.() -> Unit = {}) {
    val intent = Intent(this, clazz.java)
    handler(intent)
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

inline fun <reified T : ViewModel> FragmentActivity.getViewModel(): T {
    return ViewModelProviders.of(this).get(T::class.java)
}


inline fun <reified T : ViewModel> FragmentV4.getViewModel(): T {
    return ViewModelProviders.of(this).get(T::class.java)
}

fun toast(message: String) {
    val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}


//Thread==================================================
val mainHandler = Handler(Looper.getMainLooper())

fun <T> T.runOnUiThread(delay: Long = 0, runnable: T.() -> Unit) {
    mainHandler.postDelayed({ runnable() }, delay)
}

fun runOnUiThread(delay: Long = 0, runnable: () -> Unit) {
    mainHandler.postDelayed({ runnable() }, delay)
}

//Ui==================================================
fun View.inflate(layoutId: Int, viewGroup: ViewGroup? = parent as? ViewGroup, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, viewGroup, attachToRoot)
}

fun ViewGroup.inflate(layoutId: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

fun Context.inflate(layoutId: Int, viewGroup: ViewGroup? = null, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(this).inflate(layoutId, viewGroup, attachToRoot)
}

val Int.dimensionPixelSize: Int
    get() = applicationContext.resources.getDimensionPixelSize(this)

val Number.dip2Px: Int
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, toFloat(), applicationContext.resources.displayMetrics)
            .toInt()

val Int.colorResource: Int
    get() = if (Build.VERSION.SDK_INT >= 23) {
        applicationContext.resources.getColor(this, applicationContext.theme)
    } else {
        applicationContext.resources.getColor(this)
    }

val Int.stringResource: String
    get() = applicationContext.resources.getString(this)

fun Int.setAlpha(alpha: Int): Int {
    return (this and 0x00ffffff) or (alpha shl 24)
}

fun Int.toMeasureSpec(mode: Int): Int {
    return View.MeasureSpec.makeMeasureSpec(this, mode)
}

operator fun ViewGroup.plus(child: View): ViewGroup {
    this.addView(child)
    return this
}

operator fun ViewGroup.minus(child: View): ViewGroup {
    this.removeView(child)
    return this
}

operator fun ViewGroup.get(index: Int): View? {
    return getChildAt(index)
}

//File==================================================
val File?.exists: Boolean
    get() = this != null && exists()

val File?.isDirectory: Boolean
    get() = this != null && this.exists() && this.isDirectory

val pictureDir: File
    get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

fun String.toFile(): File {
    return File(this)
}

val File.uri: Uri
    get() = Uri.fromFile(this)

//String==================================================
val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }

val String.uri: Uri
    get() = Uri.parse(this)

fun Any?.stringValue(nullReplace: String = ""): String {
    return when {
        this == null -> nullReplace
        this is String -> this
        else -> return toString()
    }
}

val String.PERMISSION_GRANTED: Boolean
    get() = ContextCompat.checkSelfPermission(applicationContext, this) == PackageManager.PERMISSION_GRANTED

val String.PERMISSION_DENIED: Boolean
    get() = ContextCompat.checkSelfPermission(applicationContext, this) == PackageManager.PERMISSION_DENIED

fun String.toDate(pattern: String): Date? {
    dateFormat.applyPattern(pattern)
    return try {
        dateFormat.parse(this)
    } catch (e: Exception) {
        null
    }
}

fun Number.formatDate(pattern: String): String? {
    dateFormat.applyPattern(pattern)
    return try {
        dateFormat.format(this)
    } catch (e: Exception) {
        null
    }
}

fun StringBuilder.clear(): StringBuilder {
    return delete(0, length)
}

operator fun StringBuilder.plus(s: String?): StringBuilder {
    if(s!=null){
        append(s)
    }
    return this
}

operator fun StringBuilder.minus(count: Int): StringBuilder {
    if (count > length) {
        delete(0, length)
    } else {
        delete(0, length - count)
    }
    return this
}

operator fun StringBuilder.times(time: Int): StringBuilder {
    when{
        time<=0 -> clear()
        time==1 -> {}
        time == 2 ->append(toString())
        else -> {
            val current = toString()
            loopDo(time-1){append(current)}
        }
    }
    return this
}

fun StringBuilder.nextLine(): StringBuilder {
    append("\n")
    return this
}

fun StringBuilder.tab(): StringBuilder{
    space(4)
    return this
}

fun StringBuilder.space(count: Int = 1): StringBuilder{
    appendMultiTimes(count," ")
    return this
}

fun StringBuilder.appendMultiTimes(time: Int, s: String): StringBuilder{
    if(time==1){
        append(s)
    }else if(time>1){
        loopDo(time){append(s)}
    }
    return this
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


fun test() {
    var sb = StringBuilder()
    sb += "5"
}
