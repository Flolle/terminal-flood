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
    override var playedMoves: MoveList<Color>,
    override val filled: BitSet,
    override val neighbors: BitSet,
    override val notFilledNotNeighbors: BitSet,
    override val sensibleMoves: ColorSet
) : GameState {
    /**
     * @see [GameState.makeMove]
     */
    override fun makeMove(move: Color): MutableGame {
        val newNodes = gameBoard.boardNodesByColor[move.value].clone() as BitSet
        newNodes.and(neighbors)
        computeMove(move, newNodes)

        return this
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeTwoColorMove(color1: Color, color2: Color): MutableGame {
        val newNodes = gameBoard.boardNodesByColor[color1.value].clone() as BitSet
        newNodes.or(gameBoard.boardNodesByColor[color2.value])
        newNodes.and(neighbors)
        computeMove(Color.DUMMY, newNodes)

        return this
    }

    /**
     * Makes a move that takes all neighboring nodes of the given colors.
     */
    fun makeMultiColorMove(colors: ColorSet): MutableGame {
        val firstColorValue = colors.nextSetBit(0)
        val newNodes = gameBoard.boardNodesByColor[firstColorValue].clone() as BitSet
        colors.forEachSetBit(firstColorValue + 1) { colorValue ->
            newNodes.or(gameBoard.boardNodesByColor[colorValue])
        }
        newNodes.and(neighbors)
        computeMove(Color.DUMMY, newNodes)

        return this
    }

    /**
     * Makes a move that ignores all colors. This move takes all neighboring nodes of this game state.
     */
    fun makeColorBlindMove(): MutableGame {
        computeMove(Color.DUMMY, neighbors.clone() as BitSet)

        return this
    }

    /**
     * Updates the internal state.
     */
    private fun computeMove(move: Color, newNodes: BitSet) {
        // Updates filled, neighbors, notFilledNotNeighbors and playedMoves
        filled.or(newNodes)
        newNodes.forEachNode(gameBoard) { neighbors.or(it.borderingNodes) }
        neighbors.andNot(filled)
        notFilledNotNeighbors.andNot(neighbors)
        playedMoves += move

        // Updates sensibleMoves
        sensibleMoves.clear()
        neighbors.forEachNode(gameBoard) { sensibleMoves.set(it.color.value) }
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