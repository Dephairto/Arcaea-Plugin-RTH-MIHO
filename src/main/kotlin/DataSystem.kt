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
 * DataSystem.kt - 本地数据读取存储系统
 *
 * Author: Dephairto
 */

package rthmiho

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.ResourceContainer.Companion.asResourceContainer
import javax.imageio.ImageIO

object DataSystem {
    val pluginDataConfigList = listOf(UserData, PluginConfig)

    object UserData : AutoSavePluginData("userData") {
        class UserData(
            var arcId: String,
            var lastInquiryTime: Long,
            var InquiryTimes: Int
        )

        val userData: MutableMap<Long, UserData> by value()
    }

    object PluginConfig : AutoSavePluginConfig("apiConfig") {
        val apiUrl: String by value()
        val apiToken: String by value()

        private val alertList: MutableList<Pair<String, String>> by value()
        fun setAlert(alertId: String, alertType: String) {
            alertList.add(Pair(alertId, alertType))
        }

        fun removeAlert(alertId: String, alertType: String) {
            alertList.removeIf { it == Pair(alertId, alertType) }
        }
    }

    object GameResource {
    }

    private fun getResource(src: String) =
        this::class.asResourceContainer().getResource(src)

    private fun getResourceAsStream(src: String) =
        this::class.asResourceContainer().getResourceAsStream(src)

    private fun getImageSource(src: String) = getResourceAsStream(src).use { ImageIO.read(it) }
}