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

object Inquirer {
    private val client = HttpClient()

    class UserInfo(data: JsonObject) {
        val name: String = data["name"].asString
        val id: String = data["code"].asString
        val rating: Int = data["rating"].asInt
    }

    class AlertException(val innerMessage: String) : Exception("API失效")

    suspend fun getUserInfo(idOrUserName: String, isId: Boolean = false): UserInfo {
        val path = "user/info"
        val httpResponse = getResponse(path, Pair(if (isId) "usercode" else "user", idOrUserName))
        val response = Gson().fromJson(httpResponse.body<String>(), JsonObject::class.java)
        return checkStatus(response) {
            val data = response["content"].asJsonObject["account_info"].asJsonObject
            UserInfo(data)
        }
    }

    private fun <T> checkStatus(response: JsonObject, run: () -> T): T {
        when (response["status"].asInt) {
            0 -> return run()

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
}