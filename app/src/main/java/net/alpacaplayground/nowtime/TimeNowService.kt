package net.alpacaplayground.nowtime

import android.app.Service
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class TimeNowService : Service(), Observer<String> {
    override fun onChanged(t: String?) {
        mainBinder?.onMessage(t)
    }

    var mainBinder: ICallMainAidl? = null

    val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mainBinder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mainBinder = ICallMainAidl.Stub.asInterface(service)
        }

    }

    val binder: IBinder = object : ICallTimerAidl.Stub() {
        override fun bindMain() {
            Log.i("TImeNow","bindMain")
            bindService(Intent(this@TimeNowService, MainService::class.java), serviceConnection, BIND_AUTO_CREATE)
        }

        override fun unbindMain() {
            Log.i("TImeNow","unbindMain")
            unbindService(serviceConnection)
        }

    }

    private lateinit var worker: TimeNowWorker

    override fun onCreate() {
        super.onCreate()
        TimeNowWorker(this).apply { worker = this }.liveMsg.observeForever(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = worker.timeNow().let { START_STICKY }

    override fun onDestroy() {
        super.onDestroy()
        worker.liveMsg.removeObserver(this)
        worker.onDestroy()
    }
}