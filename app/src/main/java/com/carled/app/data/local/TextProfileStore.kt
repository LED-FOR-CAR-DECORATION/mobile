package com.carled.app.data.local

import android.content.Context
import com.carled.app.domain.model.*
import org.json.JSONArray
import org.json.JSONObject

class TextProfileStore(context: Context, accountId: String,
    private val plans: TextProfilePlanProvider = PendingTextProfilePlans) : TextProfileRepository {
    private val prefs = context.applicationContext.getSharedPreferences("mirage_text_profiles", Context.MODE_PRIVATE)
    private val key = "account:$accountId"
    init { require(accountId.isNotBlank()) }
    override fun plan() = plans.currentPlan()
    override fun list(): List<TextProfile> = synchronized(lock) { read() }
    private fun read(): List<TextProfile> {
        val array = JSONArray(prefs.getString(key,"[]")!!)
        return List(array.length()) { i ->
            val p=array.getJSONObject(i)
            TextProfile(p.getString("id"),p.getString("name"),p.getString("text"),p.getInt("size"),p.getInt("speed"),
                p.getString("mode"),p.getInt("r"),p.getInt("g"),p.getInt("b"),p.optBoolean("rainbow",false)).also { it.validate() }
        }
    }
    override fun save(profile: TextProfile) = synchronized(lock) {
        profile.validate()
        val profiles=read()
        require(profiles.none { it.id != profile.id && it.name.equals(profile.name,ignoreCase=true) }) { "Tên profile đã được sử dụng." }
        require(profiles.any { it.id==profile.id } || profiles.size < plan().limit) { "Đã đủ ${plan().limit} profile của gói hiện tại. Xóa bớt hoặc mở rộng gói." }
        // Updates remain available after a downgrade; no saved profile is removed automatically.
        write(listOf(profile) + profiles.filterNot { it.id==profile.id })
    }
    override fun delete(id: String) = synchronized(lock) { write(read().filterNot { it.id==id }) }
    private fun write(profiles:List<TextProfile>) {
        val array=JSONArray()
        profiles.forEach { p -> array.put(JSONObject().put("id",p.id).put("name",p.name).put("text",p.text).put("size",p.size)
            .put("speed",p.speed).put("mode",p.mode).put("r",p.r).put("g",p.g).put("b",p.b).put("rainbow",p.rainbow)) }
        check(prefs.edit().putString(key,array.toString()).commit()) { "Không thể lưu profile trên điện thoại." }
    }
    companion object { private val lock=Any() }
}
