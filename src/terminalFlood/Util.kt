package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.ColorArray
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
        memoryScheme: MemorySavingScheme = MemorySavingScheme.NO_SPECIAL_SCHEME
    ) {
        println("Using strategy: $strategy")
        println("Memory scheme: $memoryScheme")
        var simResult: ColorArray?
        val t = measureTimeMillis {
            simResult = when (memoryScheme) {
                MemorySavingScheme.NO_SPECIAL_SCHEME -> AStar.calculateMoves(gameBoard, strategy)
                MemorySavingScheme.QUEUE_CUTOFF      -> AStar.calculateMoves(
                    gameBoard,
                    strategy,
                    2_000_000
                )
            }
        }
        simResult?.let {
            val isFoundWin = it.size <= gameBoard.maximumSteps
            println("Time taken: ${t}ms")
            println("Winning move found: $isFoundWin")
            println("Play: $it")
            println("Amount of moves: ${it.size}")
            if (!isFoundWin)
                println("Couldn't find win within maximum amount of moves!")
        }
    }
}