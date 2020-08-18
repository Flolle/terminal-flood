package terminalFlood.game

/**
 * This is a bitmap implementation specific to [Color]s.
 *
 * Its functionality is basically equivalent to [java.util.BitSet], with the difference being that it only implements
 * the necessary BitSet functions and uses only one single [Long] to hold all bits instead of an array of Longs like
 * BitSet does. One Long is enough because the maximum amount of supported colors is limited by [Character.MAX_RADIX],
 * which is well below the possible 64 discrete values available through a Long.
 *
 * The bit indexes and [Color]s are linked through [Color.value].
 *
 * @see Color
 */
class ColorSet private constructor(
    private var bits: Long
) {
    constructor() : this(0L)

    companion object {
        // The binary representation of -1 is all ones in two's complement.
        private const val ALL_ONES_MASK: Long = -1L
    }

    val isEmpty: Boolean
        get() = bits == 0L

    val size: Int
        get() = bits.countOneBits()

    val maximumColorValue: Int
        get() = 63 - bits.countLeadingZeroBits()

    operator fun get(bitIndex: Int): Boolean = (bits and (1L shl bitIndex)) != 0L

    @Suppress("NOTHING_TO_INLINE")
    inline operator fun contains(color: Color): Boolean = get(color.value)

    @Suppress("NOTHING_TO_INLINE")
    inline fun set(color: Color) {
        set(color.value)
    }

    fun set(bitIndex: Int) {
        bits = bits or (1L shl bitIndex)
    }

    fun clear(bitIndex: Int) {
        bits = bits and (1L shl bitIndex).inv()
    }

    fun clear() {
        bits = 0L
    }

    fun andNot(colorSet: ColorSet) {
        bits = bits and colorSet.bits.inv()
    }

    fun nextSetBit(fromIndex: Int): Int {
        if (bits == 0L)
            return -1

        val bitsWithZerosBelowIndex = bits and (ALL_ONES_MASK shl fromIndex)
        val nextSetBitIndex = bitsWithZerosBelowIndex.countTrailingZeroBits()

        return if (nextSetBitIndex < 64) nextSetBitIndex else -1
    }

    inline fun forEachSetBit(fromIndex: Int = 0, action: (index: Int) -> Unit) {
        var i = nextSetBit(fromIndex)
        while (i >= 0) {
            action(i)
            i = nextSetBit(i + 1)
        }
    }

    inline fun forEachColor(action: (color: Color) -> Unit) {
        var i = nextSetBit(0)
        while (i >= 0) {
            action(Color.colorCache[i])
            i = nextSetBit(i + 1)
        }
    }

    fun setToNotEliminatedColors(gameState: Game) {
        bits = gameState.sensibleMoves.bits

        gameState.gameBoard.colorSet.forEachSetBit { colorValue ->
            if (gameState.gameBoard.boardNodesByColor[colorValue].intersects(gameState.notFilledNotNeighbors))
                set(colorValue)
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun setToColorEliminations(gameState: Game) {
        setToColorEliminations(gameState, gameState.sensibleMoves)
    }

    fun setToColorEliminations(boardState: BoardState, containedColors: ColorSet) {
        bits = containedColors.bits

        containedColors.forEachSetBit { colorValue ->
            if (boardState.gameBoard.boardNodesByColor[colorValue].intersects(boardState.notFilledNotNeighbors))
                clear(colorValue)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorSet

        return bits == other.bits
    }

    // This is what java.lang.Long.hashCode(value) does.
    override fun hashCode(): Int = (bits xor (bits ushr 32)).toInt()

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