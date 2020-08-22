package terminalFlood.algo.astar

import terminalFlood.algo.Greedy
import terminalFlood.game.*
import java.util.*

object AStar {
    /**
     * Computes a solution for the given [GameBoard] using an A* algorithm with the given strategy. The returned [Game]
     * instance is the game in its computed solved state. For example, use [Game.playedMoves] if you are interested
     * in the move list necessary to get to this solution.
     */
    fun calculateMoves(
        gameBoard: GameBoard,
        strategy: AStarStrategies = AStarStrategies.INADMISSIBLE_FAST
    ): Game {
        val noMaxStepsGameBoard = gameBoard.noMaximumStepsLimitCopy()
        val heuristicStrategy = when (strategy) {
            AStarStrategies.ADMISSIBLE           -> AdmissibleStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_SLOW    -> InadmissibleSlowStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE         -> InadmissibleStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_FAST    -> InadmissibleFastStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_FASTEST -> InadmissibleFastestStrategy
        }
        val colorEliminationMoves = ColorSet()
        val movesNeededForBoardState = BoardStateHashMap(noMaxStepsGameBoard)
        val frontier = PriorityQueue<AStarNode>()
        frontier.offer(AStarNode(Game(noMaxStepsGameBoard), false, 0))

        while (frontier.isNotEmpty()) {
            val currentNode = frontier.poll()
            var currentGameState = currentNode.gameState

            if (currentGameState.isWon)
                return currentGameState

            // Pruning technique for inadmissible heuristics: If we can eliminate colors, we only do that. This can
            // sometimes result in slightly worse solutions, but speeds up the algorithm.
            if (strategy != AStarStrategies.ADMISSIBLE) {
                colorEliminationMoves.setToColorEliminations(currentGameState)
                colorEliminationMoves.forEachColor {
                    currentGameState = currentGameState.makeMove(it)
                }
                if (!colorEliminationMoves.isEmpty) {
                    addToQueue(currentGameState, true, movesNeededForBoardState, heuristicStrategy, frontier)
                    continue
                }
            }

            // Continue with A* but prune the possible moves first.
            val nextMoves =
                if (strategy != AStarStrategies.ADMISSIBLE)
                    pruneSymmetries(currentGameState, currentNode.isIslandEliminationSpecialCase)
                else
                    pruneSymmetriesAdmissible(currentGameState)
            nextMoves.forEachColor {
                addToQueue(currentGameState.makeMove(it), false, movesNeededForBoardState, heuristicStrategy, frontier)
            }
        }

        error("Algorithm error!")
    }

    /**
     * Adds the given game state to the queue with the right cost value, but only if this was the fastest way to that
     * board state.
     */
    private fun addToQueue(
        gameState: Game,
        isIslandEliminationSpecialCase: Boolean,
        movesNeededForBoardState: BoardStateHashMap,
        heuristicStrategy: Strategy,
        frontier: PriorityQueue<AStarNode>
    ) {
        val isFastestWayToBoardState = movesNeededForBoardState.putIfLess(gameState.filled, gameState.playedMoves.size)

        if (isFastestWayToBoardState) {
            val priority = gameState.playedMoves.size + heuristicStrategy.heuristic(gameState)
            frontier.offer(AStarNode(gameState, isIslandEliminationSpecialCase, priority))
        }
    }

