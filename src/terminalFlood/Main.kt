package terminalFlood

import terminalFlood.algo.astar.AStarStrategies
import terminalFlood.game.GameBoard
import terminalFlood.game.StartPos
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.contains("-debug")) {
        debug()
        return
    }
    if (args.contains("-help") || args.contains("-h")) {
        printHelp()
        return
    }

    val memoryScheme = when {
        args.contains("-queueCutoff")           -> MemorySavingScheme.QUEUE_CUTOFF
        args.contains("-lessMemoryQueueCutoff") -> MemorySavingScheme.QUEUE_CUTOFF
        else                                    -> MemorySavingScheme.NO_SPECIAL_SCHEME
    }

    if (args.contains("-solutionsForDataset")) {
        val arrayIndex = args.indexOf("-solutionsForDataset")
        val threads = args[arrayIndex + 1].toInt()
        val strategy = Util.strategyFromString(args[arrayIndex + 2])
        val startPos = parseStartPosition(args[arrayIndex + 3])
        val file = Paths.get(args[arrayIndex + 4])

        Datasets.findSolutionsForDataset(file, threads, strategy, startPos, memoryScheme)
        return
    }
    if (args.contains("-createDataset")) {
        val arrayIndex = args.indexOf("-createDataset")
        val boardSize = args[arrayIndex + 1].toInt()
        val numberOfColors = args[arrayIndex + 2].toInt()
        val numberOfBoards = args[arrayIndex + 3].toInt()
        val file =
            if (args.size > arrayIndex + 4) {
                Paths.get(args[arrayIndex + 4])
            } else {
                Paths.get(
                    System.getProperty("user.dir"),
                    "dataset b${boardSize}c${numberOfColors}n${numberOfBoards}.txt"
                )
            }

        Datasets.createDataset(file, boardSize, numberOfColors, numberOfBoards)
        return
    }
    if (args.contains("-playFromDataset")) {
        val arrayIndex = args.indexOf("-playFromDataset")
        val lineNumber = args[arrayIndex + 1].toInt()
        val startPos = parseStartPosition(args[arrayIndex + 2])
        val file = Paths.get(args[arrayIndex + 3])

        Datasets.playFromDataset(file, lineNumber, startPos)
        return
    }
    if (args.contains("-solveFromDataset")) {
        val arrayIndex = args.indexOf("-solveFromDataset")
        val lineNumber = args[arrayIndex + 1].toInt()
        val strategy = Util.strategyFromString(args[arrayIndex + 2])
        val startPos = parseStartPosition(args[arrayIndex + 3])
        val file = Paths.get(args[arrayIndex + 4])

        Datasets.solveFromDataset(file, lineNumber, startPos, strategy, memoryScheme)
        return
    }

    var seed = Util.generateRandomSeed()
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
        args.contains("-astar_a")    -> Util.aStarSimulation(
            gameBoard,
            AStarStrategies.ADMISSIBLE,
            memoryScheme
        )
        args.contains("-astar_ia")   -> Util.aStarSimulation(
            gameBoard,
            AStarStrategies.INADMISSIBLE,
            memoryScheme
        )
        args.contains("-astar_iaf")  -> Util.aStarSimulation(
            gameBoard,
            AStarStrategies.INADMISSIBLE_FAST,
            memoryScheme
        )
        args.contains("-astar_iaff") -> Util.aStarSimulation(
            gameBoard,
            AStarStrategies.INADMISSIBLE_FASTEST,
            memoryScheme
        )
        args.contains("-astar_ias")  -> Util.aStarSimulation(
            gameBoard,
            AStarStrategies.INADMISSIBLE_SLOW,
            memoryScheme
        )
        else                         -> PlayGame.play(gameBoard)
    }
}

private fun parseStartPosition(startPosString: String): StartPos = when (startPosString) {
    "ul" -> StartPos.UPPER_LEFT
    "ur" -> StartPos.UPPER_RIGHT
    "ll" -> StartPos.LOWER_LEFT
    "lr" -> StartPos.LOWER_RIGHT
    "m" -> StartPos.MIDDLE
    else -> throw IllegalArgumentException("Incorrect value for startPos!")
}

private fun printHelp() {
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

    println("\n\nWhen trying to find solutions for game boards, you can turn on special memory saving modes with one")
    println("of the following parameters:")
    println("  -lessMemory            \t --")
    println("\t\tThis mode will trade some performance in exchange for less memory needed for the")
    println("\t\tcomputation. This can be a net plus for performance in cases of heavy memory requirements")
    println("\t\twhere using this mode would result in less GC pauses or the ability to use more cores of")
    println("\t\tyour CPU. The quality of the solutions is not impaired by this mode.")
    println("  -lessMemoryQueueCutoff \t --")
    println("\t\tThe points given for \"-lessMemory\" also apply to this mode, but additionally, this mode")
    println("\t\twill also limit the size of the queue used by A* by discarding elements whenever a")
    println("\t\tcertain cutoff limit is reached. The result is a clear upper bound in regards to the")
    println("\t\tmaximum memory needed to compute a solution, but it also means that the quality of the")
    println("\t\tsolution can be impaired by this mode. Don't use this mode together with \"astar_a\" when")
    println("\t\ttrying to find guaranteed optimal results.")

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

    println("\n\nGiven a dataset, you can choose and play or solve a board contained in it given the following")
    println("commands:")
    println("  -playFromDataset [lineNumber] [startPos] [filepath]\n")
    println("  -solveFromDataset [lineNumber] [strategy] [startPos] [filepath]\n")
    println("  [lineNumber] \t --")
    println("\t\tThe line number of the game board you want to play within the dataset file. Note that")
    println("\t\tline numbers are starting from 1.")
    println("  [strategy]   \t --")
    println("\t\tThe strategy used to compute a solution. The possible values are the same as the single")
    println("\t\tsolution computation arguments, just without the minus sign in front. See above for the")
    println("\t\tlist of options and what each option does.")
    println("  [startPos]   \t --")
    println("\t\tThe starting position of the game board. The option is exactly the same as the argument")
    println("\t\tused when simply playing the game.")
    println("  [filepath]   \t --")
    println("\t\tThe file path to the text file containing the game boards. Please only use forward")
    println("\t\tslashes, even on Windows systems.")

    println("\nTo give examples of what the full commands would look like:")
    println("  java -jar terminal-flood.jar -playFromDataset 815 ul \"/path/to/dataset.txt\"")
    println("  java -jar terminal-flood.jar -solveFromDataset 815 astar_iaf ul \"/path/to/dataset.txt\"")
    println("With the above commands you would either play or solve board number 815 from the given dataset with")
    println("the starting position in the upper left (and use the astar_iaf strategy in case you try to solve")
    println("the board).")
}

private fun debug() {
    /*val gameBoard = GameBoard.createBoardFromCompactString(
        "1431514031212154004452332221115000531414333521225054122322413323254315030323223210022035210020144005342453523042122135454331353531345305455243253500454305212315542212142404523351344554550121513122",
        StartPos.UPPER_LEFT,
        1000
    )*/
    val seed = Util.generateRandomSeed()
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

    Util.aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FASTEST)
    Util.aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_FAST)
    //Util.aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE)
    //Util.aStarSimulation(gameBoard, AStarStrategies.INADMISSIBLE_SLOW)
    //Util.aStarSimulation(gameBoard, AStarStrategies.ADMISSIBLE)
}

enum class MemorySavingScheme {
    NO_SPECIAL_SCHEME, QUEUE_CUTOFF
}