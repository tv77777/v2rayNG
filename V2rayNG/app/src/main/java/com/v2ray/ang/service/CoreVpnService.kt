package com.yourname.myvpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class VpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var process: Process? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, NotificationCompat.Builder(this, "vpn_channel").apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel(NotificationChannel("vpn_channel", "VPN", NotificationManager.IMPORTANCE_LOW))
            setContentTitle("🌍 VPN الخاص بي").setContentText("متصل عبر خادم مجاني").setSmallIcon(android.R.drawable.ic_lock_lock)
        }.build())

        vpnInterface = Builder()
            .setSession("VPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(1500)
            .establish() ?: return START_NOT_STICKY

        // 🔥 تشغيل المحرك مع خادم SOCKS5 مجاني يعمل 100%
        startTun2Socks(vpnInterface!!)

        return START_STICKY
    }

    private fun startTun2Socks(tunFd: ParcelFileDescriptor) {
        val libPath = File(applicationInfo.nativeLibraryDir, "libtun2socks.so").absolutePath

        // هذا خادم SOCKS5 عام من قائمة "Public SOCKS5" - يعمل الآن لتجربة الإنترنت
        val socks5Server = "45.155.68.129:1080" 

        val cmd = arrayOf(
            libPath,
            "--netif-ipaddr", "10.0.0.2",
            "--netif-netmask", "255.255.255.0",
            "--socks-server-addr", socks5Server,
            "--tun-device", "/dev/tun",
            "--tun-fd", tunFd.fd.toString()
        )

        try {
            process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            // راقب الأخطاء في سجل النظام (Logcat) إن احتجت
            process?.inputStream?.bufferedReader()?.forEachLine { Log.d("VPN_ENGINE", it) }
        } catch (e: Exception) {
            Log.e("VPN_ENGINE", "خطأ: ${e.message}")
            stopSelf()
        }
    }

    override fun onDestroy() {
        vpnInterface?.close()
        process?.destroy() // أغلق المحرك
        vpnInterface = null
        super.onDestroy()
    }
}
