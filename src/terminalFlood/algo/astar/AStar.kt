package terminalFlood.algo.astar

import terminalFlood.algo.Greedy
import terminalFlood.game.*
import java.util.*

object AStar {
    /**
     * Computes a solution for the given [GameBoard] using an A* algorithm with the given strategy. Returns an array
     * containing said solution.
     *
     * This method trades in some performance in exchange for less memory needed by only caching a certain amount of
     * [GameState]s for the priority queue. If a node is checked whose game state is not cached, the game state is
     * recomputed.
     *
     * As an additional memory saving scheme, this method will discard some of the highest priority nodes (high is
     * worse) when the queue size reaches the cutoff value denoted by [queueMaxSizeCutoff]. This operation can result
     * in worse solutions being found than normally possible with a given heuristic. If you prefer to not use this
     * memory saving scheme, use [Int.MAX_VALUE] as the [queueMaxSizeCutoff] parameter value.
     */
    fun calculateMoves(
        gameBoard: GameBoard,
        strategy: AStarStrategies = AStarStrategies.INADMISSIBLE_FAST,
        queueMaxSizeCutoff: Int = Int.MAX_VALUE
    ): ColorArray = calculateSolution(GameExternalMoveList(gameBoard), MoveCollection(), strategy, queueMaxSizeCutoff)

    /**
     * Computes a solution for the given partially solved [Game] using an A* algorithm with the given strategy. Returns
     * an array containing the already played moves and the computed solution.
     *
     * This method trades in some performance in exchange for less memory needed by only caching a certain amount of
     * [GameState]s for the priority queue. If a node is checked whose game state is not cached, the game state is
     * recomputed.
     *
     * As an additional memory saving scheme, this method will discard some of the highest priority nodes (high is
     * worse) when the queue size reaches the cutoff value denoted by [queueMaxSizeCutoff]. This operation can result
     * in worse solutions being found than normally possible with a given heuristic. If you prefer to not use this
     * memory saving scheme, use [Int.MAX_VALUE] as the [queueMaxSizeCutoff] parameter value.
     */
    fun calculateMoves(
        game: Game,
        strategy: AStarStrategies = AStarStrategies.INADMISSIBLE_FAST,
        queueMaxSizeCutoff: Int = Int.MAX_VALUE
    ): ColorArray {
        if (game.amountOfMovesMade == 0)
            return calculateMoves(game.gameBoard, strategy, queueMaxSizeCutoff)

        val movesCollection = MoveCollection()
        var lastIndex = MoveCollection.NO_MOVE_INDEX
        game.playedMoves.forEach {
            lastIndex = movesCollection.addMoveEntry(lastIndex, it)
        }
        val initialGame = GameExternalMoveList(
            game.gameBoard,
            game.filled,
            game.neighbors,
            game.notFilledNotNeighbors,
            game.sensibleMoves,
            game.amountOfMovesMade,
            game.lastMove,
            lastIndex
        )

        return calculateSolution(initialGame, movesCollection, strategy, queueMaxSizeCutoff)
    }

