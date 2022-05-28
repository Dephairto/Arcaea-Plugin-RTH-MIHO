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

    suspend fun getUserInfo(idOrUserName: String): UserInfo? {
        val path = "user/info"
        val httpResponse = getResponse(path, Pair("user", idOrUserName)) ?: return null
        val response = Gson().fromJson(httpResponse.body<String>(), JsonObject::class.java)
        if (response["status"].asInt != 0) return null
        val data = response["content"].asJsonObject["account_info"].asJsonObject
        return UserInfo(data)
    }

    private suspend fun getResponse(path: String, vararg params: Pair<String, Any>): HttpResponse? {
        val url = DataSystem.PluginConfig.apiUrl
        val token = DataSystem.PluginConfig.apiToken
        return withContext(Dispatchers.Default) {
            val response: HttpResponse = client.get(url + path) {
                params.forEach { (key, value) -> parameter(key, value) }
                header("User-Agent", token)
            }
            if (response.status.value == 404) null else response
        }
    }
}