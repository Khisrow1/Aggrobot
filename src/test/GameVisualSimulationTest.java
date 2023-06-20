package test;

import ai.core.AI;
import ai.*;
import ai.abstraction.*;
import ai.abstraction.pathfinding.BFSPathFinding;
import gui.PhysicalGameStatePanel;

import javax.swing.JFrame;

import abid.Aggrobot;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.units.UnitTypeTable;

 /**
 *
 * @author santi
 */
public class GameVisualSimulationTest {
    public static void main(String[] args) throws Exception {
        UnitTypeTable utt = new UnitTypeTable();

        // PhysicalGameState pgs = PhysicalGameState.load("maps/8x8/basesWorkers8x8.xml", utt);
        // PhysicalGameState pgs = PhysicalGameState.load("maps/16x16/basesWorkers16x16.xml", utt);
        PhysicalGameState pgs = PhysicalGameState.load("maps/24x24/basesWorkers24x24.xml", utt);
        // PhysicalGameState pgs = PhysicalGameState.load("maps/chambers32x32.xml", utt);
        // PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();


//        PhysicalGameState pgs = MapGenerator.basesWorkers8x8Obstacle();

        GameState gs = new GameState(pgs, utt);
        int MAXCYCLES = 5000;
        int PERIOD = 20;
        boolean gameover = false;
        
        // AI ai1 = new WorkerRush(utt, new BFSPathFinding());        
        // AI ai1 = new Aggrobot(utt);
        AI ai1 = new Aggrobot(utt);
        // AI ai2 = new Aggrobot(utt);
        // AI ai2 = new WorkerRush(utt);
        // AI ai2 = new LightRush(utt);
        // AI ai1 = new RangedRush(utt);
        AI ai2 = new HeavyRush(utt);


        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_BLACK);
//        JFrame w = PhysicalGameStatePanel.newVisualizer(gs,640,640,false,PhysicalGameStatePanel.COLORSCHEME_WHITE);

        long nextTimeToUpdate = System.currentTimeMillis() + PERIOD;
        do{
            if (System.currentTimeMillis()>=nextTimeToUpdate) {
                PlayerAction pa1 = ai1.getAction(0, gs);
                PlayerAction pa2 = ai2.getAction(1, gs);
                gs.issueSafe(pa1);
                gs.issueSafe(pa2);

                // simulate:
                gameover = gs.cycle();
                w.repaint();
                nextTimeToUpdate+=PERIOD;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }while(!gameover && gs.getTime()<MAXCYCLES);
        ai1.gameOver(gs.winner());
        ai2.gameOver(gs.winner());
        
        System.out.println("Game Over");
    }    
}
