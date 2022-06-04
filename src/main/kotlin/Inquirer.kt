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
import com.google.gson.JsonSyntaxException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rthmiho.GameResource.B30
import rthmiho.GameResource.Record
import rthmiho.GameResource.SongInfo
import rthmiho.GameResource.UserInfo
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

object Inquirer {
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

    suspend fun getRecord(id: String, songName: String, difficulty: String): Record {
        val response = getResponse(
            "user/best",
            Pair("usercode", id),
            Pair("songname", songName),
            Pair("difficultly", difficulty)
        ).toJson<JsonObject>()
        return checkStatus(response) { Record(this["record"].asJsonObject, SongInfo(this["song_info"].asJsonObject)) }
    }

    suspend fun getSongInfo(songName: String, difficulty: String): SongInfo {
        val response = getResponse("song/info", Pair("songname", songName)).toJson<JsonObject>()
        return checkStatus(response) {
            val data = this["difficulties"].asJsonArray.toList()
                .getOrNull(listOf("pst", "prs", "ftr", "byd").indexOf(difficulty))
                ?: throw Exception("这首歌没有Beyond难度")
            SongInfo(data.asJsonObject)
        }
    }

    suspend fun getCharacterImage(character: Int, isAwakened: Boolean): BufferedImage {
        val response = getResponse(
            "assets/char",
            Pair("partner", character),
            Pair("awakened", isAwakened)
        )
        try {
            throw AlertException(response.toJson<JsonObject>()["message"].asString)
        } catch (e: JsonSyntaxException) {
            return response.toImage()
        }
    }

    suspend fun getSongImage(songId: String, difficulty: String): BufferedImage {
        val response = getResponse(
            "assets/song",
            Pair("songid", songId),
            Pair("difficulty", difficulty)
        )
        try {
            throw AlertException(response.toJson<JsonObject>()["message"].asString)
        } catch (e: JsonSyntaxException) {
            return response.toImage()
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
        Gson().fromJson(body<String>(), T::class.java)

    private suspend fun HttpResponse.toImage() =
        withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(this@toImage.body<ByteArray>()))
        }
}