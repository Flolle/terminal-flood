package terminalFlood.game

/**
 * This class represents any given Flood-It game state.
 *
 * No methods or accessors of this class modify its internal state and making moves will create new
 * [GameExternalMoveList] objects. As a result, instances can be freely shared. If you do not need to share or remember
 * previous board states, you can use [SimpleBoardState] instead to take advantage of its better performance
 * characteristics. Use [toSimpleBoardState] to create [SimpleBoardState] instances from [GameExternalMoveList] objects.
 *
 * This class doesn't directly store the moves made to reach the represented board state and is intended to be used
 * together with [MoveCollection]. This is done due to performance and memory requirement considerations. If these
 * considerations are not of pressing concern for your use case, it is recommended to use [Game] instead.
 *
 * Warning:
 * Do not modify the exposed [NodeSet]s, since doing so will invalidate the game state. If you want to do modifying
 * operations on those collections, you must create copies of them.
 *
 * This class is thread safe.
 *
 * @param moveEntryIndex The entry index of the last move resulting in the current game state.
 */
class GameExternalMoveList(
    override val gameBoard: GameBoard,
    override val filled: NodeSet,
    override val neighbors: NodeSet,
    override val notFilledNotNeighbors: NodeSet,
    override val sensibleMoves: ColorSet,
    override val amountOfMovesMade: Int,
    override val lastMove: Color,
    val moveEntryIndex: Int
) : GameState {
    companion object {
        /**
         * Creates a new [GameExternalMoveList] instance based on the given [GameBoard].
         */
        operator fun invoke(gameBoard: GameBoard): GameExternalMoveList {
            val startNode = gameBoard.getNodeAtPosition(gameBoard.startPos.x, gameBoard.startPos.y)
            val filled = NodeSet(gameBoard.amountOfNodes)
            filled.set(startNode.id)
            val neighbors = startNode.borderingNodes.copy()
            val notFilledNotNeighbors = filled.copy()
            notFilledNotNeighbors.or(neighbors)
            notFilledNotNeighbors.flipAll()

            return GameExternalMoveList(
                gameBoard,
                filled,
                neighbors,
                notFilledNotNeighbors,
                GameState.createSensibleMoveSet(gameBoard, neighbors),
                0,
                Color.NO_COLOR,
                MoveCollection.NO_MOVE_INDEX
            )
        }
    }

    /**
     * Makes a move that takes all [neighbors] of the given color and returns the new state.
     */
    fun makeMove(move: Color, newMoveEntryIndex: Int): GameExternalMoveList {
        val newNodes = gameBoard.boardNodesByColor[move.value.toInt()].copy()
        newNodes.and(neighbors)
        val newFilled = filled.copy()
        val newNeighbors = neighbors.copy()
        newFilled.or(newNodes)
        newNodes.forEachNode(gameBoard) { newNeighbors.or(it.borderingNodes) }
        newNeighbors.andNot(newFilled)
        val newNotFilledNotNeighbors = notFilledNotNeighbors.copy()
        newNotFilledNotNeighbors.andNot(newNeighbors)

        return GameExternalMoveList(
            gameBoard,
            newFilled,
            newNeighbors,
            newNotFilledNotNeighbors,
            GameState.createSensibleMoveSet(gameBoard, newNeighbors),
            amountOfMovesMade + 1,
            move,
            newMoveEntryIndex
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameExternalMoveList

        if (moveEntryIndex != other.moveEntryIndex) return false
        if (gameBoard != other.gameBoard) return false
        if (filled != other.filled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gameBoard.hashCode()
        result = 31 * result + moveEntryIndex
        result = 31 * result + filled.hashCode()
        return result
    }
}