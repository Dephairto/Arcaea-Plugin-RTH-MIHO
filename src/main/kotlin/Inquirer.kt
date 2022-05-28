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

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Inquirer {
    private val client = HttpClient()

    private suspend fun getResponse(path: String, vararg params: Pair<String, Any>): HttpResponse? {
        val url = DataSystem.ApiConfig.url
        val token = DataSystem.ApiConfig.token
        return withContext(Dispatchers.Default) {
            val response: HttpResponse = client.get(url + path) {
                params.forEach { (key, value) -> parameter(key, value) }
                header("User-Agent", token)
            }
            if (response.status.value == 404) null else response
        }
    }
}