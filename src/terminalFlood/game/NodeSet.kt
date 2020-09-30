package terminalFlood.game

/**
 * This is a bitmap implementation specific to [BoardNode]s.
 *
 * Its functionality is basically equivalent to [java.util.BitSet], with the difference being that it only implements
 * the necessary BitSet functions and uses a fixed-size [LongArray] to hold all bits instead of a [LongArray] of
 * variable length like BitSet does. This limitation is not a problem due to the fact that the amount of bits needed
 * is known at [GameBoard] creation time and even very large game boards of e.g. 64x64 need less than 4096 bits.
 *
 * The bit indexes and [BoardNode]s are linked through [BoardNode.id].
 *
 * @see BoardNode
 */
class NodeSet private constructor(
    val words: LongArray,
    private val amountOfBits: Int
) {
    companion object {
        private fun bitsToWords(amountOfBits: Int): Int = ((amountOfBits - 1) shr 6) + 1

        /**
         * Generates a hash value from the given long values.
         *
         * The range used is startIndex (inclusive) to endIndex (exclusive).
         */
        fun wordsToHash(words: LongArray, startIndex: Int, endIndex: Int): Long {
            var h = 0L
            var i = endIndex
            while (--i >= startIndex)
                h = java.lang.Long.rotateLeft(h xor words[i], 1)

            return h
        }
    }

    constructor(amountOfBits: Int) : this(LongArray(bitsToWords(amountOfBits)), amountOfBits)

    private val amountOfWords: Int = words.size

    /**
     * Returns true if this bitset contains no bits set to true.
     */
    val isEmpty: Boolean
        get() {
            for (word in words)
                if (word != 0L)
                    return false

            return true
        }

    /**
     * Returns the population count of this bitset. Or in simpler terms: The amount of bits set to true.
     */
    val cardinality: Int
        get() {
            var result = 0
            for (word in words)
                result += word.countOneBits()

            return result
        }

    /**
     * Returns the amount of fields occupied by the nodes within this [NodeSet].
     */
    fun amountOfFields(gameBoard: GameBoard): Int {
        var amountOfFields = 0
        forEachNode(gameBoard) { amountOfFields += it.amountOfFields }

        return amountOfFields
    }

    /**
     * Returns true if the bit at the given index is set to true.
     */
    operator fun get(index: Int): Boolean {
        val i = index shr 6 // div 64
        return (words[i] and (1L shl index)) != 0L
    }

    /**
     * Will set the bit at the given index to true.
     */
    fun set(index: Int) {
        val i = index shr 6 // div 64
        words[i] = words[i] or (1L shl index)
    }

    /**
     * Will set the bit at the given index to false.
     */
    fun clear(index: Int) {
        val i = index shr 6 // div 64
        words[i] = words[i] and (1L shl index).inv()
    }

    /**
     * Will set all bits in this bitset to false.
     */
    fun clear() {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = 0
    }

    /**
     * Will inverse this bitset.
     */
    fun flipAll() {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = words[i].inv()
    }

    /**
     * Will set this bitset to the bordering nodes of the given bitset.
     *
     * @see [BoardNode.borderingNodes]
     */
    // Assumes both NodeSets have the same internal array size.
    @Suppress("NOTHING_TO_INLINE")
    inline fun setToBorderingNodesOf(gameBoard: GameBoard, nodes: NodeSet) {
        var i = nodes.nextSetBit(0)
        if (i == -1) {
            clear()
            return
        }
        setTo(gameBoard.getNodeWithIndex(i).borderingNodes)
        i = nodes.nextSetBit(i + 1)
        while (i > 0) {
            or(gameBoard.getNodeWithIndex(i).borderingNodes)
            i = nodes.nextSetBit(i + 1)
        }
    }

    /**
     * Will set this bitset to the intersection of the [BoardState.neighbors] bitset and the [GameBoard.boardNodesByColor]
     * bitset of the given color.
     */
    // Assumes both NodeSets have the same internal array size.
    @Suppress("NOTHING_TO_INLINE")
    inline fun setToNeighborsWithColor(boardState: BoardState, color: Color) {
        setToIntersection(boardState.gameBoard.boardNodesByColor[color.value], boardState.neighbors)
    }

    /**
     * Will set this bitset to the intersection of the two given bitsets.
     */
    // Assumes both NodeSets have the same internal array size.
    fun setToIntersection(bitset1: NodeSet, bitset2: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = bitset1.words[i] and bitset2.words[i]
    }

    /**
     * Will set the bits of this bitset to the same value as the bits of the given bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun setTo(nodes: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = nodes.words[i]
    }

    /**
     * Performs a logical OR of this bitset with the given bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun or(nodes: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = words[i] or nodes.words[i]
    }

    /**
     * Performs a logical XOR of this bitset with the given bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun xor(nodes: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = words[i] xor nodes.words[i]
    }

    /**
     * Performs a logical AND of this bitset with the given bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun and(nodes: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = words[i] and nodes.words[i]
    }

    /**
     * Clears all of the bits in this bitset whose corresponding bit is set in the given bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun andNot(nodes: NodeSet) {
        var i = amountOfWords
        while (--i >= 0)
            words[i] = words[i] and nodes.words[i].inv()
    }

    /**
     * Returns true if the given bitset has any bits set to true that are also set to true in this bitset.
     */
    // Assumes both NodeSets have the same internal array size.
    fun intersects(nodes: NodeSet): Boolean {
        var i = amountOfWords
        while (--i >= 0)
            if (words[i] and nodes.words[i] != 0L)
                return true

        return false
    }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index.
     */
    fun nextSetBit(index: Int): Int {
        if (index >= amountOfBits)
            return -1

        var i = index shr 6 // div 64
        var word = words[i] shr index // skip all the bits to the right of index

        if (word != 0L)
            return index + word.countTrailingZeroBits()

        while (++i < amountOfWords) {
            word = words[i]
            if (word != 0L)
                return (i shl 6) + word.countTrailingZeroBits()
        }

        return -1
    }

    /**
     * Performs the given action on every [BoardNode] contained in this set.
     */
    inline fun forEachNode(gameBoard: GameBoard, action: (node: BoardNode) -> Unit) {
        words.forEachIndexed { i, initialWord ->
            if (initialWord != 0L) {
                var bitshift = initialWord.countTrailingZeroBits()
                var index = (i shl 6) + bitshift // array index times 64 + bitshift
                var word = initialWord ushr bitshift // shift to first 1
                while (word != 0L) {
                    action(gameBoard.getNodeWithIndex(index))

                    word = word ushr 1
                    if (word == 0L)
                        break

                    bitshift = word.countTrailingZeroBits()
                    index += bitshift + 1 // bitshift + 1 because we already shifted by 1 before
                    word = word ushr bitshift
                }
            }
        }
    }

    /**
     * Creates a copy of this bitset.
     */
    fun copy(): NodeSet = NodeSet(words.copyOf(), amountOfBits)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeSet) return false

        if (amountOfBits != other.amountOfBits) return false
        if (!words.contentEquals(other.words)) return false

        return true
    }

    override fun hashCode(): Int {
        val h = wordsToHash(words, 0, amountOfWords)
        // fold leftmost bits into right and add a constant to prevent
        // empty sets from returning 0, which is too common.
        return (h shr 32 xor h).toInt() + -0x6789edcc
    }

    override fun toString(): String {
        val str = StringBuilder()

        str.append("[")
        repeat(amountOfBits) {
            if (get(it))
                str.append("1")
            else
                str.append("0")
        }
        str.append("]")

        return str.toString()
    }
}