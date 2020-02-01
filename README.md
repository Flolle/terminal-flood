Welcome to terminal-flood, a CLI version of Flood-It.

# Gameplay

### original Flood-It
Flood-It is a game consisting of a grid of colored squares. The player has a starting position (usually the upper left, but doesn't have to be) with a starting color and every turn you choose a new color. Every grid connected to your starting position that has the same color as it will be [flood filled](https://en.wikipedia.org/wiki/Flood_fill) with the new color. The goal is to turn the whole game board monocolored using as few moves as possible.

### terminal-flood
Since displaying colors isn't as easy in terminals, terminal-flood works like this: You start in one of the corners or the middle of the board, every field you own is denoted by an "@". Not taken fields consist of groups with the same color denoted by a number. Every turn you choose a color and every bordering group of that color will be taken over. You win the game by taking over the whole board within the allotted number of moves.


# Program usage

To use this program, you need Java 8 or newer. You can download Java Runtimes for example [here](https://adoptopenjdk.net/) or [here](https://www.azul.com/downloads/zulu-community/).

The program can be started by simply running the jar file:

```
java -jar terminal-flood.jar
```

The above command will start a game with default arguments (14x14 board, 6 colors, upper left starting position, random seed), but you can customize the game through program arguments. For example the following command:

```
java -jar terminal-flood.jar -seed "xyzzy" -size 18 -colors 6 -startPos m
```

Will create a game with an 18x18 board, 6 colors, starting position in the middle and the string "xyzzy" used as the seed value for the board creation.

For the complete documentation of all program arguments, start the program with the `-help` or `-h` argument like so:

```
java -jar terminal-flood.jar -help
```

terminal-flood also allows you to solve single or multiple Flood-It boards as a secondary program use. This functionality can be used to find solutions for boards that you struggle with or simply as a sort of benchmark for your PC. See the above help command for documentation of the program arguments used for this feature.

This repository contains some datasets for use and benchmark results from my PC in the `benchmark` folder.


# Links

- Java GUI version: [ColorFill](https://github.com/smack42/ColorFill)
- Android version: [Open Flood](https://github.com/GunshipPenguin/open_flood/)
- Browser version: [Flood-It](https://unixpapa.com/floodit/)


# License

```
terminal-flood
Copyright (C) 2020 Florian Fischer

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License version 3 as
published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
