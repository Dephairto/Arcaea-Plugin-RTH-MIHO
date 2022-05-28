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
 * ArcaeaPluginRTHMIHO.kt - 插件声明
 *
 * Author: Dephairto
 */

package rthmiho

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object ArcaeaPluginRTHMIHO : KotlinPlugin(
    JvmPluginDescription(
        id = "rthmiho.arcaea",
        name = "Arcaea-Plugin-RTH-MIHO",
        version = "1.0.0",
    ) {
        author("Dephairto")
        info("Miho - Arcaea 插件")
    }
) {
    override fun onEnable() {
        DataSystem.pluginDataConfigList.forEach { it.reload() }
        logger.info { "Plugin loaded" }
    }
}