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
 * GameResource.kt - 游戏相关
 *
 * Author: Dephairto
 */

package rthmiho

import com.google.gson.JsonObject
import rthmiho.DataSystem.File.asImage
import rthmiho.DataSystem.File.saveImage
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*

object GameResource {
    class UserInfo(data: JsonObject) {
        val name: String = data["name"].asString
        val id: String = data["code"].asString
        val rating = data["rating"].asInt
        val character = data["character"].asInt
        val isAwakened = data["is_char_uncapped"].asBoolean == data["is_char_uncapped_override"].asBoolean
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

    val getUserInfo = Inquirer::getUserInfo

    val getSongInfo = Inquirer::getSongInfo

    val getRecord = Inquirer::getRecord

    val getRecent = Inquirer::getRecent

    val getB30 = Inquirer::getB30

    suspend fun getCharacterImage(character: Int, isAwakened: Boolean): BufferedImage {
        val path = "gameResource/characters/$character${if (isAwakened) "_a" else ""}.png"
        return DataSystem.File.getFile(path).run {
            this.asImage() ?: Inquirer.getCharacterImage(character, isAwakened)
                .apply { this@run.saveImage(this) }
        }
    }

    suspend fun getSongImage(songId: String, difficulty: String): BufferedImage {
        val path = "gameResource/songImages/${songId}_$difficulty.png"
        return DataSystem.File.getFile(path).run {
            this.asImage() ?: Inquirer.getSongImage(songId, difficulty)
                .apply { this@run.saveImage(this) }
        }
    }
}