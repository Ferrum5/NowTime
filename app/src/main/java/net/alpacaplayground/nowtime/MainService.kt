package net.alpacaplayground.nowtime

import android.app.Service
import android.arch.lifecycle.MutableLiveData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

val liveMsg = MutableLiveData<String>()

class MainService : Service() {

    val binder: IBinder = object : ICallMainAidl.Stub() {
        override fun onMessage(msg: String?) {
            liveMsg.postValue(msg)
        }

    }

    var timerBinder: ICallTimerAidl? = null

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            timerBinder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val timerBinder = ICallTimerAidl.Stub.asInterface(service)
            this@MainService.timerBinder = timerBinder
            timerBinder.bindMain()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startService(Intent(this@MainService, TimeNowService::class.java))
        if (timerBinder == null) {
            bindService(Intent(this@MainService, TimeNowService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timerBinder?.unbindMain()
        unbindService(serviceConnection)
    }
}