package terminalFlood

import terminalFlood.algo.astar.AStar
import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    if (args.contains("-debug")) {
        debug()
        return
    }
    if (args.contains("-help") || args.contains("-h")) {
        printHelp()
        return
    }
    if (args.contains("-solutionsForDataset")) {
        val threads = args[args.indexOf("-solutionsForDataset") + 1].toInt()
        val strategy = when (args[args.indexOf("-solutionsForDataset") + 2]) {
            "astar_a"    -> AStarStrategies.ADMISSIBLE
            "astar_ia"   -> AStarStrategies.INADMISSIBLE
            "astar_iaf"  -> AStarStrategies.INADMISSIBLE_FAST
            "astar_iaff" -> AStarStrategies.INADMISSIBLE_FASTEST
            "astar_ias"  -> AStarStrategies.INADMISSIBLE_SLOW
            else         -> throw IllegalArgumentException("Incorrect value for simulation method!")
        }
        val startPos = parseStartPosition(args[args.indexOf("-solutionsForDataset") + 3])
        val file = Paths.get(args[args.indexOf("-solutionsForDataset") + 4])

        findSolutionsForDataset(file, threads, strategy, startPos)
        return
    }
    if (args.contains("-createDataset")) {
        val boardSize = args[args.indexOf("-createDataset") + 1].toInt()
        val numberOfColors = args[args.indexOf("-createDataset") + 2].toInt()
        val numberOfBoards = args[args.indexOf("-createDataset") + 3].toInt()
        val file =
            if (args.size > args.indexOf("-createDataset") + 4) {
                Paths.get(args[args.indexOf("-createDataset") + 4])
            } else {
                Paths.get(
                    System.getProperty("user.dir"),
                    "dataset b${boardSize}c${numberOfColors}n${numberOfBoards}.txt"
                )
            }

        createDataset(file, boardSize, numberOfColors, numberOfBoards)
        return
    }

    var seed = generateRandomSeed()
    var boardSize = 14
    var numberOfColors = 6
    var startPos = StartPos.UPPER_LEFT
    if (args.contains("-seed"))
        seed = args[args.indexOf("-seed") + 1]
    if (args.contains("-size"))
        boardSize = args[args.indexOf("-size") + 1].toInt()
    if (args.contains("-colors"))
        numberOfColors = args[args.indexOf("-colors") + 1].toInt()
    if (args.contains("-startPos"))
        startPos = parseStartPosition(args[args.indexOf("-startPos") + 1])

    val gameBoard = GameBoard.initBoard(seed, boardSize, numberOfColors, startPos)

    println("Initial game board:")
    for (y in 0 until gameBoard.boardSize) {
        for (x in 0 until gameBoard.boardSize) {
            print(gameBoard.getNodeAtPosition(x, y).color)
        }
        println()
    }
    println("\nSeed: $seed")
    println("\n\n#############################################################\n\n")

    when {
        args.contains("-astar_a")    -> aStarSimulation(gameBoard, AStarStrategies.ADMISSIBLE)
        args.contains("-astar_ia")   -> aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE)
        args.contains("-astar_iaf")  -> aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FAST)
        args.contains("-astar_iaff") -> aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FASTEST)
        args.contains("-astar_ias")  -> aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_SLOW)
        else                         -> playGame(gameBoard)
    }
}

private fun parseStartPosition(startPosString: String): StartPos = when (startPosString) {
    "ul" -> StartPos.UPPER_LEFT
    "ur" -> StartPos.UPPER_RIGHT
    "ll" -> StartPos.LOWER_LEFT
    "lr" -> StartPos.LOWER_RIGHT
    "m"  -> StartPos.MIDDLE
    else -> throw IllegalArgumentException("Incorrect value for startPos!")
}

