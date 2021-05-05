package terminalFlood.game

/**
 * This interface defines more or less all the necessary data to represent any given Flood-It game state.
 */
interface GameState : BoardState {
    companion object {
        /**
         * Creates a [ColorSet] containing all colors of the nodes in the given [NodeSet] for the given [GameBoard].
         */
        fun createSensibleMoveSet(gameBoard: GameBoard, neighbors: NodeSet): ColorSet {
            var sensibleMoves = ColorSet()
            if (neighbors.cardinality < gameBoard.colorSet.size) {
                neighbors.forEachNode(gameBoard) { sensibleMoves += it.color }
            } else {
                gameBoard.colorSet.forEachColor { color ->
                    if (gameBoard.boardNodesByColor[color.value.toInt()].intersects(neighbors))
                        sensibleMoves += color
                }
            }

            return sensibleMoves
        }
    }

    override val isWon: Boolean
        get() = sensibleMoves.isEmpty

    /**
     * All the moves that would make sense to play in this game state. In effect this bitset contains all the distinct
     * colors of the nodes in [neighbors].
     */
    val sensibleMoves: ColorSet

    /**
     * The last move made to reach this game state.
     */
    val lastMove: Color

    /**
     * The amount of moves made to reach this game state.
     */
    val amountOfMovesMade: Int

    /**
     * Returns a [SimpleBoardState] version of this game state.
     */
    fun toSimpleBoardState(): SimpleBoardState =
        SimpleBoardState(
            gameBoard,
            filled.copy(),
            neighbors.copy(),
            notFilledNotNeighbors.copy()
        )
}