    private fun calculateSolution(
        initialGame: GameExternalMoveList,
        movesCollection: MoveCollection,
        strategy: AStarStrategies,
        queueMaxSizeCutoff: Int
    ): ColorArray {
        val gameBoard = initialGame.gameBoard
        val heuristicStrategy = when (strategy) {
            AStarStrategies.ADMISSIBLE           -> AdmissibleStrategy(gameBoard)
            AStarStrategies.INADMISSIBLE_SLOW    -> InadmissibleSlowStrategy(gameBoard)
            AStarStrategies.INADMISSIBLE         -> InadmissibleStrategy(gameBoard)
            AStarStrategies.INADMISSIBLE_FAST    -> InadmissibleFastStrategy(gameBoard)
            AStarStrategies.INADMISSIBLE_FASTEST -> InadmissibleFastestStrategy
        }
        val movesNeededForBoardState = BoardStateHashMap(gameBoard)
        var frontier = PriorityQueue<AStarNode>()
        val gameStateCache = GameStateCache()
        // Populate the queue with all possible moves of the initial game state.
        initialGame.sensibleMoves.forEachColor {
            val newState = initialGame.makeMove(it, movesCollection.addMoveEntry(initialGame.moveEntryIndex, it))
            val priority = newState.amountOfMovesMade + heuristicStrategy.heuristic(newState)
            frontier.offer(
                AStarNode(
                    gameStateCache.addGameState(newState),
                    newState.amountOfMovesMade.toShort(),
                    newState.moveEntryIndex,
                    true,
                    priority.toShort()
                )
            )
        }

        while (frontier.isNotEmpty()) {
            val currentNode = frontier.poll()
            var currentGameState =
                getGameFromCacheOrRecompute(currentNode, gameStateCache, movesCollection, initialGame)

            if (currentGameState.isWon)
                return movesCollection.getMoveList(currentGameState.moveEntryIndex, currentGameState.amountOfMovesMade)

            // Pruning technique for inadmissible heuristics: If we can eliminate colors, we only do that. This can
            // sometimes result in slightly worse solutions, but speeds up the algorithm.
            if (strategy != AStarStrategies.ADMISSIBLE) {
                val colorEliminationMoves = ColorSet.getColorEliminations(currentGameState)
                colorEliminationMoves.forEachColor {
                    currentGameState =
                        currentGameState.makeMove(it, movesCollection.addMoveEntry(currentGameState.moveEntryIndex, it))
                }
                if (!colorEliminationMoves.isEmpty) {
                    addToQueue(
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
                addToQueue(
                    currentGameState.makeMove(it, movesCollection.addMoveEntry(currentGameState.moveEntryIndex, it)),
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
                    val game = getGameFromCacheOrRecompute(aStarNode, gameStateCache, movesCollection, initialGame)
                    nodes[i] =
                        QueueNode(
                            aStarNode,
                            (Greedy.calculateAmountOfMovesNeeded(game) + game.amountOfMovesMade).toShort()
                        )
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
        node: AStarNode,
        gameStateCache: GameStateCache,
        moveCollection: MoveCollection,
        initialGame: GameExternalMoveList
    ): GameExternalMoveList {
        val cachedGame = gameStateCache.getGameState(node.index)
        if (cachedGame != null)
            return cachedGame

        val recomputedState = initialGame.toSimpleBoardState()
        val moves = moveCollection.getMoveList(node.moveEntryIndex, node.movesPlayed.toInt())
        moves.forEach { recomputedState.makeMove(it) }

        return GameExternalMoveList(
            recomputedState.gameBoard,
            recomputedState.filled,
            recomputedState.neighbors,
            recomputedState.notFilledNotNeighbors,
            GameState.createSensibleMoveSet(recomputedState.gameBoard, recomputedState.neighbors),
            moves.size,
            moves.last,
            node.moveEntryIndex
        )
    }

    /**
     * Adds the given game state to the queue with the right cost value, but only if this was the fastest way to that
     * board state.
     */
    private fun addToQueue(
        gameState: GameExternalMoveList,
        isIslandEliminationSpecialCase: Boolean,
        movesNeededForBoardState: BoardStateHashMap,
        heuristicStrategy: Strategy,
        frontier: PriorityQueue<AStarNode>,
        gameStateCache: GameStateCache
    ) {
        val isFastestWayToBoardState = movesNeededForBoardState.putIfLess(gameState.filled, gameState.amountOfMovesMade)

        if (isFastestWayToBoardState) {
            val priority = gameState.amountOfMovesMade + heuristicStrategy.heuristic(gameState)
            frontier.offer(
                AStarNode(
                    gameStateCache.addGameState(gameState),
                    gameState.amountOfMovesMade.toShort(),
                    gameState.moveEntryIndex,
                    isIslandEliminationSpecialCase,
                    priority.toShort()
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
    private fun pruneSymmetries(gameState: GameState, isIslandEliminationSpecialCase: Boolean): ColorSet {
        if (gameState.amountOfMovesMade == 0)
            return gameState.sensibleMoves

        // Only allow a move if the previous move added new border nodes of the next move's color.
        var allowedMoves = ColorSet()
        val previousMove = gameState.lastMove
        val amountOfBits = gameState.gameBoard.amountOfNodes
        val nextMoveNeighbors = NodeSet(amountOfBits)
        gameState.sensibleMoves.forEachColor { color ->
            nextMoveNeighbors.setToNeighborsWithColor(gameState, color)
            var i = nextMoveNeighbors.nextSetBit(0, amountOfBits)
            outerLoop@ while (i >= 0) {
                val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
                var j = borderingNodes.nextSetBit(0, amountOfBits)
                while (j >= 0) {
                    if (gameState.gameBoard.getNodeWithIndex(j).color != previousMove && gameState.filled[j]) {
                        i = nextMoveNeighbors.nextSetBit(i + 1, amountOfBits)
                        continue@outerLoop
                    }
                    j = borderingNodes.nextSetBit(j + 1, amountOfBits)
                }

                allowedMoves += color
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
    private fun pruneSymmetriesAdmissible(gameState: GameState): ColorSet {
        if (gameState.amountOfMovesMade == 0)
            return gameState.sensibleMoves

        var allowedMoves = ColorSet()
        val borderNodesByColor = NodeSet(gameState.gameBoard.amountOfNodes)
        gameState.sensibleMoves.forEachColor { nextMove ->
            borderNodesByColor.setToNeighborsWithColor(gameState, nextMove)
            if (shouldPlay(gameState, nextMove, borderNodesByColor))
                allowedMoves += nextMove
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
    private fun shouldPlay(gameState: GameState, nextMove: Color, borderNodesByColor: NodeSet): Boolean {
        if (gameState.amountOfMovesMade == 0)
            return true

        // Did the previous move add any new "nextMove" border nodes?
        val previousMove = gameState.lastMove
        val amountOfBits = gameState.gameBoard.amountOfNodes
        var isNewBorderNodes = false
        var i = borderNodesByColor.nextSetBit(0, amountOfBits)
        outerLoop@ while (i >= 0) {
            val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
            var j = borderingNodes.nextSetBit(0, amountOfBits)
            while (j >= 0) {
                if (gameState.gameBoard.getNodeWithIndex(j).color != previousMove && gameState.filled[j]) {
                    i = borderNodesByColor.nextSetBit(i + 1, amountOfBits)
                    continue@outerLoop
                }
                j = borderingNodes.nextSetBit(j + 1, amountOfBits)
            }

            isNewBorderNodes = true
            break
        }

        if (!isNewBorderNodes) {
            if (nextMove.value < previousMove.value)
                return false

            // Should nextMove have been played before previousMove?
            i = borderNodesByColor.nextSetBit(0, amountOfBits)
            while (i >= 0) {
                val borderingNodes = gameState.gameBoard.getNodeWithIndex(i).borderingNodes
                var j = borderingNodes.nextSetBit(0, amountOfBits)
                while (j >= 0) {
                    if (gameState.gameBoard.getNodeWithIndex(j).color == previousMove && !gameState.filled[j]) {
                        return false
                    }
                    j = borderingNodes.nextSetBit(j + 1, amountOfBits)
                }

                i = borderNodesByColor.nextSetBit(i + 1, amountOfBits)
            }
        }

        return true
    }
}

private data class AStarNode(
    val index: Int,
    val movesPlayed: Short,
    val moveEntryIndex: Int,
    val isIslandEliminationSpecialCase: Boolean,
    val priority: Short
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

        return other.movesPlayed - movesPlayed
    }
}

/**
 * A ringbus cache for game states of the given size.
 *
 * This class is not thread-safe.
 */
private class GameStateCache(cacheSize: Int = 10000) {
    private val ringbus: Array<GameExternalMoveList?> = arrayOfNulls(cacheSize)

    private var lastUsedIndex = -1

    /**
     * Returns the cached game state associated with the given index. Will return `null` if the game state isn't in the
     * cache anymore.
     */
    fun getGameState(index: Int): GameExternalMoveList? {
        if (index <= lastUsedIndex - ringbus.size)
            return null

        return ringbus[index % ringbus.size]
    }

    /**
     * Adds the given game state to the cache and returns the index now associated with it.
     */
    fun addGameState(gameState: GameExternalMoveList): Int {
        val nextArrayIndex = ++lastUsedIndex % ringbus.size
        ringbus[nextArrayIndex] = gameState

        return lastUsedIndex
    }
}

private data class QueueNode(
    val node: AStarNode,
    val greedyPrediction: Short
) : Comparable<QueueNode> {
    override fun compareTo(other: QueueNode): Int {
        val comp = greedyPrediction - other.greedyPrediction
        if (comp != 0)
            return comp

        return node.compareTo(other.node)
    }
}