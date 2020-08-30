package terminalFlood.algo.astar

import terminalFlood.game.BoardNode
import terminalFlood.game.BoardState
import terminalFlood.game.GameBoard
import terminalFlood.game.NodeSet

/**
 * This class is a minimal implementation of a hashtable, with only the use case of board state lookups in mind as
 * [AStar] uses them to cut down on the generation of queue elements.
 *
 * This hashtable is implemented using open addressing with linear probing and uses fibonacci hashing together with
 * [NodeSet.wordsToHash].
 */
class BoardStateHashMap(gameBoard: GameBoard) {
    companion object {
        private const val INITIAL_POWER_OF_TWO: Int = 14

        private const val INITIAL_SIZE: Int = 1 shl INITIAL_POWER_OF_TWO // 16384

        // Relatively high load factor, but based on tests seems to give better performance than 0.75 or even 0.85
        private const val LOAD_FACTOR: Double = 0.9

        // 64 bit representation of 2^64 / golden_ratio
        private const val LONG_PHI: Long = -7046029254386353131

        // If a value in the value bucket is 0, there hasn't been a key set for that index.
        private const val NO_KEY_SET_VALUE: Short = 0
    }

    /**
     * This field holds the amount of long values necessary to hold the contents of any given instance of
     * [BoardState.filled].
     *
     * See [NodeSet], [BoardNode] and [BoardState] if you need more details.
     */
    // This looks a bit silly, but this way we can have the constructor only require a GameBoard to create a new map.
    private val keySize: Int = gameBoard.boardNodesByColor[gameBoard.colorSet.nextSetBit(0)].words.size

    // Key bucket. One bucket is the size of keySize long values. Size should always be a multiple of a power of two.
    private var keys: LongArray = LongArray(keySize * INITIAL_SIZE)

    // Value bucket. Size should always be a power of two.
    // Short is big enough to hold all practical values we'll see while computing solutions.
    // Byte is sadly too small as it could run into problems with e.g. boards of size 80x80 and upwards and 6 colors,
    // for which solutions can be found pretty quickly with InadmissibleFastestStrategy.
    private var values: ShortArray = ShortArray(INITIAL_SIZE)

    // The power of two the current size of key and value buckets is based on.
    private var currentPowerOfTwo: Int = INITIAL_POWER_OF_TWO

    private var loadThreshold: Int = (INITIAL_SIZE * LOAD_FACTOR).toInt()

    /**
     * The amount of entries in this map.
     */
    var size: Int = 0
        private set

    /**
     * If the given value is the lowest value for the given board state, it will be put into the map.
     *
     * Returns true if the value for the given board state was set. That either means that the given board state was
     * not yet in the map or the given value was lower than the previously associated value.
     */
    fun putIfLess(boardState: NodeSet, value: Int): Boolean {
        val key = boardState.words
        var index = generateArrayIndex(key, 0, keySize)
        var oldValue = values[index]
        while (oldValue != NO_KEY_SET_VALUE) {
            if (isKeyEqualToSetKey(key, index * keySize))
                break

            if (++index == values.size)
                index = 0
            oldValue = values[index]
        }

        if (oldValue == NO_KEY_SET_VALUE) {
            val keyIndex = index * keySize
            for (i in 0 until keySize)
                keys[keyIndex + i] = key[i]
            values[index] = value.toShort()

            if (++size > loadThreshold)
                increaseSize()

            return true
        }

        if (value < oldValue) {
            values[index] = value.toShort()
            return true
        }

        return false
    }

    /**
     * Returns true if the given key values are equal to the key values at the given bucket in this map.
     */
    private fun isKeyEqualToSetKey(key: LongArray, keyIndex: Int): Boolean {
        for (i in 0 until keySize) {
            if (key[i] != keys[keyIndex + i])
                return false
        }

        return true
    }

    /**
     * Generates a hash from the given key values with [NodeSet.wordsToHash] and then uses fibonacci hashing to generate
     * the array indexes.
     *
     * Fibonacci hashing was chosen due to its algorithmic simplicity and good performance. This does not mean that
     * there aren't possibly better performing hashing algorithms that could be used.
     *
     * See https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/
     */
    private fun generateArrayIndex(key: LongArray, startIndex: Int, endIndex: Int): Int {
        val h = NodeSet.wordsToHash(key, startIndex, endIndex)
        // Fibonacci hashing
        return ((h * LONG_PHI) ushr (64 - currentPowerOfTwo)).toInt()
    }

    /**
     * Doubles the size of the buckets and rehashes all entries.
     */
    private fun increaseSize() {
        val newSize = 1 shl ++currentPowerOfTwo
        val newKeys = LongArray(keySize * newSize)
        val newValues = ShortArray(newSize)
        loadThreshold = (newSize * LOAD_FACTOR).toInt()

        values.forEachIndexed { index, value ->
            if (value != NO_KEY_SET_VALUE) {
                val keyIndex = index * keySize
                var newIndex = generateArrayIndex(keys, keyIndex, keyIndex + keySize)
                while (newValues[newIndex] != NO_KEY_SET_VALUE) {
                    if (++newIndex == newValues.size)
                        newIndex = 0
                }
                val newKeyIndex = newIndex * keySize
                for (i in 0 until keySize)
                    newKeys[newKeyIndex + i] = keys[keyIndex + i]
                newValues[newIndex] = value
            }
        }

        keys = newKeys
        values = newValues
    }
}