/*
 * OKTW Galaxy Project
 * Copyright (C) 2018-2020
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy.command.commands

import com.mojang.brigadier.CommandDispatcher
import kotlinx.coroutines.*
import net.minecraft.client.network.packet.TitleS2CPacket
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.GameRules
import one.oktw.galaxy.command.Command
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Suppress("DuplicatedCode")
class Spawn : Command {

    private val lock = ConcurrentHashMap.newKeySet<UUID>()

    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            CommandManager.literal("spawn")
                .executes { context ->
                    execute(context.source)
                }
        )
    }

    private fun execute(source: ServerCommandSource): Int {
        val player = source.player

        if (player == null || lock.contains(player.uuid)) return com.mojang.brigadier.Command.SINGLE_SUCCESS

        lock += player.uuid
        GlobalScope.launch {
            val world = player.serverWorld
            val level = world.levelProperties
            for (i in 0..4) {
                val component = TranslatableText("Respond.commandCountdown", 5 - i)
                    .styled { style -> style.color = Formatting.GREEN }
                player.networkHandler.sendPacket(TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, component))
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val spawnRadius = level.gameRules.getInt(GameRules.SPAWN_RADIUS)

            var spawnPos: BlockPos
            do {
                spawnPos = BlockPos(
                    level.spawnX + Random.nextInt(-spawnRadius, spawnRadius),
                    level.spawnY,
                    level.spawnZ + Random.nextInt(-spawnRadius, spawnRadius)
                )
            } while (!world.doesNotCollide(Box(spawnPos)))

            withContext(player.server.asCoroutineDispatcher()) {
                player.stopRiding()
                if (player.isSleeping) {
                    player.wakeUp(true, true)
                }
                player.requestTeleport(
                    spawnPos.x.toDouble(),
                    spawnPos.y.toDouble(),
                    spawnPos.z.toDouble()
                )
            }
            lock -= player.uuid
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS
    }
}