    /**
     * Computes a solution for the given [GameBoard] using an A* algorithm with the given strategy. The returned [Game]
     * instance is the game in its computed solved state. For example, use [Game.playedMoves] if you are interested
     * in the move list necessary to get to this solution.
     *
     * This method trades in some performance in exchange for less memory needed by only caching a certain amount of
     * [Game]s for the priority queue. If a node is checked whose game state is not cached, the game state is
     * recomputed.
     *
     * As an additional memory saving scheme, this method will discard some of the highest priority nodes (high is
     * worse) when the queue size reaches the cutoff value denoted by [queueMaxSizeCutoff]. This operation can result
     * in worse solutions being found than normally possible with a given heuristic. If you prefer to not use this
     * memory saving scheme, use [Int.MAX_VALUE] as the [queueMaxSizeCutoff] parameter value.
     */
    fun calculateMovesLessMemory(
        gameBoard: GameBoard,
        strategy: AStarStrategies = AStarStrategies.INADMISSIBLE_FAST,
        queueMaxSizeCutoff: Int = Int.MAX_VALUE
    ): Game {
        val noMaxStepsGameBoard = gameBoard.noMaximumStepsLimitCopy()
        val heuristicStrategy = when (strategy) {
            AStarStrategies.ADMISSIBLE           -> AdmissibleStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_SLOW    -> InadmissibleSlowStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE         -> InadmissibleStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_FAST    -> InadmissibleFastStrategy(noMaxStepsGameBoard)
            AStarStrategies.INADMISSIBLE_FASTEST -> InadmissibleFastestStrategy
        }
        val colorEliminationMoves = ColorSet()
        val movesNeededForBoardState = BoardStateHashMap(noMaxStepsGameBoard)
        var frontier = PriorityQueue<AStarNodeLessMemory>()
        val gameStateCache = GameStateCache()
        val initialGame = Game(noMaxStepsGameBoard)
        frontier.offer(AStarNodeLessMemory(gameStateCache.addGameState(initialGame), initialGame.playedMoves, false, 0))

        while (frontier.isNotEmpty()) {
            val currentNode = frontier.poll()
            var currentGameState = getGameFromCacheOrRecompute(currentNode, gameStateCache, initialGame)

            if (currentGameState.isWon)
                return currentGameState

            // Pruning technique for inadmissible heuristics: If we can eliminate colors, we only do that. This can
            // sometimes result in slightly worse solutions, but speeds up the algorithm.
            if (strategy != AStarStrategies.ADMISSIBLE) {
                colorEliminationMoves.setToColorEliminations(currentGameState)
                colorEliminationMoves.forEachColor {
                    currentGameState = currentGameState.makeMove(it)
                }
                if (!colorEliminationMoves.isEmpty) {
                    addToQueueLessMemory(
                        currentGameState,
                        true,
                        movesNeededForBoardState,
                        heuristicStrategy,
                        frontier,
                        gameStateCache
                    )
                    continue
                }
            }

            // Continue with A* but prune the possible moves first.
            val nextMoves =
                if (strategy != AStarStrategies.ADMISSIBLE)
                    pruneSymmetries(currentGameState, currentNode.isIslandEliminationSpecialCase)
                else
                    pruneSymmetriesAdmissible(currentGameState)
            nextMoves.forEachColor {
                addToQueueLessMemory(
                    currentGameState.makeMove(it),
                    false,
                    movesNeededForBoardState,
                    heuristicStrategy,
                    frontier,
                    gameStateCache
                )
            }

            // If the queue gets too big, run a greedy algorithm over all nodes and take the half that seems to have the
            // highest potential for good solutions. Use the nodes' priority as the tie breaker. This operation can
            // result in worse solutions being found than normally possible with a given heuristic.
            if (frontier.size > queueMaxSizeCutoff) {
                val nodes = arrayOfNulls<QueueNode>(frontier.size)
                frontier.forEachIndexed { i, aStarNode ->
                    val game = getGameFromCacheOrRecompute(aStarNode, gameStateCache, initialGame)
                    nodes[i] = QueueNode(aStarNode, Greedy.calculateAmountOfMovesNeeded(game))
                }
                frontier = PriorityQueue(queueMaxSizeCutoff + gameBoard.colorSet.size)
                nodes.sort()
                repeat(queueMaxSizeCutoff / 2) {
                    frontier.offer(nodes[it]!!.node)
                }
            }
        }

        error("Algorithm error!")
    }

    private fun getGameFromCacheOrRecompute(
        node: AStarNodeLessMemory,
        gameStateCache: GameStateCache,
        initialGame: Game
    ): Game {
        val cachedGame = gameStateCache.getGameState(node.index)
        if (cachedGame != null)
            return cachedGame

        val recomputedState = initialGame.toSimpleBoardState()
        node.playedMoves.forEach { recomputedState.makeMove(it) }

        return Game(
            recomputedState.gameBoard,
            node.playedMoves,
            recomputedState.filled,
            recomputedState.neighbors,
            recomputedState.notFilledNotNeighbors,
            Game.createSensibleMoveSet(recomputedState.gameBoard, recomputedState.neighbors)
        )
    }

