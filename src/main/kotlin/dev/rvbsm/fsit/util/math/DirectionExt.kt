package dev.rvbsm.fsit.util.math

import net.minecraft.util.math.Direction
import kotlin.math.floor

private val directions = arrayOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST)

// todo: this is temporary workaround for 1.21.4, should be changed
fun Float.toHorizontalDirection() = directions[floor(this / 90.0 + 0.5).toInt() and directions.lastIndex]
