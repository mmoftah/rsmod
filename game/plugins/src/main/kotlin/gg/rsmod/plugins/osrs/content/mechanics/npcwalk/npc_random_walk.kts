package gg.rsmod.plugins.osrs.content.mechanics.npcwalk

import gg.rsmod.game.model.FACING_PAWN_ATTR
import gg.rsmod.game.model.TimerKey
import gg.rsmod.plugins.osrs.api.ext.npc

val SEARCH_FOR_PATH_TIMER = TimerKey()
val SEARCH_FOR_PATH_DELAY = 15..30

on_global_npc_spawn {
    val npc = it.npc()
    if (npc.walkRadius > 0) {
        npc.timers[SEARCH_FOR_PATH_TIMER] = npc.world.random(SEARCH_FOR_PATH_DELAY)
    }
}

on_timer(SEARCH_FOR_PATH_TIMER) {
    val npc = it.npc()
    val facing = npc.attr[FACING_PAWN_ATTR]?.get()

    /**
     * The npc is not facing a player, so it can walk.
     */
    if (facing == null) {
        val rx = npc.world.random(-npc.walkRadius..npc.walkRadius)
        val rz = npc.world.random(-npc.walkRadius..npc.walkRadius)

        val start = npc.spawnTile
        val dest = start.transform(rx, rz)

        npc.walkTo(dest, maxPathSize = npc.walkRadius)
    }

    npc.timers[SEARCH_FOR_PATH_TIMER] = npc.world.random(SEARCH_FOR_PATH_DELAY)
}