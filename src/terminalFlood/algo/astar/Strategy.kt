package terminalFlood.algo.astar

import terminalFlood.algo.Greedy
import terminalFlood.game.ColorSet
import terminalFlood.game.Game
import terminalFlood.game.NodeSet

/**
 * The heuristic used by the A* algorithm.
 *
 * Heuristics are either admissible or inadmissible. Admissible heuristics never overestimate the amount of
 * steps needed to finish a game while inadmissible ones can do so. Admissible heuristics will always find an
 * optimal solution for any given game state, while inadmissible heuristics will tend to run faster by cutting
 * down on the amount of move permutations checked by A*.
 */
interface Strategy {
    fun heuristic(gameState: Game): Int
}

/**
 * An admissible heuristic for Flood-It. Using this [Strategy] will always result in an optimal solution but is
 * relatively slow, especially with a high amount of colors.
 *
 * Idea taken from https://github.com/aaronpuchert/floodit
 *
 * To quote the explanation of the algorithm:
 *
 * Obtain a lower bound for the amount of moves left. This is done by induction: If a move fills all remaining
 * nodes of some color, it must be optimal, so we can just apply this move. Otherwise, we use a "color-blind"
 * move as it combines the effects of all possible moves. This procedure will reduce the given state until it
 * reaches the filled state.
 */
object AdmissibleStrategy : Strategy {
    override fun heuristic(gameState: Game): Int {
        var minimumMovesLeft = 0
        val currentState = SimplifiedGame(gameState)
        val notEliminatedColors = createNotEliminatedColorsSet(gameState)

        while (!currentState.isWon) {
            val colorEliminationMoves = findColorEliminations(currentState, notEliminatedColors)
            if (!colorEliminationMoves.isEmpty) {
                // Do all the moves that eliminate a color.
                currentState.makeMultiColorMove(colorEliminationMoves)
                notEliminatedColors.andNot(colorEliminationMoves)
                minimumMovesLeft += colorEliminationMoves.size
            } else {
                // If we didn't eliminate colors, do a color blind move taking all border nodes.
                currentState.makeColorBlindMove()
                minimumMovesLeft++
            }
        }

        return minimumMovesLeft
    }

    private fun createNotEliminatedColorsSet(gameState: Game): ColorSet {
        val notEliminatedColors = gameState.sensibleMoves.copy()

        gameState.gameBoard.colorSet.forEachSetBit { colorValue ->
            if (gameState.gameBoard.boardNodesByColor[colorValue].intersects(gameState.notFilledNotNeighbors))
                notEliminatedColors.set(colorValue)
        }

        return notEliminatedColors
    }

    private fun findColorEliminations(gameState: SimplifiedGame, containedColors: ColorSet): ColorSet {
        val colorEliminations = containedColors.copy()

        containedColors.forEachSetBit { colorValue ->
            if (gameState.gameBoard.boardNodesByColor[colorValue].intersects(gameState.notFilledNotNeighbors))
                colorEliminations.clear(colorValue)
        }

        return colorEliminations
    }
}

/**
 * An inadmissible heuristic for Flood-It. Using this [Strategy] will result in optimal or close to optimal
 * solutions.
 *
 * If more than half of the fields are taken, this [Strategy] will just call [AdmissibleStrategy.heuristic].
 * Otherwise, it works very similar to [AdmissibleStrategy], with the difference that instead of purely color-blind
 * moves it will only take two of the colors sorted by the amount of new border fields they give access to.
 *
 * Is substantially faster than [AdmissibleStrategy] while often giving optimal solutions. For example, for the
 * dataset of the pc19 challenge this [Strategy] didn't give an optimal solution only 10.3% of the time and was
 * only ever off by one. It also only needed between a third and a fourth of the time to complete its computations
 * compared to [AdmissibleStrategy]. Still, using this heuristic for A* with big boards and/or a high amount of
 * colors can result in relatively long runtimes.
 *
 * For the pc19 dataset see
 * [https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm]
 */
