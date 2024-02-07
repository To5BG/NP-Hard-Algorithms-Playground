import java.io.File;

import game.board.oop.*;
import game.board.slim.BoardSlim;
import game.board.slim.STile;

public class DeadSquareTest {
    static void printLevelWithTargets(Board board) {
        for (int y = 0; y < board.height; ++y) {
            for (int x = 0; x < board.width; ++x) {
                EPlace place = board.tiles[x][y].place;
                ESpace space = board.tiles[x][y].space;
                if (place == EPlace.BOX_1) {
                    System.out.print('.');
                } else if (space != null) {
                    System.out.print(space.getSymbol());
                } else {
                    System.out.print("?");
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        File levels = new File("Lab 1/Sokoban4j/levels/Aymeric_Hard.sok");
        if (!levels.canRead()) {
            System.out.printf("can't find level file %s\n", levels.getAbsolutePath());
            return;
        }

        System.out.printf("testing levels in %s\n\n", levels.getName());
        for (int i = 1; i <= 10; ++i) {
            System.out.printf("== level %d ==\n\n", i);
            Board board = Board.fromFileSok(levels, i);

            printLevelWithTargets(board);
            System.out.println();

            BoardSlim bs = board.makeBoardCompact().makeBoardSlim();

            MyAgent.board = bs;
            MyAgent.DeadSquareDetector dsd = new MyAgent.DeadSquareDetector(bs);

            System.out.println("dead squares: \n");
            for (int y = 0; y < bs.height(); ++y) {
                for (int x = 0; x < bs.width(); ++x)
                    System.out.print((STile.WALL_FLAG & bs.tile(x, y)) != 0 ? '#' : (dsd.dead[x][y] ? 'X' : '_'));
                System.out.println();
            }
            System.out.println();
        }
    }
}
