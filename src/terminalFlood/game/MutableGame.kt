package terminalFlood.game

/**
 * This class represents any given Flood-It game state.
 *
 * This class is mutable and making moves will change its internal state while returning the MutableGame instance the
 * move method was called on. As a result, great care has to be taken when sharing instances. It is in fact recommended
 * to use [Game] instances when wanting to share a [GameState] between different parts of a program or when working
 * with threads. (use [toUnmodifiableView] to get an unmodifiable view of a MutableGame instance)
 *
 * Warning:
 * Do not modify the exposed fields, since doing so will invalidate the game state. For example, if you want to do
 * modifying operations on the [NodeSet]s, you must create copies of them.
 *
 * This class is not thread safe.
 */
class MutableGame(
    override val gameBoard: GameBoard,
    override var playedMoves: MoveList,
    override val filled: NodeSet,
    override val neighbors: NodeSet,
    override val notFilledNotNeighbors: NodeSet,
    override val sensibleMoves: ColorSet
) : GameState {
    private val cachedBitset = NodeSet(gameBoard.amountOfNodes)

    /**
     * @see [GameState.makeMove]
     */
    override fun makeMove(move: Color): MutableGame {
        cachedBitset.setToNeighborsWithColor(this, move)
        filled.or(cachedBitset)
        playedMoves += move
        cachedBitset.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
        sensibleMoves.clear()
        if (neighbors.cardinality() < gameBoard.colorSet.size) {
            neighbors.forEachNode(gameBoard) { sensibleMoves.set(it.color) }
        } else {
            gameBoard.colorSet.forEachSetBit { colorValue ->
                if (gameBoard.boardNodesByColor[colorValue].intersects(neighbors))
                    sensibleMoves.set(colorValue)
            }
        }

        return this
    }

    /**
     * Returns an unmodifiable view of this [MutableGame].
     *
     * The returned [Game] instance will behave as any other Game instance, with the only difference that its internal
     * state is linked to this MutableGame instance, meaning any changes to this MutableGame will invalidate the
     * returned Game. If the [Game.makeMove] method is called on the unmodifiable view, that new Game instance will
     * be completely independent of the unmodifiable view and this MutableGame.
     */
    fun toUnmodifiableView(): Game =
        Game(
            gameBoard,
            playedMoves,
            filled,
            neighbors,
            notFilledNotNeighbors,
            sensibleMoves
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MutableGame

        if (playedMoves != other.playedMoves) return false
        if (filled != other.filled) return false
        if (gameBoard != other.gameBoard) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gameBoard.hashCode()
        result = 31 * result + playedMoves.hashCode()
        result = 31 * result + filled.hashCode()
        return result
    }
}