object InadmissibleSlowStrategy : Strategy {
    override fun heuristic(gameState: Game): Int {
        if (gameState.amountOfTakenFields > gameState.gameBoard.amountOfFields / 2)
            return AdmissibleStrategy.heuristic(gameState)

        var minimumMovesLeft = 0
        val newBorderNodes = NodeSet(gameState.gameBoard.amountOfNodes)
        var neighborNodes = NodeSet(gameState.gameBoard.amountOfNodes)
        var bestColorNodes = NodeSet(gameState.gameBoard.amountOfNodes)
        var secondBestColorNodes = NodeSet(gameState.gameBoard.amountOfNodes)
        val currentState = gameState.toMutableGame()

        while (!currentState.isWon) {
            val colorEliminationMoves = currentState.findAllColorEliminationMoves()
            if (!colorEliminationMoves.isEmpty) {
                // Do all the moves that eliminate a color.
                currentState.makeMultiColorMove(colorEliminationMoves)
                minimumMovesLeft += colorEliminationMoves.size
            } else {
                // If we didn't eliminate colors, take at most two of the adjacent colors. If there are more than two colors
                // present, choose the two colors that give access to the most new border fields.
                if (currentState.sensibleMoves.size < 3) {
                    currentState.makeColorBlindMove()
                    minimumMovesLeft++
                } else {
                    var amountBestColor = Int.MIN_VALUE
                    var amountSecondBestColor = Int.MIN_VALUE
                    currentState.sensibleMoves.forEachColor { color ->
                        neighborNodes.setToNeighborsWithColor(currentState, color)
                        newBorderNodes.clear()
                        neighborNodes.forEachNode(currentState.gameBoard) { node ->
                            newBorderNodes.or(node.borderingNodes)
                        }
                        newBorderNodes.and(currentState.notFilledNotNeighbors)

                        var amountOfNewFields = 0
                        newBorderNodes.forEachNode(currentState.gameBoard) { node ->
                            amountOfNewFields += node.amountOfFields
                        }

                        if (amountOfNewFields > amountBestColor) {
                            amountSecondBestColor = amountBestColor
                            amountBestColor = amountOfNewFields

                            val t = secondBestColorNodes
                            secondBestColorNodes = bestColorNodes
                            bestColorNodes = neighborNodes
                            // reuse bitset
                            neighborNodes = t
                        } else if (amountOfNewFields > amountSecondBestColor) {
                            amountSecondBestColor = amountOfNewFields

                            val t = secondBestColorNodes
                            secondBestColorNodes = neighborNodes
                            // reuse bitset
                            neighborNodes = t
                        }
                    }

                    bestColorNodes.or(secondBestColorNodes)
                    currentState.takeGivenNodes(bestColorNodes)
                    minimumMovesLeft++
                }
            }
        }

        return minimumMovesLeft
    }
}

/**
 * An inadmissible heuristic for Flood-It. This is just a variation of [InadmissibleSlowStrategy] that simply adds
 * a 13th (rounded down) to the cost to prune a bunch of paths in A* and thus make the algorithm finish faster while
 * giving slightly less optimal results.
 */
object InadmissibleStrategy : Strategy {
    override fun heuristic(gameState: Game): Int {
        val estimatedCost = InadmissibleSlowStrategy.heuristic(gameState)
        return estimatedCost + estimatedCost / 13
    }
}

/**
 * An inadmissible heuristic for Flood-It. This uses the average (rounded down) of the cost between [AdmissibleStrategy]
 * and [InadmissibleFastestStrategy] with the latter having double the weight.
 */
object InadmissibleFastStrategy : Strategy {
    override fun heuristic(gameState: Game): Int {
        val cost = AdmissibleStrategy.heuristic(gameState) + InadmissibleFastestStrategy.heuristic(gameState) * 2

        return cost / 3
    }
}

/**
 * An inadmissible heuristic for Flood-It. Using this [Strategy] will result in usually decent solutions and will
 * prune a lot of paths in A* making it very fast, even with big boards with many colors.
 *
 * Simple heuristic that just calculates a result based on a greedy algorithm. Obviously, this heuristic is
 * inadmissible as it will never underestimate and quite often overestimate the moves needed to complete the game.
 */
object InadmissibleFastestStrategy : Strategy {
    override fun heuristic(gameState: Game): Int {
        if (gameState.isWon)
            return 0

        return Greedy.calculateMoves(gameState).playedMoves.size - gameState.playedMoves.size
    }
}

enum class AStarStrategies {
    ADMISSIBLE, INADMISSIBLE_SLOW, INADMISSIBLE, INADMISSIBLE_FAST, INADMISSIBLE_FASTEST
}