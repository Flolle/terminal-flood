package terminalFlood.game

/**
 * This class represents any given Flood-It game state.
 *
 * No methods or accessors of this class modify its internal state and making moves will create new [Game] objects.
 * As a result, instances can be freely shared. If you do not need to share or remember previous game states, you can
 * use [MutableGame] instead to take advantage of its better performance characteristics. Use [toMutableGame] to
 * create [MutableGame] instances from [Game] objects.
 *
 * Warning:
 * Do not modify the exposed [NodeSet]s, since doing so will invalidate the game state. If you want to do modifying
 * operations on those collections, you must create copies of them.
 *
 * This class is thread safe.
 */
class Game(
    override val gameBoard: GameBoard,
    override val playedMoves: MoveList,
    override val filled: NodeSet,
    override val neighbors: NodeSet,
    override val notFilledNotNeighbors: NodeSet,
    override val sensibleMoves: ColorSet
) : GameState {
    /**
     * @see [GameState.makeMove]
     */
    override fun makeMove(move: Color): Game {
        val newNodes = gameBoard.boardNodesByColor[move.value].copy()
        newNodes.and(neighbors)
        val newFilled = filled.copy()
        val newNeighbors = neighbors.copy()
        newFilled.or(newNodes)
        newNodes.forEachNode(gameBoard) { newNeighbors.or(it.borderingNodes) }
        newNeighbors.andNot(newFilled)
        val newNotFilledNotNeighbors = notFilledNotNeighbors.copy()
        newNotFilledNotNeighbors.andNot(newNeighbors)
        val newSensibleMoves = ColorSet()
        if (newNeighbors.cardinality < gameBoard.colorSet.size) {
            newNeighbors.forEachNode(gameBoard) { newSensibleMoves.set(it.color) }
        } else {
            gameBoard.colorSet.forEachSetBit { colorValue ->
                if (gameBoard.boardNodesByColor[colorValue].intersects(newNeighbors))
                    newSensibleMoves.set(colorValue)
            }
        }

        return Game(
            gameBoard,
            playedMoves + move,
            newFilled,
            newNeighbors,
            newNotFilledNotNeighbors,
            newSensibleMoves
        )
    }

    /**
     * Returns a mutable version of this game state.
     */
    fun toMutableGame(): MutableGame =
        MutableGame(
            gameBoard,
            playedMoves,
            filled.copy(),
            neighbors.copy(),
            notFilledNotNeighbors.copy(),
            sensibleMoves.copy()
        )

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
            val sensibleMoves = ColorSet()
            if (neighbors.cardinality < gameBoard.colorSet.size) {
                neighbors.forEachNode(gameBoard) { sensibleMoves.set(it.color) }
            } else {
                gameBoard.colorSet.forEachSetBit { colorValue ->
                    if (gameBoard.boardNodesByColor[colorValue].intersects(neighbors))
                        sensibleMoves.set(colorValue)
                }
            }

            return Game(
                gameBoard,
                MoveList.emptyMoveList(),
                filled,
                neighbors,
                notFilledNotNeighbors,
                sensibleMoves
            )
        }
    }
}