    /**
     * Adds the given game state to the queue with the right cost value, but only if this was the fastest way to that
     * board state.
     */
    private fun addToQueueLessMemory(
        gameState: Game,
        isIslandEliminationSpecialCase: Boolean,
        movesNeededForBoardState: BoardStateHashMap,
        heuristicStrategy: Strategy,
        frontier: PriorityQueue<AStarNodeLessMemory>,
        gameStateCache: GameStateCache
    ) {
        val isFastestWayToBoardState = movesNeededForBoardState.putIfLess(gameState.filled, gameState.playedMoves.size)

        if (isFastestWayToBoardState) {
            val priority = gameState.playedMoves.size + heuristicStrategy.heuristic(gameState)
            frontier.offer(
                AStarNodeLessMemory(
                    gameStateCache.addGameState(gameState),
                    gameState.playedMoves,
                    isIslandEliminationSpecialCase,
                    priority
                )
            )
        }
    }

    /**
     * Symmetry breaking by pruning all potential moves that might as well happen in a previous move.
     *
     * This doesn't work with admissible heuristics. Use [pruneSymmetriesAdmissible] instead in those cases.
     */
    // TODO: Try to figure out if we can modify this to work with admissible heuristics while still pruning more than [shouldPlay] does and still guaranteeing optimal solutions.
    private fun pruneSymmetries(gameState: Game, isIslandEliminationSpecialCase: Boolean): ColorSet {
        if (gameState.playedMoves.isEmpty)
            return gameState.sensibleMoves

        // Only allow a move if the previous move added new border nodes of the next move's color.
        val allowedMoves = ColorSet()
        val previousMove = gameState.playedMoves.lastMove
        val nextMoveNeighbors = NodeSet(gameState.gameBoard.amountOfNodes)
        gameState.sensibleMoves.forEachColor { color ->
            nextMoveNeighbors.setToNeighborsWithColor(gameState, color)
            var i = nextMoveNeighbors.nextSetBit(0)
            outerLoop@ while (i >= 0) {
                val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
                var j = borderingNodes.nextSetBit(0)
                while (j >= 0) {
                    if (gameState.gameBoard.getNodeWithIndex(j).color !== previousMove && gameState.filled[j]) {
                        i = nextMoveNeighbors.nextSetBit(i + 1)
                        continue@outerLoop
                    }
                    j = borderingNodes.nextSetBit(j + 1)
                }

                allowedMoves.set(color)
                break
            }
        }

        // It's possible that a move only eliminated island nodes. If that situation needs special-case handling, all
        // possible moves are allowed moves by default.
        // For example, since color elimination moves while using inadmissible heuristics have precedence above all
        // other moves as a pruning technique, it is an example for such a special case.
        if (allowedMoves.isEmpty && isIslandEliminationSpecialCase)
            return gameState.sensibleMoves

        return allowedMoves
    }

    /**
     * Symmetry breaking by pruning all potential moves that might as well happen in a previous move.
     */
    private fun pruneSymmetriesAdmissible(gameState: Game): ColorSet {
        if (gameState.playedMoves.isEmpty)
            return gameState.sensibleMoves

        val allowedMoves = ColorSet()
        val borderNodesByColor = NodeSet(gameState.gameBoard.amountOfNodes)
        gameState.sensibleMoves.forEachColor { nextMove ->
            borderNodesByColor.setToNeighborsWithColor(gameState, nextMove)
            if (shouldPlay(gameState, nextMove, borderNodesByColor))
                allowedMoves.set(nextMove)
        }

        return allowedMoves
    }