fun printHelp() {
    println("This program can be used without any program arguments, they simply allow customization of the")
    println("gameplay.\n")
    println("Program arguments:")
    println("  -seed STR    \t --")
    println("\t\tThe seed used for the random number generator. The value can be any string. Random default.")
    println("  -size N      \t --")
    println("\t\tThe size of the game board (always a square). Default 14.")
    println("  -colors N    \t --")
    println("\t\tThe amount of colors used for the game. Only values between 2 and ${Character.MAX_RADIX - 1} (inclusive) are")
    println("\t\tallowed. Recommended values are between 4 and 8. Default 6.")
    println("  -startPos STR\t --")
    println("\t\tThe starting position at the beginning of the game. Can be ul for upper left corner,")
    println("\t\tur for upper right corner, ll for lower left corner, lr for lower right corner and")
    println("\t\tm for middle of the board. Default ul.\n\n")

    println("Example:")
    println("  java -jar terminal-flood.jar -seed \"xyzzy\" -size 18 -colors 6 -startPos m\n")
    println("The above command will create a game with an 18x18 board, 6 colors, starting position in the middle")
    println("and the string \"xyzzy\" used as the seed value for the RNG.\n\n\n")

    println("Solution computation arguments:")
    println("The following arguments will make the program try to find winning moves for a game board using the")
    println("specified algorithms. Only one of them can be used at the same time, and you will not be able to")
    println("play the game using these arguments. These options might be useful for you if you struggle with a")
    println("board and want to know a possible solution. You'll have to have the seed of a game board for that")
    println("though.")
    println("Usage recommendation: If you just want a solution, first try \"-astar_iaf\". If the solution is")
    println("not satisfactory, try \"-astar_ias\" or if working with big boards with lots of colors \"-astar_ia\".")
    println("  -astar_a     \t --")
    println("\t\tUses an A* algorithm. Uses an admissible heuristic, which means that it will always find")
    println("\t\tan optimal solution. Can potentially take *a lot* of time to find a solution, especially")
    println("\t\tat higher board sizes with even a moderate amount of colors.")
    println("  -astar_ias   \t --")
    println("\t\tUses an A* algorithm. Uses an inadmissible heuristic, which means it will not always find")
    println("\t\tan optimal solution. Still, the result will often be optimal or very close to optimal. Is")
    println("\t\tsubstantially faster than \"-astar_a\" but still runs into long runtimes at higher board")
    println("\t\tsizes and/or a lot of colors.")
    println("  -astar_ia    \t --")
    println("\t\tUses an A* algorithm. Uses an inadmissible heuristic, which means it will not always find")
    println("\t\tan optimal solution. Still, the result will often be close to optimal. Is substantially")
    println("\t\tfaster than \"-astar_ias\" but can still run into relatively long runtimes at high board")
    println("\t\tsizes with a lot of colors.")
    println("  -astar_iaf   \t --")
    println("\t\tUses an A* algorithm. Uses an inadmissible heuristic, which means it will not always find")
    println("\t\tan optimal solution. Will find good solutions, but usually not optimal ones. Is fast")
    println("\t\tand can be used even at high board sizes with a lot of colors. This is the recommended")
    println("\t\tcomputation option if you just want to quickly find a solution for any given game board.")
    println("  -astar_iaff  \t --")
    println("\t\tUses an A* algorithm. Uses an inadmissible heuristic, which means it will not always find")
    println("\t\tan optimal solution. Will find decent solutions, but usually not optimal ones. Is very")
    println("\t\tfast, even at high board sizes and a lot of colors.")

    println("\n\nYou can also let the program find solutions for a whole file containing compact strings of game")
    println("boards using this command:")
    println("  -solutionsForDataset [threads] [strategy] [startPos] [filepath]\n")
    println("Please note that all arguments are mandatory.")
    println("  [threads]    \t --")
    println("\t\tThe amount of boards being computed at the same time. It's generally advisable to set")
    println("\t\tthis number no higher than the amount of logical processors of your machine. Also, keep")
    println("\t\tin mind that more simultaneous computations will need more heap space, so set the")
    println("\t\t-Xmx value of the JVM accordingly. For example, 24x24 boards with the astar_ias strategy")
    println("\t\tand 4 threads may need upwards of 10GB of heap space, depending on the specific dataset.")
    println("  [strategy]   \t --")
    println("\t\tThe strategy used to compute solutions. The possible values are the same as the single")
    println("\t\tsolution computation arguments, just without the minus sign in front. See above for the")
    println("\t\tlist of options and what each option does.")
    println("  [startPos]   \t --")
    println("\t\tThe starting position of the game boards. The option is exactly the same as the argument")
    println("\t\tused when simply playing the game.")
    println("  [filepath]   \t --")
    println("\t\tThe file path to the text file containing the game boards. Please only use forward")
    println("\t\tslashes, even on Windows systems.")

    println("\nTo give an example of what a full command would look like:")
    println("  java -jar terminal-flood.jar -solutionsForDataset 4 astar_iaf ul \"/path/to/dataset.txt\"")
    println("The above command would compute solutions for the file located at \"/path/to/dataset.txt\" using")
    println("4 threads, the astar_iaf strategy and using the upper left corner as the starting position for the")
    println("game boards. The solutions would be saved to \"/path/to/solutions.txt\".")

    println("\n\nAdditionally, you can use this program to create dataset files containing compact strings of game")
    println("boards. Compact strings are defined as holding the whole game board in one single line with no")
    println("whitespace characters in them. The board size is derived from the length of the string.")
    println("(boardSize = sqrt(string.length))")
    println("\nThe command is as follows:")
    println("  -createDataset [boardSize] [numColors] [numBoards] [filepath]\n")
    println("  [boardSize]  \t --")
    println("\t\tThe size of the game boards (always a square). The option is exactly the same as the")
    println("\t\targument used when simply playing the game.")
    println("  [numColors]  \t --")
    println("\t\tThe amount of colors used for the game boards. The option is exactly the same as the")
    println("\t\targument used when simply playing the game.")
    println("  [numBoards]  \t --")
    println("\t\tThe amount of boards created for the dataset. The value must be equal or above 1.")
    println("  [filepath]   \t --")
    println("\t\tThe file path denoting the location plus name of the dataset to be saved. Please only use")
    println("\t\tforward slashes, even on Windows systems. This option is optional and if left out, a file")
    println("\t\tnamed \"dataset b[boardSize]c[numColors]n[numBoards].txt\" will be saved in the current")
    println("\t\tworking directory.")

    println("\nTo give an example of what a full command would look like:")
    println("  java -jar terminal-flood.jar -createDataset 14 6 1000 \"/path/to/dataset.txt\"")
    println("The above command would create a dataset containing 1000 boards of the size 14x14 with 6 colors.")
    println("The dataset would be saved to \"/path/to/dataset.txt\".")
}

