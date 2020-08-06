package terminalFlood.game

import terminalFlood.algo.astar.SimplifiedGame

/**
 * This class represents the color of a field. This class is immutable.
 *
 * Use [Color.colorCache] to get instances of this class. Use [Color.DUMMY] if you just need a default color value.
 */
class Color private constructor(
    val value: Int
) : Comparable<Color> {
    override fun compareTo(other: Color): Int = value - other.value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Color

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value

    override fun toString(): String = value.toString(Character.MAX_RADIX)

    companion object {
        /**
         * A sort of default color. It is not a valid color value for playing the game. Use this if you don't need/want
         * to specify a specific color. For example, [SimplifiedGame.makeColorBlindMove] will use this as the played move
         * value.
         */
        val DUMMY: Color = Color(-1)

        /**
         * Use this list to get [Color] instances of the corresponding value.
         *
         * Only values below [Character.MAX_RADIX] are allowed.
         */
        val colorCache: List<Color> = (0 until Character.MAX_RADIX).map { Color(it) }

        /**
         * Returns a color with the given value.
         *
         * The Char is converted to an Int using [Character.MAX_RADIX] as the radix. If the Char cannot be converted to
         * an Int or if the converted value is below 0 an exception will be thrown.
         *
         * @see [String.toInt]
         */
        fun fromValue(value: Char): Color {
            val colorValue = java.lang.String.valueOf(value).toInt(Character.MAX_RADIX)
            return colorCache[colorValue]
        }
    }
}