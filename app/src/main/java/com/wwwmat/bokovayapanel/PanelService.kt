package com.wwwmat.bokovayapanel

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Color as AColor
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class PanelService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private var active = 0
    private var downX=0f
    private val channel="bok_panel_service"
    override fun onCreate() {
        super.onCreate()
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        createNotification()
        showHandle()
    }
    private fun createNotification() {
        if(Build.VERSION.SDK_INT>=26) { val nm=getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel(channel,"Боковая Панель",NotificationManager.IMPORTANCE_LOW)) }
        val n=NotificationCompat.Builder(this,channel).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Боковая Панель активна").setContentText("Проведите влево от полоски у края экрана").setOngoing(true).build()
        if(Build.VERSION.SDK_INT>=34) startForeground(7,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(7,n)
    }
    private fun lp(type:Int): WindowManager.LayoutParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.RIGHT }
    private fun showHandle() {
        if(!Settings.canDrawOverlays(this)) { stopSelf(); return }
        val v=View(this); val (w,h,yPercent)=PanelData.handle(this)
        v.background=GradientDrawable().apply { setColor(0xFF36A66A.toInt()); cornerRadius=30f }
        val params=lp(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY).apply { width=(resources.displayMetrics.density*w/10).toInt().coerceAtLeast(8); height=(resources.displayMetrics.heightPixels*h/100).toInt().coerceIn(50,resources.displayMetrics.heightPixels); y=(resources.displayMetrics.heightPixels*yPercent/100) }
        v.setOnTouchListener { _,e -> when(e.action) { MotionEvent.ACTION_DOWN -> { downX=e.rawX; true }; MotionEvent.ACTION_UP -> { if(downX-e.rawX>24) openPanel() else if(panel!=null) closePanel(); true }; else -> true } }
        handle=v; wm.addView(v,params)
    }
    private fun openPanel() {
        if(panel!=null) { closePanel(); return }
        val cfg=PanelData.read(this,active.coerceIn(0,PanelData.count(this)-1))
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,16,18,16); background=GradientDrawable().apply { setColor((cfg.color and 0x00FFFFFF or ((cfg.alpha*255/100).toLong() shl 24)).toInt()); cornerRadius=28f } }
        val title=TextView(this).apply { text=cfg.title; textSize=17f; setTextColor(AColor.WHITE); setPadding(4,4,4,12) }; root.addView(title)
        cfg.items.forEach { item ->
            val b=TextView(this).apply { text="▣  ${item.label}"; textSize=16f; setTextColor(AColor.WHITE); setPadding(12,12,12,12); setOnClickListener { launch(item); closePanel() } }
            root.addView(b)
        }
        root.setOnLongClickListener { startActivity(Intent(this,MainActivity::class.java).putExtra("panel_index",active).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); closePanel(); true }
        root.setOnTouchListener(object:View.OnTouchListener {
            var sx=0f
            override fun onTouch(v:View,e:MotionEvent):Boolean { if(e.action==MotionEvent.ACTION_DOWN){sx=e.rawX;return true}; if(e.action==MotionEvent.ACTION_UP){val dx=e.rawX-sx;if(abs(dx)>50){active=(active+1)%PanelData.count(this@PanelService);closePanel();openPanel()} else if(e.rawX<resources.displayMetrics.widthPixels-240) closePanel();return true};return true }
        })
        val p=lp(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY).apply { width=resources.displayMetrics.widthPixels*3/5; height=WindowManager.LayoutParams.WRAP_CONTENT; y=resources.displayMetrics.heightPixels/5; flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN }
        panel=root; wm.addView(root,p)
    }
    private fun launch(item:PanelItem) {
        try { val i=if(item.isUrl) Intent(Intent.ACTION_VIEW,Uri.parse(item.packageName)) else packageManager.getLaunchIntentForPackage(item.packageName); i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if(i!=null) startActivity(i) } catch (_:Exception) {}
    }
    private fun closePanel() { panel?.let { try{wm.removeView(it)}catch(_:Exception){} }; panel=null }
    override fun onDestroy() { closePanel(); handle?.let{try{wm.removeView(it)}catch(_:Exception){}}; handle=null; super.onDestroy() }
    override fun onBind(intent:Intent?):IBinder?=null
}
