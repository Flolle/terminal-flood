package terminalFlood.game

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * This class represents a game board.
 *
 * No methods or accessors of this class modify its internal state. As a result, instances can be freely shared.
 *
 * Warning:
 * Do not modify the exposed bitmaps or [Array]s, since doing so will invalidate the board state. If you want to do
 * modifying operations on those collections, you must create copies of them.
 *
 * This class is thread safe.
 *
 * @param boardNodes The [BoardNode]s of the game board.
 * @param boardNodesByColor The nodes of the game board grouped by color. [Color.value] is used as the array index. [BoardNode.id] is used as the bit index.
 * @param boardSize The size of the game board. Game boards are always squares, so a game board contains `boardSize*boardSize` fields.
 * @param colorSet The collection of all colors used on the game board.
 * @param startPos The start position of the game board.
 * @param maximumSteps The maximum amount of moves allowed to finish the game.
 * @throws IllegalArgumentException if [maximumSteps] is equal or below 0; if size of [colorSet] is below 2 or equal or above [Character.MAX_RADIX]
 */
class GameBoard(
    private val boardNodes: Array<BoardNode>,
    val boardNodesByColor: Array<NodeSet>,
    val boardSize: Int,
    val colorSet: ColorSet,
    val startPos: Point,
    val maximumSteps: Int
) {
    init {
        if (maximumSteps < 1)
            throw IllegalArgumentException("The maximum amount of steps must be above 0!")
        if (colorSet.size < 2)
            throw IllegalArgumentException("The amount of colors must be at least 2!")
        if (colorSet.size >= Character.MAX_RADIX)
            throw IllegalArgumentException("The amount of colors must be below ${Character.MAX_RADIX}!")
    }

    /**
     * The highest [Color.value] contained in [colorSet].
     */
    val maximumColorValue: Int = colorSet.maximumColorValue

    /**
     * The total amount of [BoardNode]s in this game board.
     */
    val amountOfNodes: Int = boardNodes.size

    /**
     * The total amount of fields contained within this game board.
     */
    val amountOfFields: Int = boardSize * boardSize

    private val nodeLookupTable: Array<Array<BoardNode>> = calculateLookupTable()

    private fun calculateLookupTable(): Array<Array<BoardNode>> {
        val lookupTable = Array(boardSize) { arrayOfNulls<BoardNode>(boardSize) }

        for (node in boardNodes) {
            for (field in node.occupiedFields) {
                lookupTable[field.x][field.y] = node
            }
        }

        @Suppress("UNCHECKED_CAST")
        return lookupTable as Array<Array<BoardNode>>
    }

    /**
     * Returns the [BoardNode] with the corresponding index value as its [BoardNode.id].
     */
    fun getNodeWithIndex(index: Int): BoardNode = boardNodes[index]

    /**
     * Returns the node present at the given field position.
     */
    fun getNodeAtPosition(x: Int, y: Int): BoardNode = nodeLookupTable[x][y]

    /**
     * Creates a compact string from this [GameBoard].
     *
     * Compact strings are defined as holding the whole game board in one single line with no whitespace characters
     * in them. The board size is derived from the length of the string. (`boardSize = sqrt(string.length)`)
     *
     * @see createBoardFromCompactString
     */
    fun createCompactString(): String {
        val str = StringBuilder(boardSize * boardSize)

        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                str.append(nodeLookupTable[x][y].color)
            }
        }

        return str.toString()
    }

    /**
     * Returns a copy of this [GameBoard] with the only difference being that its [maximumSteps] value is [Int.MAX_VALUE].
     */
    fun noMaximumStepsLimitCopy(): GameBoard = withMaximumStepsLimitCopy(Int.MAX_VALUE)

    /**
     * Returns a copy of this [GameBoard] with the only difference being that its [maximumSteps] is set to the given
     * value.
     */
    fun withMaximumStepsLimitCopy(maximumSteps: Int = 30 * (boardSize * colorSet.size) / 100): GameBoard =
        GameBoard(
            boardNodes,
            boardNodesByColor,
            boardSize,
            colorSet,
            startPos,
            maximumSteps
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameBoard

        if (boardSize != other.boardSize) return false
        if (startPos != other.startPos) return false
        if (maximumSteps != other.maximumSteps) return false
        if (!boardNodes.contentEquals(other.boardNodes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = boardNodes.hashCode()
        result = 31 * result + boardSize
        result = 31 * result + startPos.hashCode()
        result = 31 * result + maximumSteps
        return result
    }

    companion object {
        /**
         * Crates a [GameBoard] from a compact string.
         *
         * Compact strings are defined as holding the whole game board in one single line with no whitespace characters
         * in them. The board size is derived from the length of the string. (`boardSize = sqrt(string.length)`)
         *
         * @param boardString The compact string holding the board.
         * @param startPos The start position of the board.
         * @param maxSteps The maximum amount of moves this board should be finished in.
         *
         * @see createCompactString
         */
        fun createBoardFromCompactString(
            boardString: String,
            startPos: StartPos,
            maxSteps: Int
        ): GameBoard {
            val boardSize = sqrt(boardString.length.toDouble()).toInt()
            val colorBoard = Array(boardSize) { ByteArray(boardSize) }
            var colors = ColorSet()

            for (i in boardString.indices) {
                val column = i % boardSize
                val row = i / boardSize

                val fieldColor = boardString[i].digitToInt(Character.MAX_RADIX).toByte()

                colorBoard[column][row] = fieldColor
                colors += Color(fieldColor)
            }

            return initBoard(colorBoard, boardSize, colors.size, startPos, maxSteps)
        }

        fun initBoard(
            seed: String,
            boardSize: Int,
            numberOfColors: Int,
            startPos: StartPos,
            maximumSteps: Int = 30 * (boardSize * numberOfColors) / 100
        ): GameBoard =
            initBoard(
                initColorBoard(seed.hashCode(), boardSize, numberOfColors),
                boardSize,
                numberOfColors,
                startPos,
                maximumSteps
            )

        fun initBoard(
            colorBoard: Array<ByteArray>,
            boardSize: Int,
            numberOfColors: Int,
            startPos: StartPos,
            maximumSteps: Int = 30 * (boardSize * numberOfColors) / 100
        ): GameBoard {
            var colors = ColorSet()
            val checkedFields = Array(boardSize) { BooleanArray(boardSize) }
            val boardNodes = ArrayList<BoardNodeImpl>()

            for (y in 0 until boardSize) {
                for (x in 0 until boardSize) {
                    if (!checkedFields[x][y]) {
                        val color = Color(colorBoard[x][y])
                        val nodeFields = ArrayList<Point>()

                        exploreColorNode(nodeFields, colorBoard, checkedFields, color, x, y)

                        colors += color
                        boardNodes.add(BoardNodeImpl(color, nodeFields))
                    }
                }
            }
            boardNodes.forEachIndexed { i, node -> node.id = i }

            val maximumColorValue = colors.maximumColorValue
            val boardNodesByColor = Array(maximumColorValue + 1) { NodeSet(boardNodes.size) }
            for (node in boardNodes) {
                boardNodesByColor[node.color.value.toInt()].set(node.id)
            }

            val startPoint = when (startPos) {
                StartPos.UPPER_LEFT  -> Point(0, 0)
                StartPos.UPPER_RIGHT -> Point(boardSize - 1, 0)
                StartPos.LOWER_LEFT  -> Point(0, boardSize - 1)
                StartPos.LOWER_RIGHT -> Point(boardSize - 1, boardSize - 1)
                StartPos.MIDDLE      -> Point(boardSize / 2, boardSize / 2)
            }
            val gameBoard =
                GameBoard(boardNodes.toTypedArray(), boardNodesByColor, boardSize, colors, startPoint, maximumSteps)

            for (node in boardNodes) {
                val neighbors = NodeSet(gameBoard.amountOfNodes)
                for (field in node.occupiedFields) {
                    if (field.x > 0) {
                        val borderingNode = gameBoard.nodeLookupTable[field.x - 1][field.y]
                        if (borderingNode !== node)
                            neighbors.set(borderingNode.id)
                    }
                    if (field.y > 0) {
                        val borderingNode = gameBoard.nodeLookupTable[field.x][field.y - 1]
                        if (borderingNode !== node)
                            neighbors.set(borderingNode.id)
                    }
                    if (field.x < colorBoard.size - 1) {
                        val borderingNode = gameBoard.nodeLookupTable[field.x + 1][field.y]
                        if (borderingNode !== node)
                            neighbors.set(borderingNode.id)
                    }
                    if (field.y < colorBoard.size - 1) {
                        val borderingNode = gameBoard.nodeLookupTable[field.x][field.y + 1]
                        if (borderingNode !== node)
                            neighbors.set(borderingNode.id)
                    }
                }
                node.borderingNodes = neighbors
            }

            return gameBoard
        }

        private fun initColorBoard(seed: Int, boardSize: Int, numberOfColors: Int): Array<ByteArray> {
            val board = Array(boardSize) { ByteArray(boardSize) }
            val rng = Random(seed)

            for (y in 0 until boardSize) {
                for (x in 0 until boardSize) {
                    board[x][y] = (rng.nextInt(numberOfColors) + 1).toByte()
                }
            }

            return board
        }

        private fun exploreColorNode(
            fields: MutableList<Point>,
            colorBoard: Array<ByteArray>,
            checkedFields: Array<BooleanArray>,
            color: Color,
            x: Int,
            y: Int
        ) {
            if (checkedFields[x][y] || colorBoard[x][y] != color.value)
                return

            checkedFields[x][y] = true
            fields.add(Point(x, y))
            if (x > 0)
                exploreColorNode(fields, colorBoard, checkedFields, color, x - 1, y)
            if (y > 0)
                exploreColorNode(fields, colorBoard, checkedFields, color, x, y - 1)
            if (x < colorBoard.size - 1)
                exploreColorNode(fields, colorBoard, checkedFields, color, x + 1, y)
            if (y < colorBoard.size - 1)
                exploreColorNode(fields, colorBoard, checkedFields, color, x, y + 1)
        }
    }
}

/**
 * We only need a private implementation of the [BoardNode] interface because board nodes are only created once when
 * a [GameBoard] gets created and are *not* to be modified again afterwards.
 */
private data class BoardNodeImpl(
    override val color: Color,
    override val occupiedFields: List<Point>
) : BoardNode {
    override var borderingNodes: NodeSet = NodeSet.EMPTY

    override var id: Int = -1

    override fun toString(): String = "BoardNode(color=$color,occupiedFields=$occupiedFields)"
}

data class Point(
    val x: Int,
    val y: Int
) : Comparable<Point> {
    override fun compareTo(other: Point): Int {
        val comp = y - other.y
        if (comp != 0)
            return comp

        return x - other.x
    }
}

enum class StartPos {
    UPPER_LEFT, UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT, MIDDLE
}