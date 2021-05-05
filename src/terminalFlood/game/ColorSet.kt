package terminalFlood.game

/**
 * This class is an implementation of a set of [Color]s. Due to being a value class, this class is immutable.
 *
 * Its functionality is heavily inspired by [java.util.BitSet], with the difference being that it only implements
 * the necessary BitSet functions and uses only one single [Long] to hold all bits instead of an array of Longs like
 * BitSet does. One Long is enough because the maximum amount of supported colors is limited by [Character.MAX_RADIX],
 * which is well below the possible 64 discrete values available through a Long.
 *
 * The bit indexes and [Color]s are linked through [Color.value].
 *
 * @see Color
 */
@JvmInline
value class ColorSet(
    val bits: Long = 0L
) {
    companion object {
        // The binary representation of -1 is all ones in two's complement.
        private const val ALL_ONES_MASK: Long = -1L

        fun getNotEliminatedColors(gameState: GameState): ColorSet {
            var bits = gameState.sensibleMoves.bits

            gameState.gameBoard.colorSet.forEachSetBit { colorValue ->
                if (gameState.gameBoard.boardNodesByColor[colorValue].intersects(gameState.notFilledNotNeighbors))
                    bits = bits or (1L shl colorValue) // set the color
            }

            return ColorSet(bits)
        }

        @Suppress("NOTHING_TO_INLINE")
        inline fun getColorEliminations(gameState: GameState): ColorSet {
            return getColorEliminations(gameState, gameState.sensibleMoves)
        }

        fun getColorEliminations(boardState: BoardState, containedColors: ColorSet): ColorSet {
            var bits = containedColors.bits

            containedColors.forEachSetBit { colorValue ->
                if (boardState.gameBoard.boardNodesByColor[colorValue].intersects(boardState.notFilledNotNeighbors))
                    bits = bits and (1L shl colorValue).inv() // clear the color
            }

            return ColorSet(bits)
        }
    }

    val isEmpty: Boolean
        get() = bits == 0L

    val size: Int
        get() = bits.countOneBits()

    val maximumColorValue: Int
        get() = 63 - bits.countLeadingZeroBits()

    operator fun get(color: Color): Boolean = (bits and (1L shl color.value.toInt())) != 0L

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun contains(color: Color): Boolean = get(color)

    fun set(color: Color): ColorSet {
        val newBits = bits or (1L shl color.value.toInt())
        return ColorSet(newBits)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun plus(color: Color): ColorSet = set(color)

    fun andNot(colorSet: ColorSet): ColorSet {
        val newBits = bits and colorSet.bits.inv()
        return ColorSet(newBits)
    }

    fun nextSetBit(fromIndex: Int): Int {
        if (bits == 0L)
            return -1

        val bitsWithZerosBelowIndex = bits and (ALL_ONES_MASK shl fromIndex)
        val nextSetBitIndex = bitsWithZerosBelowIndex.countTrailingZeroBits()

        return if (nextSetBitIndex < 64) nextSetBitIndex else -1
    }

    inline fun forEachSetBit(fromIndex: Int = 0, action: (colorValue: Int) -> Unit) {
        var i = nextSetBit(fromIndex)
        while (i >= 0) {
            action(i)
            i = nextSetBit(i + 1)
        }
    }

    inline fun forEachColor(action: (color: Color) -> Unit) {
        var i = nextSetBit(0)
        while (i >= 0) {
            action(Color(i.toByte()))
            i = nextSetBit(i + 1)
        }
    }

    fun copy(): ColorSet = ColorSet(bits)

    fun toList(): List<Color> {
        if (isEmpty)
            return emptyList()

        val colorList = ArrayList<Color>(size)
        forEachColor { colorList.add(it) }

        return colorList
    }

    override fun toString(): String {
        if (isEmpty)
            return "{ }"

        val str = StringBuilder()

        str.append("{")
        var i = nextSetBit(0)
        while (i >= 0) {
            str.append(i)
            i = nextSetBit(i + 1)
            if (i >= 0)
                str.append(", ")
        }
        str.append("}")

        return str.toString()
    }
}