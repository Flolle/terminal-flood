package terminalFlood.game

/**
 * This interface defines all the necessary data to represent any given Flood-It board state.
 *
 * Note that this interface does not define enough data to properly play a game of Flood-It, use [Game] instead for
 * those cases.
 */
interface BoardState {
    /**
     * The game board of this board state.
     */
    val gameBoard: GameBoard

    /**
     * The nodes already taken over.
     *
     * [BoardNode]s and this bitset are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val filled: NodeSet

    /**
     * The nodes bordering [filled].
     *
     * [BoardNode]s and this bitset are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val neighbors: NodeSet

    /**
     * The nodes that are neither in [filled] nor in [neighbors]. Basically all not taken nodes that don't directly
     * border the filled nodes.
     *
     * [BoardNode]s and this bitset are linked through [BoardNode.id], meaning the id is used as the bit index.
     */
    val notFilledNotNeighbors: NodeSet

    /**
     * Returns true if the board is in a won state. Boards are considered won if there are no more bordering nodes.
     */
    val isWon: Boolean
        get() = neighbors.isEmpty

    /**
     * The amount of fields already filled.
     *
     * Please note that this property is computed on every call and has O(n) runtime, where n is the amount of nodes in
     * [filled].
     */
    val amountOfTakenFields: Int
        get() {
            var amountOfTakenFields = 0
            filled.forEachNode(gameBoard) {
                amountOfTakenFields += it.amountOfFields
            }

            return amountOfTakenFields
        }
}