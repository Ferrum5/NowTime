package net.alpacaplayground.nowtime

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v7.app.AlertDialog
import java.lang.ref.WeakReference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val WhatTimeNow = 1
private const val WhatTimeNowForce = 2
private const val WhatTtsRelease = 3


private const val FlagReport0 = 0
private const val FlagReport15 = 1
private const val FlagReport30 = 2
private const val FlagReport45 = 3

val liveMsg = MutableLiveData<String>()
val liveTtsStatus = MutableLiveData<String>()

object TimeNowWorker : Handler.Callback {

    var context by RefDelegate<Context>()

    private var reportFlag = FlagReport0
    private val time = Time()
    private var tts: TextToSpeech? = null

    private val timerHandler: Handler = HandlerThread("TimeNow").let {
        it.start()
        Handler(it.looper, this)
    }

    val ttsListener = object : UtteranceProgressListener() {
        override fun onError(utteranceId: String?) {
            releaseTts()
            toast("TTS异常:${utteranceId}")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            releaseTts()
            toast("TTS异常:${utteranceId},code:$errorCode")
        }

        override fun onStart(utteranceId: String?) {
        }

        override fun onDone(utteranceId: String?) {
            releaseTts()
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            releaseTts()
        }

        fun releaseTts() {
            timerHandler.sendEmptyMessage(WhatTtsRelease)
        }
    }

    override fun handleMessage(msg: Message): Boolean {

        //释放tts
        if (msg.what == WhatTtsRelease) {
            liveTtsStatus.postValue(null)
            timerHandler.removeMessages(WhatTtsRelease)
            tts?.apply {
                setOnUtteranceProgressListener(null)
                shutdown()
            }
            tts = null
            return true
        }

        timerHandler.removeMessages(WhatTimeNow)
        timerHandler.removeMessages(WhatTimeNowForce)

        time.updateToNow()
        val minute = time.minute

        var speech = WhatTimeNowForce == msg.what


        //判断是否播报
        val nextReportMinute = when {
            minute in 0..14 -> {
                if (reportFlag != FlagReport15) {
                    reportFlag = FlagReport15
                    speech = true
                }
                15
            }
            minute in 15..29 -> {
                if (reportFlag != FlagReport30) {
                    reportFlag = FlagReport30
                    speech = true
                }
                30
            }
            minute in 30..44 -> {
                if (reportFlag != FlagReport45) {
                    reportFlag = FlagReport45
                    speech = true
                }
                45
            }
            else -> {
                if (reportFlag != FlagReport0) {
                    reportFlag = FlagReport0
                    speech = true
                }
                60
            }
        }


        liveMsg.postValue("""
                |距下次播报时间
                |${String.format("%02d:%02d",
                if (nextReportMinute == 60) time.nextHour else time.hour,
                if(nextReportMinute == 60) 0 else nextReportMinute)}
                |还有${nextReportMinute - minute}分钟
                """.trimMargin())

        //报时
        if (speech) {
            if (minute <= 1) {
                speech("现在时间,${time.hour}点整")
            } else if (minute >= 59) {
                speech("现在时间,${time.nextHour}点整")
            } else {
                speech("现在时间,${time.hourMinute("%02d点%02d分")}")
            }
        }

        //下次消息
        time.updateToNow()
        val messageDelaySecond = time.second.let { if (it >= 30) 60 - it else 30 - it }
        log("下一分钟消息延迟${messageDelaySecond}s")
        timerHandler.sendEmptyMessageDelayed(WhatTimeNow, messageDelaySecond * 1000L - time.milliSecond)

        return true
    }

    fun timeNow() {
        timerHandler.removeCallbacksAndMessages(null)
        timerHandler.sendEmptyMessage(WhatTimeNowForce)
    }

    private fun speech(s: String) {
        timerHandler.removeMessages(WhatTtsRelease)
        var ttsPrepared = false
        var ttsCode = TextToSpeech.ERROR
        val tts = tts?.apply { ttsPrepared = true } ?: TextToSpeech(context) {
            ttsCode = it
            ttsPrepared = true
        }.apply { this@TimeNowWorker.tts = this }
        while (!ttsPrepared) {
            liveTtsStatus.postValue("等待TTS初始化")
            SystemClock.sleep(1000)
        }
        liveTtsStatus.postValue("正在报时")
        if (ttsCode == TextToSpeech.SUCCESS) {
            //监听
            tts.setOnUtteranceProgressListener(ttsListener)

            //语音
            if (Build.VERSION.SDK_INT >= 21) {
                tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "time")
            } else {
                tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
            }

        } else {
            liveTtsStatus.postValue(null)
            toast("TTS初始化失败")
        }
    }

    private fun toast(s: String) {
        (this.context as? Activity)?.apply {
            runOnUiThread {
                AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage(s)
                        .setPositiveButton("OK") { d, _ -> d.dismiss() }
                        .show()
            }
        }
    }

    private fun log(s: String) {
        android.util.Log.i("TimeNow", s)
    }
}

private class RefDelegate<T> : ReadWriteProperty<Any?, T?> {

    private var ref: WeakReference<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>) = ref?.get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value != null) {
            if (ref?.get() !== value) {
                ref = WeakReference(value)
            }
        } else {
            ref = null
        }
    }

}