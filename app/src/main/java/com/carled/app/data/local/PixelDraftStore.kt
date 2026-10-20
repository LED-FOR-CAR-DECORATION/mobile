package com.carled.app.data.local

import android.content.Context
import com.carled.app.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

data class PixelDraft(val id: String, val name: String, val document: PixelDocument)
data class PixelPalette(val id: String, val name: String, val colors: List<Int>)

class PixelDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mirage_pixel_studio", Context.MODE_PRIVATE)
    fun drafts(): List<PixelDraft> = decodeArray(prefs.getString("drafts", "[]")!!) { item ->
        val w = item.getInt("width"); val h = item.getInt("height")
        val pixels = item.getJSONArray("pixels"); val texts = item.optJSONArray("texts") ?: JSONArray()
        PixelDraft(item.getString("id"), item.getString("name"), PixelDocument(w, h,
            List(pixels.length()) { pixels.getInt(it) }, List(texts.length()) {
                val b = texts.getJSONObject(it)
                PixelText(b.getString("id"), b.getString("text"), b.getInt("x"), b.getInt("y"), b.getInt("w"), b.getInt("h"), b.getInt("color"))
            }))
    }
    fun save(draft: PixelDraft) {
        val entries = listOf(draft) + drafts().filterNot { it.id == draft.id }
        val array = JSONArray()
        entries.forEach { d ->
            val texts = JSONArray()
            d.document.texts.forEach { b -> texts.put(JSONObject().put("id", b.id).put("text", b.text).put("x", b.x).put("y", b.y)
                .put("w", b.glyphWidth).put("h", b.glyphHeight).put("color", b.color)) }
            array.put(JSONObject().put("id", d.id).put("name", d.name).put("width", d.document.width).put("height", d.document.height)
                .put("pixels", JSONArray(d.document.pixels)).put("texts", texts))
        }
        check(prefs.edit().putString("drafts", array.toString()).commit()) { "Không thể lưu bản nháp" }
    }
    fun palettes(): List<PixelPalette> {
        val saved = prefs.getString("palettes", null) ?: return listOf(PixelPalette("mirage", "Mirage Retro",
            listOf(0xff89c89b.toInt(), 0xff111111.toInt(), 0xffffffff.toInt(), 0xfff3c7c7.toInt(), 0xffffd966.toInt(), 0xff66bbff.toInt())))
        return decodeArray(saved) { item ->
            val c = item.getJSONArray("colors")
            PixelPalette(item.getString("id"), item.getString("name"), List(c.length()) { c.getInt(it) })
        }
    }
    fun savePalettes(palettes: List<PixelPalette>) {
        val array = JSONArray()
        palettes.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("colors", JSONArray(it.colors))) }
        check(prefs.edit().putString("palettes", array.toString()).commit()) { "Không thể lưu bảng màu" }
    }
    private fun <T> decodeArray(json: String, parse: (JSONObject) -> T): List<T> {
        val array = JSONArray(json)
        return List(array.length()) { parse(array.getJSONObject(it)) }
    }
}
