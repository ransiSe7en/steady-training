package com.steady.training

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object RestAlerts {
    private const val CHANNEL = "rest_timer_v1"
    private const val REQUEST = 410
    private fun prefs(c:Context)=c.getSharedPreferences("rest_timer",Context.MODE_PRIVATE)
    private fun intent(c:Context)=PendingIntent.getBroadcast(c,REQUEST,Intent(c,RestReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun channel(c:Context){
        val channel=NotificationChannel(CHANNEL,"Rest timer",NotificationManager.IMPORTANCE_HIGH).apply {
            description="Sounds when your workout rest finishes"
            enableVibration(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build())
        }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    fun schedule(c:Context,seconds:Int){
        stop(c)
        val end=System.currentTimeMillis()+seconds.coerceIn(1,86400)*1000L
        prefs(c).edit().putLong("end",end).apply()
        arm(c,end)
    }
    fun rearm(c:Context){val end=prefs(c).getLong("end",0);if(end>System.currentTimeMillis())arm(c,end)}
    private fun arm(c:Context,end:Long){
        val alarms=c.getSystemService(AlarmManager::class.java)
        if(Build.VERSION.SDK_INT<31||alarms.canScheduleExactAlarms())alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,intent(c))
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,intent(c))
    }
    fun stop(c:Context){c.getSystemService(AlarmManager::class.java).cancel(intent(c));prefs(c).edit().remove("end").apply()}
    @Synchronized fun finish(c:Context){
        val end=prefs(c).getLong("end",0)
        if(end==0L||end>System.currentTimeMillis()+500)return
        prefs(c).edit().remove("end").apply()
        c.getSystemService(AlarmManager::class.java).cancel(intent(c))
        channel(c)
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val open=PendingIntent.getActivity(c,REQUEST,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Rest finished").setContentText("Ready for your next set?").setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER).setContentIntent(open).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(REQUEST,notification)
    }
}
class RestReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){RestAlerts.finish(context)}}
