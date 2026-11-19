package com.wwwmat.bokovayapanel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selected = intent.getIntExtra("panel_index", -1)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF4CAF70))) {
            if (selected >= 0) PanelEditor(selected) { finish() } else HomeScreen()
        } }
    }
    @Composable private fun HomeScreen() {
        var count by remember { mutableIntStateOf(PanelData.count(this)) }
        var width by remember { mutableIntStateOf(PanelData.handle(this).first) }
        var height by remember { mutableIntStateOf(PanelData.handle(this).second) }
        var y by remember { mutableIntStateOf(PanelData.handle(this).third) }
        Column(Modifier.fillMaxSize().background(Color(0xFF101915)).padding(20.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Боковая Панель", style=MaterialTheme.typography.headlineMedium, color=Color.White)
            Text("Настройте панели и добавьте любимые приложения.", color=Color.LightGray)
            Text("Количество панелей: $count", color=Color.White)
            Slider(count.toFloat(), { count=it.toInt().coerceIn(1,10); PanelData.setCount(this@MainActivity,count) }, valueRange=1f..10f, steps=8)
            Text("Полоска активации", color=Color.White)
            Text("Ширина: $width%", color=Color.White); Slider(width.toFloat(),{width=it.toInt();PanelData.saveHandle(this@MainActivity,width,height,y)},valueRange=4f..40f)
            Text("Высота: $height%", color=Color.White); Slider(height.toFloat(),{height=it.toInt();PanelData.saveHandle(this@MainActivity,width,height,y)},valueRange=4f..100f)
            Text("Положение по вертикали: $y%", color=Color.White); Slider(y.toFloat(),{y=it.toInt();PanelData.saveHandle(this@MainActivity,width,height,y)},valueRange=0f..100f)
            Button(onClick={ if(!Settings.canDrawOverlays(this@MainActivity)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) else startService(Intent(this@MainActivity,PanelService::class.java)) }, modifier=Modifier.fillMaxWidth()) { Text(if(Settings.canDrawOverlays(this@MainActivity)) "Запустить боковую панель" else "Разрешить отображение поверх окон") }
            OutlinedButton(onClick={stopService(Intent(this@MainActivity,PanelService::class.java))}, modifier=Modifier.fillMaxWidth()) { Text("Остановить панель") }
            Text("Мои панели", color=Color.White, style=MaterialTheme.typography.titleLarge)
            repeat(count) { i -> Card(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@MainActivity,MainActivity::class.java).putExtra("panel_index",i)) }, colors=CardDefaults.cardColors(containerColor=Color(0xFF20352B))) {
                Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically) { Text(PanelData.read(this@MainActivity,i).title, color=Color.White, modifier=Modifier.weight(1f)); Icon(Icons.Default.ChevronRight,null,tint=Color.White) }
            } }
        }
    }
    @Composable private fun PanelEditor(index:Int, close:()->Unit) {
        var cfg by remember { mutableStateOf(PanelData.read(this,index)) }
        var title by remember { mutableStateOf(cfg.title) }
        var alpha by remember { mutableFloatStateOf(cfg.alpha.toFloat()) }
        var showApps by remember { mutableStateOf(false) }
        var showUrl by remember { mutableStateOf(false) }
        var url by remember { mutableStateOf("https://") }
        val pm=packageManager
        val apps=remember { pm.getInstalledApplications(0).filter { pm.getLaunchIntentForPackage(it.packageName)!=null && it.packageName!=packageName }.sortedBy { pm.getApplicationLabel(it).toString().lowercase() } }
        Column(Modifier.fillMaxSize().background(Color(0xFF101915)).padding(18.dp)) {
            Text("Настройка панели ${index+1}",color=Color.White,style=MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title,{title=it},label={Text("Название панели")},modifier=Modifier.fillMaxWidth())
            Text("Прозрачность: ${alpha.toInt()}%",color=Color.White); Slider(alpha,{alpha=it},valueRange=0f..100f)
            Button(onClick={showApps=true},modifier=Modifier.fillMaxWidth()) { Text("Добавить приложение") }
            OutlinedButton(onClick={showUrl=true},modifier=Modifier.fillMaxWidth()) { Text("Добавить интернет-ссылку") }
            OutlinedButton(onClick={cfg=cfg.copy(color=when(cfg.color){0xEE17251F->0xEE172034;0xEE172034->0xEE24334A;else->0xEE17251F})},modifier=Modifier.fillMaxWidth()) { Text("Изменить цвет панели") }
            Text("Ярлыки: ${cfg.items.size}/10",color=Color.White)
            LazyColumn(Modifier.weight(1f)) {
                items(cfg.items) { item -> Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) { Text(item.label,color=Color.White,modifier=Modifier.weight(1f)); IconButton(onClick={cfg=cfg.copy(items=cfg.items-item)}) { Icon(Icons.Default.Delete,null,tint=Color.White) } } }
            }
            Button(onClick={PanelData.write(this@MainActivity,index,cfg.copy(title=title,alpha=alpha.toInt()));close()},modifier=Modifier.fillMaxWidth()) { Text("Сохранить") }
            if(showApps) AlertDialog(onDismissRequest={showApps=false},title={Text("Выберите приложение")},text={
                LazyColumn(Modifier.heightIn(max=420.dp)) { items(apps) { app -> val label=pm.getApplicationLabel(app).toString()
                    Row(Modifier.fillMaxWidth().clickable { if(cfg.items.size<10 && cfg.items.none{it.packageName==app.packageName}) cfg=cfg.copy(items=cfg.items+PanelItem(label,app.packageName)); showApps=false }.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(label,modifier=Modifier.weight(1f)); if(cfg.items.any{it.packageName==app.packageName}) Icon(Icons.Default.Check,null)
                    }
                } }
            },confirmButton={TextButton(onClick={showApps=false}){Text("Готово")}})
            if(showUrl) AlertDialog(onDismissRequest={showUrl=false},title={Text("Интернет-ссылка")},text={OutlinedTextField(url,{url=it},label={Text("Адрес сайта")})},confirmButton={
                TextButton(onClick={ if(cfg.items.size<10 && url.startsWith("http")) cfg=cfg.copy(items=cfg.items+PanelItem(url,url,true)); showUrl=false }) { Text("Добавить") }
            },dismissButton={TextButton(onClick={showUrl=false}){Text("Отмена")}})
        }
    }
}
