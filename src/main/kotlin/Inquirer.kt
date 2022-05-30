/*
 * Copyright © 2022 NOCPY*TEAM.
 *
 * This file is part of Arcaea-Plugin-RTH-MIHO.
 *
 * Arcaea-Plugin-RTH-MIHO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Arcaea-Plugin-RTH-MIHO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Arcaea-Plugin-RTH-MIHO. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Inquirer.kt 查询系统
 *
 * Author: Dephairto
 */

package rthmiho

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object Inquirer {
    class UserInfo(data: JsonObject) {
        val name: String = data["name"].asString
        val id: String = data["code"].asString
        val rating = data["rating"].asInt
        val character = data["character"].asInt
        val isUncapped = data["is_char_uncapped"].asBoolean == data["is_char_uncapped_override"].asBoolean
    }

    class SongInfo(data: JsonObject) {
        val name: String = data["name_en"].asString
        val bpm: String = data["bpm"].asString
        val time = data["time"].asInt.let { "%d:%2d".format(it / 60, it % 60) }
        val imageOverride = data["jacket_override"].asBoolean

        val set: String = data["set_friendly"].asString
        val worldUnlock = data["world_unlock"].asBoolean
        val remoteDownload = data["remote_download"].asBoolean
        val date: String = SimpleDateFormat("yyyy/MM/dd").format(Date(data["date"].asLong * 1000))
        val version: String = data["version"].asString

        val side = if (data["side"].asInt == 0) "光芒侧" else "纷争侧"
        val bg: String = data["bg"].asString.ifEmpty { if (side == "光芒侧") "base_light" else "base_conflict" }
        val rating = data["rating"].asInt
        val note = data["note"].asInt

        val artist: String = data["artist"].asString
        val chartDesigner: String = data["chart_designer"].asString
        val imageDesigner: String = data["jacket_designer"].asString.ifEmpty { "未知" }
    }

    class Record(data: JsonObject, val songInfo: SongInfo, val userInfo: UserInfo? = null) {
        val score = data["score"].asInt
        val rating = data["rating"].asDouble
        val songId: String = data["song_id"].asString
        val difficulty = data["difficulty"].asInt
        val timePlayed = data["time_played"].asLong
        val perfectP = data["shiny_perfect_count"].asInt
        val perfect = data["perfect_count"].asInt
        val far = data["near_count"].asInt
        val lost = data["near_count"].asInt
    }

    class B30(
        val b30Ptt: Double,
        val r10Ptt: Double,
        val userInfo: UserInfo,
        val recordList: List<Record>,
    )

    suspend fun getUserInfo(idOrUserName: String, isId: Boolean = false): UserInfo {
        val response = getResponse(
            "user/info",
            Pair(if (isId) "usercode" else "user", idOrUserName)
        ).toJson<JsonObject>()
        return checkStatus(response) { UserInfo(this["account_info"].asJsonObject) }
    }

    suspend fun getRecent(id: String): Record {
        val response = getResponse(
            "user/info",
            Pair("usercode", id),
            Pair("recent", 1),
            Pair("withsonginfo", true)
        ).toJson<JsonObject>()
        return checkStatus(response) {
            try {
                val recentData = this["recent"].asJsonArray[0].asJsonObject
                val songInfoData = this["songinfo"].asJsonArray[0].asJsonObject
                Record(recentData, SongInfo(songInfoData), UserInfo(this["account_info"].asJsonObject))
            } catch (e: IndexOutOfBoundsException) {
                throw Exception("没有recent成绩")
            }
        }
    }

    suspend fun getB30(id: String): B30 {
        val response = getResponse(
            "user/best30",
            Pair("usercode", id),
            Pair("withsonginfo", true)
        ).toJson<JsonObject>()
        return checkStatus(response) {
            B30(
                this["best30_avg"].asDouble,
                this["recent10_avg"].asDouble,
                UserInfo(this["account_info"].asJsonObject),
                this["best30_list"].asJsonArray
                    .zip(this["best30_songinfo"].asJsonArray)
                    .map { (data, songInfoData) ->
                        Record(data.asJsonObject, SongInfo(songInfoData.asJsonObject))
                    }
            )
        }
    }

    class AlertException(val innerMessage: String) : Exception("API失效")

    private fun <T> checkStatus(response: JsonObject, run: JsonObject.() -> T): T {
        when (response["status"].asInt) {
            0 -> return response["content"].asJsonObject.run()

            -1, -2, -3 -> throw Exception("账号信息未找到")
            -4 -> throw Exception("有多个重名账号")
            -5, -6, -7 -> throw Exception("未找到歌曲")
            -8 -> throw Exception("太多相关歌曲，请修改指令")
            -14 -> throw Exception("这首歌没有Beyond难度")
            -15 -> throw Exception("尚未游玩此歌曲")
            -16 -> throw Exception("查询账号被屏蔽")
            -17 -> throw Exception("查询B30失败")
            -23 -> throw Exception("未达到可查询门槛, (PTT 7.0)")
            else -> throw AlertException(response["message"].asString)
        }
    }

    private val client = HttpClient()

    private suspend fun getResponse(path: String, vararg params: Pair<String, Any>): HttpResponse {
        val url = DataSystem.PluginConfig.apiUrl
        val token = DataSystem.PluginConfig.apiToken
        return withContext(Dispatchers.Default) {
            val response: HttpResponse = client.get(url + path) {
                params.forEach { (key, value) -> parameter(key, value) }
                header("User-Agent", token)
            }
            if (response.status.value == 404)
                throw AlertException("address or token broken")
            else response
        }
    }

    private suspend inline fun <reified T> HttpResponse.toJson() =
        Gson().fromJson(this.body<String>(), T::class.java)
}