    /**
     * Method is used to prune moves by breaking symmetries.
     *
     * Idea for this pruning technique taken from here:
     * [https://github.com/aaronpuchert/floodit] and
     * [https://github.com/smack42/ColorFill]
     *
     * To quote:
     * We try to break symmetry. A lower color move is only permitted if the last move (could have) made new nodes
     * of this color accessible. Otherwise we could just do the lower color move first.
     */
    private fun shouldPlay(gameState: Game, nextMove: Color, borderNodesByColor: NodeSet): Boolean {
        if (gameState.playedMoves.isEmpty)
            return true

        // Did the previous move add any new "nextMove" border nodes?
        val previousMove = gameState.playedMoves.lastMove
        var isNewBorderNodes = false
        var i = borderNodesByColor.nextSetBit(0)
        outerLoop@ while (i >= 0) {
            val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
            var j = borderingNodes.nextSetBit(0)
            while (j >= 0) {
                if (gameState.gameBoard.getNodeWithIndex(j).color !== previousMove && gameState.filled[j]) {
                    i = borderNodesByColor.nextSetBit(i + 1)
                    continue@outerLoop
                }
                j = borderingNodes.nextSetBit(j + 1)
            }

            isNewBorderNodes = true
            break
        }

        if (!isNewBorderNodes) {
            if (nextMove < previousMove)
                return false

            // Should nextMove have been played before previousMove?
            i = borderNodesByColor.nextSetBit(0)
            while (i >= 0) {
                val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
                var j = borderingNodes.nextSetBit(0)
                while (j >= 0) {
                    if (gameState.gameBoard.getNodeWithIndex(j).color === previousMove && !gameState.filled[j]) {
                        return false
                    }
                    j = borderingNodes.nextSetBit(j + 1)
                }

                i = borderNodesByColor.nextSetBit(i + 1)
            }
        }

        return true
    }
}

private data class AStarNode(
    val gameState: Game,
    val isIslandEliminationSpecialCase: Boolean,
    val priority: Int
) : Comparable<AStarNode> {
    /**
     * Break ties by sorting the states by amount of played moves (descending). Basically will explore nodes that should
     * be closer to finishing the board first and potentially prune a bunch of paths in A*. However, this can result in
     * less optimal solutions when using inadmissible heuristics.
     *
     * Idea for this behavior taken from here:
     * [https://movingai.com/astar.html]
     */
    override fun compareTo(other: AStarNode): Int {
        val comp = priority - other.priority
        if (comp != 0)
            return comp

        return other.gameState.playedMoves.size - gameState.playedMoves.size
    }
}

// The actual node class needs more memory than AStarNode, but it doesn't hold references to Game objects, which are far
// more expensive than an Int and a MoveList object.
private data class AStarNodeLessMemory(
    val index: Int,
    val playedMoves: MoveList,
    val isIslandEliminationSpecialCase: Boolean,
    val priority: Int
) : Comparable<AStarNodeLessMemory> {
    /**
     * Break ties by sorting the states by amount of played moves (descending). Basically will explore nodes that should
     * be closer to finishing the board first and potentially prune a bunch of paths in A*. However, this can result in
     * less optimal solutions when using inadmissible heuristics.
     *
     * Idea for this behavior taken from here:
     * [https://movingai.com/astar.html]
     */
    override fun compareTo(other: AStarNodeLessMemory): Int {
        val comp = priority - other.priority
        if (comp != 0)
            return comp

        return other.playedMoves.size - playedMoves.size
    }
}

/**
 * A ringbus cache for game states of the given size.
 *
 * This class is not thread-safe.
 */
private class GameStateCache(cacheSize: Int = 10000) {
    companion object {
        private val DUMMY_GAME = Game(GameBoard.initBoard("", 2, 2, StartPos.UPPER_LEFT))
    }

    private val ringbus: Array<Game> = Array(cacheSize) { DUMMY_GAME }

    private var lastUsedIndex = -1

    /**
     * Returns the cached game state associated with the given index. Will return `null` if the game state isn't in the
     * cache anymore.
     */
    fun getGameState(index: Int): Game? {
        if (index <= lastUsedIndex - ringbus.size)
            return null

        return ringbus[index % ringbus.size]
    }

    /**
     * Adds the given game state to the cache and returns the index now associated with it.
     */
    fun addGameState(gameState: Game): Int {
        val nextArrayIndex = ++lastUsedIndex % ringbus.size
        ringbus[nextArrayIndex] = gameState

        return lastUsedIndex
    }
}

private data class QueueNode(
    val node: AStarNodeLessMemory,
    val greedyPrediction: Int
) : Comparable<QueueNode> {
    override fun compareTo(other: QueueNode): Int {
        val comp = greedyPrediction - other.greedyPrediction
        if (comp != 0)
            return comp

        return node.compareTo(other.node)
    }
}