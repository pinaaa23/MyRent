package com.myrent.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.R
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Worker pengingat lokal harian untuk mengecek transaksi sewa yang perlu di-follow up
 */
class FollowUpWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val user = auth.currentUser ?: return Result.success()

            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val cabangId = userDoc.getString("cabangId") ?: ""

            if (cabangId.isNotEmpty()) {
                val snapshot = firestore.collection("rentals")
                    .whereEqualTo("cabangId", cabangId)
                    .whereEqualTo("status", "berjalan")
                    .get()
                    .await()

                val now = System.currentTimeMillis()
                val tomorrow = now + (24 * 60 * 60 * 1000L)

                var countNeedFollowUp = 0
                snapshot.documents.forEach { doc ->
                    val tglKembali = doc.getLong("tanggalKembali") ?: 0L
                    if (isSameDay(tglKembali, now) || isSameDay(tglKembali, tomorrow)) {
                        countNeedFollowUp++
                    }
                }

                if (countNeedFollowUp > 0) {
                    showNotification(applicationContext, countNeedFollowUp)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(context: Context, count: Int) {
        val channelId = "myrent_followup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Follow-Up MyRent",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pengingat Follow-Up MyRent")
            .setContentText("Ada $count barang yang perlu di-follow up hari ini/besok.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
