package terminalFlood.game

/**
 * This is a simple implementation of [BoardState] that contains the bare minimum functionality necessary to have better
 * performance characteristics.
 *
 * This class shouldn't be used to actually play the game.
 *
 * This class doesn't implement hashCode and equals. This class is not thread safe.
 */
class SimpleBoardState(
    override val gameBoard: GameBoard,
    override val filled: NodeSet,
    override val neighbors: NodeSet,
    override val notFilledNotNeighbors: NodeSet
) : BoardState {
    private val cachedBitset = NodeSet(gameBoard.amountOfNodes)

    /**
     * Creates an empty [SimpleBoardState] instance with the given [GameBoard].
     */
    constructor(gameBoard: GameBoard) : this(
        gameBoard,
        NodeSet(gameBoard.amountOfNodes),
        NodeSet(gameBoard.amountOfNodes),
        NodeSet(gameBoard.amountOfNodes)
    )

    /**
     * Sets the state of this SimpleBoardState to the state of the given [BoardState].
     *
     * Please note that this SimpleBoardState and the given [BoardState] have to be based on the [GameBoard], otherwise
     * this method may throw an exception or this SimpleBoardState will end up in an invalid state.
     */
    fun setToState(gameState: BoardState) {
        filled.setTo(gameState.filled)
        neighbors.setTo(gameState.neighbors)
        notFilledNotNeighbors.setTo(gameState.notFilledNotNeighbors)
    }

    /**
     * Makes a move that takes all neighboring nodes of the given color.
     */
    fun makeMove(color: Color) {
        cachedBitset.setToNeighborsWithColor(this, color)
        computeMove(cachedBitset)
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeMultiColorMove(colorSet: ColorSet) {
        val firstColorValue = colorSet.nextSetBit(0)
        cachedBitset.setTo(gameBoard.boardNodesByColor[firstColorValue])
        colorSet.forEachSetBit(firstColorValue + 1) { colorValue ->
            cachedBitset.or(gameBoard.boardNodesByColor[colorValue])
        }
        cachedBitset.and(neighbors)
        computeMove(cachedBitset)
    }

    /**
     * Makes a move that ignores all colors. This move takes all neighboring nodes of this game state.
     */
    fun makeColorBlindMove() {
        cachedBitset.setTo(neighbors)
        computeMove(cachedBitset)
    }

    /**
     * Makes a move that takes the given nodes.
     */
    fun takeGivenNodes(nodes: NodeSet) {
        computeMove(nodes)
    }

    /**
     * Updates the internal state.
     */
    private fun computeMove(newNodes: NodeSet) {
        filled.or(newNodes)
        if (filled.cardinality < gameBoard.amountOfNodes) {
            newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
            neighbors.andNot(filled)
            notFilledNotNeighbors.andNot(neighbors)
        } else {
            neighbors.clear()
            notFilledNotNeighbors.clear()
        }
    }
}