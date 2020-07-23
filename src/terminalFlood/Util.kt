package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.Game
import terminalFlood.game.GameBoard
import java.util.concurrent.ThreadLocalRandom
import kotlin.system.measureTimeMillis

object Util {
    fun strategyFromString(str: String): AStarStrategies = when (str) {
        "astar_a"    -> AStarStrategies.ADMISSIBLE
        "astar_ia"   -> AStarStrategies.INADMISSIBLE
        "astar_iaf"  -> AStarStrategies.INADMISSIBLE_FAST
        "astar_iaff" -> AStarStrategies.INADMISSIBLE_FASTEST
        "astar_ias"  -> AStarStrategies.INADMISSIBLE_SLOW
        else         -> throw IllegalArgumentException("Incorrect value for strategy!")
    }

    fun generateRandomSeed(): String {
        val seedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
        val rng = ThreadLocalRandom.current()

        val seedLength = 5 + rng.nextInt(11)
        val seed = StringBuilder(15)
        for (i in 0 until seedLength)
            seed.append(seedChars[rng.nextInt(seedChars.length)])

        return seed.toString()
    }

    fun aStarSimulation(
        gameBoard: GameBoard,
        strategy: AStarStrategies,
        memoryScheme: MemorySavingScheme = MemorySavingScheme.NO_MEMORY_SAVING
    ) {
        println("Using strategy: $strategy")
        println("Memory scheme: $memoryScheme")
        var simResult: Game? = null
        val t = measureTimeMillis {
            simResult = when (memoryScheme) {
                MemorySavingScheme.NO_MEMORY_SAVING         -> AStar.calculateMovesParallel(gameBoard, strategy)
                MemorySavingScheme.LESS_MEMORY              -> AStar.calculateMovesLessMemory(
                    gameBoard,
                    strategy,
                    Int.MAX_VALUE
                )
                MemorySavingScheme.LESS_MEMORY_QUEUE_CUTOFF -> AStar.calculateMovesLessMemory(gameBoard, strategy)
            }
        }
        simResult?.let {
            val isFoundWin = it.playedMoves.size <= gameBoard.maximumSteps
            println("Time taken: ${t}ms")
            println("Winning move found: $isFoundWin")
            println("Play: ${it.playedMoves}")
            println("Amount of moves: ${it.playedMoves.size}")
            if (!isFoundWin)
                println("Couldn't find win within maximum amount of moves!")
        }
    }
}