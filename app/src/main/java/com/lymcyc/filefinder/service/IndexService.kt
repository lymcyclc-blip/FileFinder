package com.lymcyc.filefinder.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lymcyc.filefinder.R
import com.lymcyc.filefinder.data.AppDatabase
import com.lymcyc.filefinder.scanner.FileScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class IndexService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat(0)
        if (job?.isActive == true) return START_STICKY

        job = scope.launch {
            val dao = AppDatabase.get(this@IndexService).fileDao()
            val scanner = FileScanner(dao)
            try {
                scanner.scanAll { count -> startForegroundCompat(count) }
                startForegroundCompat(-1)
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(count: Int) {
        val text = when {
            count < 0 -> "索引完成"
            count == 0 -> "正在扫描..."
            else -> "已索引 $count 个文件"
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FileFinder")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(count >= 0)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "索引服务", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "indexer"
        private const val NOTIF_ID = 1
    }
}