private fun generateRandomSeed(): String {
    val seedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"
    val rng = ThreadLocalRandom.current()

    val seedLength = 5 + rng.nextInt(11)
    val seed = StringBuilder(15)
    for (i in 0 until seedLength)
        seed.append(seedChars[rng.nextInt(seedChars.length)])

    return seed.toString()
}

fun aStarSimulation(gameBoard: GameBoard, strategy: AStarStrategies) {
    println("AStar $strategy")
    var simResult: Game? = null
    val t = measureTimeMillis {
        simResult = AStar.calculateMovesParallel(gameBoard, strategy)
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

fun playGame(gameBoard: GameBoard) {
    println("Thank you for playing terminal-flood!\n")
    println("You start in one of the corners or the middle of the board, every field you own is denoted")
    println("by an \"@\". Not taken fields consist of groups with the same color denoted by a number.")
    println("Every turn you choose a color and every bordering group of that color will be taken over.")
    println("You win the game by taking over the whole board within the allotted number of moves.\n\n")

    val colorMaxValue = gameBoard.maximumColorValue.toString(Character.MAX_RADIX)
    val gameStack = Stack<Game>()
    gameStack.push(Game(gameBoard))

    while (!gameStack.peek().isFinished) {
        printCurrentState(gameStack.peek())

        print("\nNext move (enter character between 1-$colorMaxValue")
        if (gameStack.size > 1)
            print(" or undo):")
        else
            print("):")

        val input = readLine() ?: error("Couldn't read input!")
        val inputStr = input.trim()

        if (inputStr == "undo") {
            if (gameStack.size == 1) {
                println("No moves played. Cannot undo!")
            } else {
                println("Undoing last move.")
                gameStack.pop()
            }
        } else {
            try {
                if (inputStr.length > 1)
                    throw NumberFormatException()
                if (java.lang.String.valueOf(inputStr[0]).toInt(Character.MAX_RADIX) > gameBoard.maximumColorValue)
                    throw IllegalArgumentException()

                val currentGame = gameStack.peek()
                val move = Color.fromValue(inputStr[0])
                if (currentGame.sensibleMoves[move.value])
                    gameStack.push(currentGame.makeMove(move))
                else
                    println("Move doesn't do anything!")
            } catch (ex: NumberFormatException) {
                println("Only characters between 1-$colorMaxValue and undo are valid inputs!")
            } catch (ex: IllegalArgumentException) {
                println("The specified move is outside the color range of this game! (range: 1-$colorMaxValue)")
            }
        }

        println("\n#############################################################\n\n")
    }

    printCurrentState(gameStack.peek())

    if (gameStack.peek().isWon)
        println("\nYou've won the game!")
    else
        println("\nYou've lost! Better luck next time.")
}

fun printCurrentState(gameState: GameState) {
    println("Game board:\n")
    for (y in 0 until gameState.gameBoard.boardSize) {
        for (x in 0 until gameState.gameBoard.boardSize) {
            val node = gameState.gameBoard.getNodeAtPosition(x, y)
            if (gameState.filled[node.id])
                print("@")
            else
                print(node.color)
        }
        println()
    }
    println("\nPlayed moves: ${gameState.playedMoves}")
    println("Number of moves left: ${gameState.gameBoard.maximumSteps - gameState.playedMoves.size}")
}

fun createDataset(file: Path, boardSize: Int, numberOfColors: Int, numberOfBoards: Int) {
    if (Files.isDirectory(file))
        throw IllegalArgumentException("The file is a directory!")

    if (Files.exists(file))
        Files.delete(file)

    println("Creating dataset.")
    println("Board dimensions: ${boardSize}x$boardSize")
    println("Number of colors: $numberOfColors")
    println("Number of boards to be created: $numberOfBoards")

    Files.newBufferedWriter(file, Charsets.UTF_8).use { outputWriter ->
        for (i in 0 until numberOfBoards) {
            // Writing random numbers directly to the file would be more efficient, but this way we have automatic
            // validation of the boards and this isn't really performance critical code unless you want to create
            // a million or more boards.
            val seed = generateRandomSeed()
            val gameBoard = GameBoard.initBoard(
                seed = seed,
                boardSize = boardSize,
                numberOfColors = numberOfColors,
                startPos = StartPos.UPPER_LEFT // Doesn't matter
            )

            outputWriter.appendln(gameBoard.createCompactString())

            val index = i + 1
            if (index % 100 == 0)
                println("$index boards finished")
        }
    }

    println("Dataset created and saved to: $file")
}

fun findSolutionsForDataset(file: Path, threads: Int, strategy: AStarStrategies, startPos: StartPos) {
    if (!Files.exists(file) || Files.isDirectory(file))
        throw IllegalArgumentException("The file doesn't exist or is a directory!")

    println("Finding solutions for the given dataset: $file")
    println("Number of threads used: $threads")
    println("Start position on the game boards: $startPos")
    println("Using strategy: $strategy")

    val executor = Executors.newFixedThreadPool(threads)
    val promisedGames = ArrayList<Future<GameResult>>(100_000)
    val index = AtomicInteger(0)
    val t = System.nanoTime()

    Files.lines(file, Charsets.UTF_8).use { lines ->
        lines
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .forEach { line ->
                promisedGames.add(executor.submit<GameResult> {
                    val gameBoard = GameBoard.createBoardFromCompactString(line, startPos, Int.MAX_VALUE)
                    val finishedGame = AStar.calculateMovesSequential(gameBoard, strategy)

                    val i = index.incrementAndGet()
                    if (i % 100 == 0)
                        println("$i boards finished")

                    GameResult(finishedGame.isWon, finishedGame.playedMoves)
                })
            }
    }

    val finishedGames = promisedGames.map { it.get() }
    val time = System.nanoTime() - t
    executor.shutdown()

    val output = file.parent.resolve("solutions.txt")
    if (Files.exists(output))
        Files.delete(output)

    Files.newBufferedWriter(output, Charsets.UTF_8).use { outputWriter ->
        for (game in finishedGames) {
            if (!game.isWon)
                outputWriter.append("game not won")
            else
                game.playedMoves.toList().forEach { outputWriter.append(it.toString()) }
            outputWriter.newLine()
        }
    }

    println("\n\nSolutions saved to: $output")
    println("Time taken: ${(time / 1_000_000.0).roundToInt()}ms")
    println("Total score: ${finishedGames.sumBy { it.playedMoves.size }}")
}

private data class GameResult(
    val isWon: Boolean,
    val playedMoves: MoveList<Color>
)

fun debug() {
    /*val gameBoard = GameBoard.createBoardFromCompactString(
        "1431514031212154004452332221115000531414333521225054122322413323254315030323223210022035210020144005342453523042122135454331353531345305455243253500454305212315542212142404523351344554550121513122",
        StartPos.UPPER_LEFT,
        1000
    )*/
    val seed = generateRandomSeed()
    val gameBoard = GameBoard.initBoard(
        seed = seed,
        boardSize = 64,
        numberOfColors = 6,
        startPos = StartPos.UPPER_LEFT
    )

    println("Seed: $seed")
    println("Game board:")
    for (y in 0 until gameBoard.boardSize) {
        for (x in 0 until gameBoard.boardSize) {
            print(gameBoard.getNodeAtPosition(x, y).color)
        }
        println()
    }
    println()

    aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FASTEST)
    aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FAST)
    //aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE)
    //aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_SLOW)
    //aStarSimulation(gameBoard, AStarStrategies.ADMISSIBLE)
}