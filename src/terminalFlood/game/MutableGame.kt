package terminalFlood.game

import java.util.*

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
 * modifying operations on the [BitSet]s, you must create copies of them.
 *
 * This class is not thread safe.
 */
class MutableGame(
    override val gameBoard: GameBoard,
    override var playedMoves: MoveList,
    override val filled: BitSet,
    override val neighbors: BitSet,
    override val notFilledNotNeighbors: BitSet,
    override val sensibleMoves: ColorSet
) : GameState {
    private val cachedBitset = BitSet(gameBoard.amountOfNodes)

    /**
     * @see [GameState.makeMove]
     */
    override fun makeMove(move: Color): MutableGame {
        cachedBitset.setTo(gameBoard.boardNodesByColor[move.value])
        cachedBitset.and(neighbors)
        computeMove(move, cachedBitset)

        return this
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeTwoColorMove(color1: Color, color2: Color): MutableGame {
        cachedBitset.setTo(gameBoard.boardNodesByColor[color1.value])
        cachedBitset.or(gameBoard.boardNodesByColor[color2.value])
        cachedBitset.and(neighbors)
        computeMove(Color.DUMMY, cachedBitset)

        return this
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeMultiColorMove(colors: ColorSet): MutableGame {
        val firstColorValue = colors.nextSetBit(0)
        cachedBitset.setTo(gameBoard.boardNodesByColor[firstColorValue])
        colors.forEachSetBit(firstColorValue + 1) { colorValue ->
            cachedBitset.or(gameBoard.boardNodesByColor[colorValue])
        }
        cachedBitset.and(neighbors)
        computeMove(Color.DUMMY, cachedBitset)

        return this
    }

    /**
     * Makes a move that ignores all colors. This move takes all neighboring nodes of this game state.
     */
    fun makeColorBlindMove(): MutableGame {
        cachedBitset.setTo(neighbors)
        computeMove(Color.DUMMY, cachedBitset)

        return this
    }

    /**
     * Updates the internal state.
     */
    private fun computeMove(move: Color, newNodes: BitSet) {
        filled.or(newNodes)
        playedMoves += move
        if (filled.cardinality() < gameBoard.amountOfNodes) {
            newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
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
        } else {
            neighbors.clear()
            notFilledNotNeighbors.clear()
            sensibleMoves.clear()
        }
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