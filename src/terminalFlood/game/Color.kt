package terminalFlood.game

/**
 * This value class represents the color of a field.
 *
 * Use [Color.NO_COLOR] if you just need a default color value.
 */
@JvmInline
value class Color(
    val value: Byte
) {
    override fun toString(): String = value.toString(Character.MAX_RADIX)

    companion object {
        /**
         * A sort of default color. It is not a valid color value for playing the game. Use this if you don't need/want
         * to specify a specific color.
         */
        val NO_COLOR: Color = Color(-1)

        /**
         * Returns a color with the given value.
         *
         * The Char is converted to an Int using [Character.MAX_RADIX] as the radix. If the Char cannot be converted to
         * an Int or if the converted value is below 0 an exception will be thrown.
         *
         * @see [Char.digitToInt]
         */
        fun fromValue(value: Char): Color {
            val colorValue = value.digitToInt(Character.MAX_RADIX)
            return Color(colorValue.toByte())
        }
    }
}

/**
 * A helper class for [Color] that is an efficient implementation of an array of colors. Just using `Array<Color>`
 * would result in all Color values being boxed.
 */
@JvmInline
value class ColorArray(
    val colorValues: ByteArray
) {
    val size: Int
        get() = colorValues.size

    val last: Color
        get() = Color(colorValues[colorValues.size - 1])

    operator fun get(index: Int): Color = Color(colorValues[index])

    inline fun forEach(action: (color: Color) -> Unit) {
        for (colorValue in colorValues)
            action(Color(colorValue))
    }

    override fun toString(): String {
        if (size == 0)
            return "[]"

        val str = StringBuilder()
        str.append("[")
        forEach { str.append(it).append(", ") }
        str.delete(str.length - 2, str.length) // remove last comma and space
        str.append("]")

        return str.toString()
    }

    companion object {
        val EMPTY: ColorArray = ColorArray(ByteArray(0))
    }
}