package gg.rsmod.game.model.path.actions

import gg.rsmod.game.fs.def.ObjectDef
import gg.rsmod.game.model.*
import gg.rsmod.game.model.collision.ObjectType
import gg.rsmod.game.model.entity.Entity
import gg.rsmod.game.model.entity.GameObject
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.plugin.Plugin
import gg.rsmod.util.DataConstants

/**
 * @author Tom <rspsmods@gmail.com>
 */
object ObjectPathfinder {

    /**
     * TODO: varrock museum, basement staircase shouldn't be valid where the rope is
     * TODO: varrock anvil on east side, the entrances r being clipped for some reason
     * TODO: [getValidTiles] for other object types. i.e object type 5 needs other rotations support
     */

    val walkPlugin: (Plugin) -> Unit = {
        val p = it.player()
        val obj = p.attr[INTERACTING_OBJ_ATTR]
        val opt = p.attr[INTERACTING_OPT_ATTR]

        val validTiles = getValidTiles(p.world, obj)
        if (p.tile !in validTiles) {
            p.walkTo(obj, MovementQueue.StepType.NORMAL)
            it.suspendable {
                awaitArrival(it, obj, opt)
            }
        } else {
            it.suspendable {
                faceObj(p, obj)
                handleAction(it, obj, opt)
            }
        }
    }

    private fun handleAction(it: Plugin, obj: GameObject, opt: Int) {
        val p = it.player()

        if (!p.world.plugins.executeObject(p, obj.id, opt)) {
            p.message(Entity.NOTHING_INTERESTING_HAPPENS)
            if (p.world.gameContext.devMode) {
                p.message("Unhandled object action: [opt=$opt, id=${obj.id}, type=${obj.type}, rot=${obj.rot}, x=${obj.tile.x}, z=${obj.tile.z}]")
            }
        }
    }

    private suspend fun awaitArrival(it: Plugin, obj: GameObject, opt: Int) {
        val p = it.player()
        val destination = p.movementQueue.peekLast()
        if (destination == null) {
            p.message(Entity.YOU_CANT_REACH_THAT)
            return
        }
        while (true) {
            if (!p.tile.sameAs(destination)) {
                it.wait(1)
                continue
            }
            faceObj(p, obj)
            it.wait(1)
            if (p.tile in getValidTiles(p.world, obj)) {
                handleAction(it, obj, opt)
            } else {
                p.message(Entity.YOU_CANT_REACH_THAT)
            }
            break
        }
    }

    private fun faceObj(p: Player, obj: GameObject) {
        val def = p.world.definitions.get(ObjectDef::class.java, obj.id)
        val rot = obj.rot
        val type = obj.type

        var width = def.width
        var length = def.length

        if (rot == 1 || rot == 3) {
            width = def.length
            length = def.width
        }

        when (type) {
            ObjectType.LENGTHWISE_WALL.value -> {
                /**
                 * Doors and walls, otherwise you end up facing the same direction
                 * the object is facing.
                 */
                val dir = when (rot) {
                    0 -> obj.tile.transform(if (p.tile.x == obj.tile.x) -1 else 0, 0)
                    1 -> obj.tile.transform(0, 1)
                    2 -> obj.tile.transform(1, 0)
                    else -> obj.tile.transform(0, -1)
                }
                p.faceTile(dir)
            }
            else -> {
                p.faceTile(obj.tile.transform(width shr 1, length shr 1), width, length)
            }
        }
    }

    fun getValidTiles(world: World, obj: GameObject): Array<Tile> {
        val directions = hashSetOf<Tile>()

        val def = world.definitions.get(ObjectDef::class.java, obj.id)
        val rot = obj.rot
        val type = obj.type
        var width = def.width
        var length = def.length

        if (rot == 1 || rot == 3) {
            width = def.length
            length = def.width
        }

        val blockBits = 4
        val clipMask = def.clipFlag
        val clipFlag = (DataConstants.BIT_MASK[blockBits] and (clipMask shl rot)) or (clipMask shr (blockBits - rot))

        when (clipFlag) {
            7 -> { // West to east
                for (z in 0 until length) {
                    directions.add(obj.tile.transform(-1, z))
                }
                return directions.toTypedArray()
            }
            11 -> { // South to north.
                for (x in 0 until width) {
                    directions.add(obj.tile.transform(x, -1))
                }
                return directions.toTypedArray()
            }
            13 -> { // East to west
                for (z in 0 until length) {
                    directions.add(obj.tile.transform(width, z))
                }
                return directions.toTypedArray()
            }
            14 -> { // North to south
                for (x in 0 until width) {
                    directions.add(obj.tile.transform(x, length))
                }
                return directions.toTypedArray()
            }
        }

        if (type >= ObjectType.INTERACTABLE.value && type <= ObjectType.FLOOR_DECORATION.value) {
            /**
             * Same dimension objects can be interacted from any side.
             */
            if (width == length) {
                for (x in -1 .. width) {
                    for (z in -1 .. length) {

                        if (x == -1 && z == -1 || x == width && z == -1
                                || x == -1 && z == length || x == width && z == length) {
                            continue
                        }

                        val tile = obj.tile.transform(x, z)

                        /**
                         * We make sure the object can be reached from this [tile].
                         */
                        val objFace: Direction = when {
                            (x in 0 .. width) && z == -1 -> Direction.SOUTH
                            (x in 0 .. width) && z == length -> Direction.NORTH
                            (z in 0 .. length) && x == -1 -> Direction.WEST
                            (z in 0 .. length) && x == width -> Direction.EAST
                            else -> Direction.NONE // Inside object - can always interact
                        }
                        if (objFace == Direction.NONE || world.collision.canTraverse(tile.step(1, objFace), Direction.between(tile.step(1, objFace), tile), EntityType.PLAYER)) {
                            directions.add(tile)
                        }
                    }
                }
                return directions.toTypedArray()
            }

            /**
             * Rotation one can only be accessed from the south & north side.
             * Rotation three can only be accessed from the north side.
             */
            if (rot == 1 || rot == 3) {
                for (x in 0 until width) {
                    directions.add(obj.tile.transform(x, if (rot == 1) -1 else length))
                    if (rot == 1) {
                        directions.add(obj.tile.transform(x, length))
                    }
                }
                return directions.toTypedArray()
            }

            /**
             * Rotation zero can only be accessed from the south side.
             * Rotation two can only be accessed from the north side.
             */
            if (rot == 0 || rot == 2) {
                for (x in 0 until width) {
                    directions.add(obj.tile.transform(x, if (rot == 0) -1 else length))
                }
                return directions.toTypedArray()
            }
        } else if (type == ObjectType.LENGTHWISE_WALL.value) {
            /**
             * Doors, mainly.
             */
            return when (rot) {
                1 -> arrayOf(obj.tile.transform(-1, 0), obj.tile.transform(0, 0), obj.tile.transform(0, 1), obj.tile.transform(0, -1))
                2 -> arrayOf(obj.tile.transform(0, 0), obj.tile.transform(1, 0))
                3 -> arrayOf(obj.tile.transform(0, -1), obj.tile.transform(0, 0))
                else -> arrayOf(obj.tile.transform(0, 0), obj.tile.transform(-1, 0))
            }
        } else if (type == 5) {
            /**
             * Varrock agility course start is type 5. Look more into this type
             * to insert it into [ObjectType] properly.
             */
            return when (rot) {
                else -> arrayOf(obj.tile.transform(0, 0))
            }
        }

        return directions.toTypedArray()
    }
}