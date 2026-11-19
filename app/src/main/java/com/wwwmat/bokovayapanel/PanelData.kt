package com.wwwmat.bokovayapanel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PanelItem(val label: String, val packageName: String, val isUrl: Boolean = false)
data class PanelConfig(val title: String, val items: List<PanelItem>, val color: Long = 0xEE17251F, val alpha: Int = 92)

object PanelData {
    private const val PREF = "bok_panel"
    fun count(c: Context) = c.getSharedPreferences(PREF, 0).getInt("count", 3)
    fun setCount(c: Context, n: Int) { c.getSharedPreferences(PREF, 0).edit().putInt("count", n.coerceIn(1,10)).apply() }
    fun read(c: Context, index: Int): PanelConfig {
        val p = c.getSharedPreferences(PREF, 0)
        val raw = p.getString("panel_$index", null) ?: return PanelConfig("Панель ${index+1}", emptyList())
        return try {
            val o = JSONObject(raw); val a = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until a.length()).map { val x=a.getJSONObject(it); PanelItem(x.optString("label"), x.optString("package"), x.optBoolean("url")) }
            PanelConfig(o.optString("title", "Панель ${index+1}"), items, o.optLong("color", 0xEE17251F), o.optInt("alpha",92))
        } catch (_: Exception) { PanelConfig("Панель ${index+1}", emptyList()) }
    }
    fun write(c: Context, index: Int, config: PanelConfig) {
        val a=JSONArray(); config.items.forEach { a.put(JSONObject().put("label",it.label).put("package",it.packageName).put("url",it.isUrl)) }
        val o=JSONObject().put("title",config.title).put("items",a).put("color",config.color).put("alpha",config.alpha)
        c.getSharedPreferences(PREF,0).edit().putString("panel_$index",o.toString()).apply()
    }
    fun handle(c: Context): Triple<Int,Int,Int> {
        val p=c.getSharedPreferences(PREF,0)
        return Triple(p.getInt("handleWidth",18),p.getInt("handleHeight",110),p.getInt("handleY",50))
    }
    fun saveHandle(c:Context,w:Int,h:Int,y:Int) {
        c.getSharedPreferences(PREF,0).edit().putInt("handleWidth",w).putInt("handleHeight",h).putInt("handleY",y).apply()
    }
}
