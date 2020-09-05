package terminalFlood.game

/**
 * This class represents any given Flood-It game state.
 *
 * No methods or accessors of this class modify its internal state and making moves will create new [Game] objects.
 * As a result, instances can be freely shared. If you do not need to share or remember previous board states, you can
 * use [SimpleBoardState] instead to take advantage of its better performance characteristics. Use [toSimpleBoardState]
 * to create [SimpleBoardState] instances from [Game] objects.
 *
 * Warning:
 * Do not modify the exposed [NodeSet]s, since doing so will invalidate the game state. If you want to do modifying
 * operations on those collections, you must create copies of them.
 *
 * This class is thread safe.
 *
 * @param playedMoves The moves played so far.
 */
class Game(
    override val gameBoard: GameBoard,
    val playedMoves: MoveList,
    override val filled: NodeSet,
    override val neighbors: NodeSet,
    override val notFilledNotNeighbors: NodeSet,
    override val sensibleMoves: ColorSet
) : GameState {
    companion object {
        /**
         * Creates a new [Game] instance based on the given [GameBoard].
         */
        operator fun invoke(gameBoard: GameBoard): Game {
            val startNode = gameBoard.getNodeAtPosition(gameBoard.startPos.x, gameBoard.startPos.y)
            val filled = NodeSet(gameBoard.amountOfNodes)
            filled.set(startNode.id)
            val neighbors = startNode.borderingNodes.copy()
            val notFilledNotNeighbors = filled.copy()
            notFilledNotNeighbors.or(neighbors)
            notFilledNotNeighbors.flipAll()

            return Game(
                gameBoard,
                MoveList.emptyMoveList(),
                filled,
                neighbors,
                notFilledNotNeighbors,
                GameState.createSensibleMoveSet(gameBoard, neighbors)
            )
        }
    }

    override val lastMove: Color
        get() = playedMoves.lastMove

    override val amountOfMovesMade: Int
        get() = playedMoves.size

    /**
     * Returns true if the game is finished. Games are considered finished if they are won or if the move limit
     * is reached.
     */
    val isFinished: Boolean
        get() = playedMoves.size == gameBoard.maximumSteps || isWon

    /**
     * Makes a move that takes all [neighbors] of the given color and returns the new state.
     */
    fun makeMove(move: Color): Game {
        val newNodes = gameBoard.boardNodesByColor[move.value].copy()
        newNodes.and(neighbors)
        val newFilled = filled.copy()
        val newNeighbors = neighbors.copy()
        newFilled.or(newNodes)
        newNodes.forEachNode(gameBoard) { newNeighbors.or(it.borderingNodes) }
        newNeighbors.andNot(newFilled)
        val newNotFilledNotNeighbors = notFilledNotNeighbors.copy()
        newNotFilledNotNeighbors.andNot(newNeighbors)

        return Game(
            gameBoard,
            playedMoves + move,
            newFilled,
            newNeighbors,
            newNotFilledNotNeighbors,
            GameState.createSensibleMoveSet(gameBoard, newNeighbors)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Game

        if (playedMoves != other.playedMoves) return false
        if (gameBoard != other.gameBoard) return false
        if (filled != other.filled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gameBoard.hashCode()
        result = 31 * result + playedMoves.hashCode()
        result = 31 * result + filled.hashCode()
        return result
    }
}