package com.example.livecast.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lưu danh sách cấu hình kênh live vào SharedPreferences dưới dạng JSON.
 * Không dùng thư viện ngoài (Gson) để giữ project gọn nhẹ, tự parse JSON thủ công.
 */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("livecast_profiles", Context.MODE_PRIVATE)

    fun loadAll(): MutableList<StreamProfile> {
        val raw = prefs.getString(KEY, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val list = mutableListOf<StreamProfile>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                StreamProfile(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    platform = StreamProfile.Platform.valueOf(o.getString("platform")),
                    rtmpUrl = o.getString("rtmpUrl"),
                    streamKey = o.getString("streamKey"),
                    loopVideo = o.optBoolean("loopVideo", true)
                )
            )
        }
        return list
    }

    fun saveAll(list: List<StreamProfile>) {
        val arr = JSONArray()
        list.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("platform", p.platform.name)
            o.put("rtmpUrl", p.rtmpUrl)
            o.put("streamKey", p.streamKey)
            o.put("loopVideo", p.loopVideo)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(profile: StreamProfile) {
        val list = loadAll()
        list.add(profile)
        saveAll(list)
    }

    fun delete(id: String) {
        val list = loadAll().filterNot { it.id == id }
        saveAll(list)
    }

    companion object {
        private const val KEY = "profiles_json"
    }
}
