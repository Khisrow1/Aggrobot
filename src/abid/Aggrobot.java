package abid;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Move;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class Aggrobot extends AbstractionLayerAI{

    public class Cluster
    {
        GameState gs;
        List<Unit> units = null;
        // TODO(Abid): Make directional movement
        int direction;
        String action = "wait";
        Unit target = null;
        Unit source = null;
        UnitType makeType = null;
        int posX = 0;
        int posY = 0;


        public Cluster()
        {
            units = new LinkedList<>();
        }

        public void moveC(GameState gs, int x, int y) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null) { moveToVicinity(gs, unit, x, y); }
            }
            action = "move";
            posX = x;
            posY = y;
        }

        // public boolean notInVicinity(GameState gs)
        // {
        //     float averageXPos = 0.0f;
        //     float averageyPos = 0.0f;
        //     for (Unit unit : units)
        //     {
        //         unit.getX();
        //     }

        //     return false;
        // }

        public class Vector2D
        {
            public float x;
            public float y;

            public Vector2D() { x = 0.f; y = 0.f; }
        }
        public Vector2D getCentroid()
        {
            Vector2D centroid = new Vector2D();
            
            for (Unit unit : units)
            {
                centroid.x = centroid.x + unit.getX();
                centroid.y = centroid.y + unit.getY();
            }

            centroid.x = centroid.x / units.size();
            centroid.y = centroid.y / units.size();

            return centroid;
        }
        public void moveCIfNotVicinity(GameState gs, int x, int y) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null) { moveToVicinity(gs, unit, x, y); }
            }
            action = "move";
            posX = x;
            posY = y;
        }

        public void trainC(GameState gs, UnitType unit_type) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    train(unit, unit_type);
                }
            }
            action = "train";
            makeType = unit_type;
        }

        public void buildC(GameState gs, UnitType unit_type, int x, int y) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    // Find the closest spot
                    build(unit, unit_type, x, y);
                }
            }
            action = "build";
            makeType = unit_type;
            posX = x;
            posY = y;
        }

        public void harvestC(GameState gs, Unit target, Unit base) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    harvest(unit, target, base);
                }
            }
            action = "harvest";
            this.target = base;
            source = target;
        }

        public void attackC(GameState gs, Unit target) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    attack(unit, target);
                }
            }
            action = "attack";
            this.target = target;
        }

        public void idleC(GameState gs) {
            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    idle(unit);
                }
            }
            action = "idle";
        }

        protected void moveToVicinity(GameState gs, Unit unit, int x, int y)
        {
            assert(gs.getActionAssignment(unit) == null);
            boolean[][] isGridFree = gs.getAllFree();

            if(isGridFree[x][y])
            {
                move(unit, x, y);
                return;
            }

            int xAllowed = 0;
            if(x > gs.getPhysicalGameState().getWidth() - x) { xAllowed = x; }
            else { xAllowed = gs.getPhysicalGameState().getWidth() - x; }

            int yAllowed = 0;
            if(y > gs.getPhysicalGameState().getHeight() - y) { yAllowed = y; }
            else { yAllowed = gs.getPhysicalGameState().getHeight() - y; }

            int xAlternate = 1;
            int yAlternate = 1;
            for (int xStride = 1; xStride < xAllowed; xStride++)
            {
                for (int yStride = 1; yStride < yAllowed; yStride++)
                {
                    int potentialX = x + xStride*xAlternate;
                    int potentialY = y + yStride*yAlternate;
                    if(potentialX >= gs.getPhysicalGameState().getWidth() || potentialX < 0) { continue; }
                    if(potentialY >= gs.getPhysicalGameState().getHeight() || potentialY < 0) { continue; }

                    if (isGridFree[potentialX][potentialY])
                    {
                        move(unit, potentialX, potentialY);
                        return;
                    }
                    yAlternate = yAlternate*-1;
                }
                xAlternate = xAlternate*-1;
            }
        }

        protected void buildInVicinity(GameState gs, Player p, UnitType unitType)
        {
            Unit unit = this.get(0);
            // for (Unit unit2 : units)
            // {
            //     if(gs.getActionAssignment(unit2) == null) { unit = unit2; }
            // }

            // if(unit == null) { return; }

            boolean[][] isGridFree = gs.getAllFree();

            int x = unit.getX();
            int y = unit.getY();

            int xAllowed = 0;
            if(x > gs.getPhysicalGameState().getWidth() - x) { xAllowed = x; }
            else { xAllowed = gs.getPhysicalGameState().getWidth() - x; }

            int yAllowed = 0;
            if(y > gs.getPhysicalGameState().getHeight() - y) { yAllowed = y; }
            else { yAllowed = gs.getPhysicalGameState().getHeight() - y; }

            int xAlternate = 1;
            int yAlternate = 1;
            List<Integer> reservedPositions = new LinkedList<>();
            for (int xStride = 2; xStride < xAllowed; xStride += 2)
            {
                for (int yStride = 1; yStride < yAllowed; yStride++)
                {
                    int potentialX = x + xStride*xAlternate;
                    int potentialY = y + yStride*yAlternate;
                    if(potentialX >= gs.getPhysicalGameState().getWidth() || potentialX < 0) { continue; }
                    if(potentialY >= gs.getPhysicalGameState().getHeight() || potentialY < 0) { continue; }

                    if (isGridFree[potentialX][potentialY])
                    {
                        // build(unit, unitType, potentialX, potentialY);
                        buildIfNotAlreadyBuilding(unit, unitType, potentialX, potentialY,reservedPositions,p,gs.getPhysicalGameState());
                        return;
                    }
                    yAlternate = yAlternate*-1;
                }
                xAlternate = xAlternate*-1;
            }
        }

        public int size() { return units.size(); }

        public Unit get(int idx) { return units.get(idx); }

        public Unit remove(int idx) { return units.remove(idx); }

        public boolean isIdle() { return action.equals("wait"); }

        public List<Unit> getUnits() { return units; }

        public class UnitDesc
        {
            Unit unit = null;
            Vector2D distVec = new Vector2D();
        }
        public UnitDesc getClosestEnemy(PhysicalGameState physicalGS, Player player)
        {
            UnitDesc unitDesc = new UnitDesc();

            Unit leader = pawnCluster.get(0);
            for(Unit unit : physicalGS.getUnits())
            {
                if(unit.getPlayer() >= 0 && unit.getPlayer() != player.getID())
                {
                    int unitDistX = Math.abs(leader.getX() - unit.getX());
                    int unitDistY = Math.abs(leader.getY() - unit.getY());

                    int dist = unitDistX + unitDistY;

                    if(unitDesc.unit == null || dist < (unitDesc.distVec.x + unitDesc.distVec.y))
                    {
                        unitDesc.distVec.x = unitDistX;
                        unitDesc.distVec.y = unitDistY;
                        unitDesc.unit = unit;
                    }
                }
            }

            return unitDesc;
        }
        public boolean withinVicinity(Unit unit, int margin)
        {
            Vector2D centroid = getCentroid();

            int dist = (int)(Math.abs(centroid.x - unit.getX()) + Math.abs(centroid.y - unit.getY()));

            if(dist <= margin)
            {
                return true;
            }
            else return false;
        }

        public void update(GameState gs, Player player, List<Unit> workers, int numberNeeded)
        {
            for (Unit unit : units)
            {
                if (unit == null) { units.remove(unit); }
            }
            int missingWorkers = numberNeeded - units.size();
            assert(missingWorkers >= 0);

            for(int idx = 0; idx < workers.size() && !workers.isEmpty();)
            {
                Unit unit = workers.get(idx);
                if(units.contains(unit))
                {
                    workers.remove(unit);
                }
                else if(missingWorkers > 0)
                {
                    units.add(unit);
                    workers.remove(unit);
                    missingWorkers--;
                }
                else idx++;
            }

            for (Unit unit : units)
            {
                if(gs.getActionAssignment(unit) == null)
                {
                    switch (action) {
                        case "wait":
                            idle(unit);
                            break;
                        case "move":
                            // ClusterCloser(gs, Cluster, maxDistance);
                            if (!(getAbstractAction(unit) instanceof ai.abstraction.Move))
                            {
                                moveToVicinity(gs, unit, posX, posY);
                            }
                            break;
                        case "harvest":
                            assert(source != null && target != null);
                            harvest(unit, source, target);
                            break;
                        case "return":
                            assert(source != null && target != null);
                            harvest(unit, source, target);
                            break;
                        case "produce":
                            assert(makeType!= null);
                            train(unit, makeType);
                            break;
                        case "attack":
                            assert(target!= null);
                            if(target.getHitPoints() == 0)
                            {
                                target = getClosestEnemy(gs.getPhysicalGameState(), player).unit;
                            }
                            attack(unit, target);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    UnitTypeTable m_utt = null;

    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType lightType;
    UnitType rangedType;
    UnitType heavyType;

    // List<Unit> pawnCluster = new LinkedList<>();
    // List<Unit> homeCluster = new LinkedList<>();

    int maxNumPawn = 4;
    int maxNumHome = 3;

    int maxBarracksNum = 2;

    int numRange = 0;
    int numHeavy = 0;
    int numLight = 0;
    Cluster pawnCluster = new Cluster();
    Cluster homeCluster = new Cluster();

    boolean clusterInit = false;
    // public AggroBot(int timeBudget, int iterationsBudget) {
    //     super(timeBudget, iterationsBudget);
    //     //TODO Auto-generated constructor stub
    // }

    public Aggrobot(UnitTypeTable utt)
    {
        this(utt, new AStarPathFinding());
    }

    public Aggrobot(UnitTypeTable utt, PathFinding pf)
    {
        super(pf);
        reset(utt);
    }

    @Override
    public AI clone() {
        return new Aggrobot(m_utt, pf);
    }

    @Override
    public PlayerAction getAction(int p, GameState gameState) throws Exception {
        PhysicalGameState physicalGS = gameState.getPhysicalGameState();
        Player player = gameState.getPlayer(p);


        List<Unit> workers = new LinkedList<>();
        for (Unit unit : physicalGS.getUnits())
        {
            if(unit.getType() == workerType &&
               unit.getPlayer() == player.getID())
            {
                workers.add(unit);
            }
        }

        // Bases
        for (Unit unit : physicalGS.getUnits())
        {
            if(unit.getType() == baseType &&
               unit.getPlayer() == player.getID() &&
               gameState.getActionAssignment(unit) == null)
            {
                if(workers.size() < maxNumHome+maxNumPawn)
                {
                    if(player.getResources() >= workerType.cost)
                    {
                        train(unit, workerType);
                    }
                }
                else
                {
                    // Either create ranged units or if done that, make more 2nd cluster workers
                }
            }
        }

        pawnCluster.update(gameState, player, workers, maxNumPawn);
        homeCluster.update(gameState, player, workers, maxNumHome);

        if(homeCluster.size() >= 1)
        {
            // homeCluster.moveToVicinity(gameState, homeCluster.get(0), 0, 2);
            int closestResDist = 0;
            Unit closestResUnit = null;

            int closestStockDist = 0;
            Unit closestStockUnit = null;

            Unit leader = homeCluster.get(0);
            for (Unit unit : physicalGS.getUnits())
            {
                if (unit.getType().isResource)
                {
                    int dist =  Math.abs(leader.getX() - unit.getX()) +
                                Math.abs(leader.getY() - unit.getY());
                    if(closestResUnit == null || dist < closestResDist)
                    {
                        closestResDist = dist;
                        closestResUnit = unit;
                    }
                }
                else if(unit.getType().isStockpile && unit.getPlayer() == player.getID())
                {
                    int dist =  Math.abs(leader.getX() - unit.getX()) +
                                Math.abs(leader.getY() - unit.getY());
                    if(closestStockUnit == null || dist < closestStockDist)
                    {
                        closestStockDist = dist;
                        closestStockUnit = unit;
                    }
                }
            }
            if(closestResUnit != null && closestStockUnit != null)
            {
                // NOTE(Abid): If we have enough resource to make a barracks
                int numBarracks = 0;
                for(Unit unit : physicalGS.getUnits())
                {
                    if(unit.getType() == barracksType) numBarracks++;
                }
                if(player.getResources() >= barracksType.cost && numBarracks < maxBarracksNum)
                {
                    // NOTE(Abid): Build barracks
                    homeCluster.buildInVicinity(gameState, player, barracksType);
                }

                // NOTE(Abid): If we still got a free worker left, then gather resources
                else
                {
                    for (Unit unit : homeCluster.units) {
                        if (gameState.getActionAssignment(unit) == null) {
                            homeCluster.harvestC(gameState, closestResUnit, closestStockUnit);
                        }
                    }

                }
                // homeCluster.harvestC(gameState, closestResUnit, closestStockUnit);
            }
        }

        if(pawnCluster.size() > 0)
        {
            Unit leader = pawnCluster.get(0);

            AbstractAction action = getAbstractAction(leader);
            // Keep moving if we are in safe zone, hoever, start attacking the moment the enemy reaches
            // half-way through.
            // Once we have, light and ranged units, attack!
            if(action instanceof Move)
            {
                // Move movAction = (Move)action;
                // if(movAction.completed(gameState)) { System.out.println("Completed"); }
                //else { System.out.println("Still going"); }
            }
            else
            {
                pawnCluster.moveC(gameState, gameState.getPhysicalGameState().getWidth()/4,
                                             gameState.getPhysicalGameState().getHeight()/4); // (leader, , leader.getY()+2);
            }
        }

        // Barracks
        for (Unit unit : physicalGS.getUnits())
        {
            int temp = Math.min(numLight, numHeavy);
            int minNum = Math.min(temp, numRange);
            boolean balanced = false;

            if (minNum == temp && minNum == numRange && temp == numLight && temp == numHeavy)
            {
                balanced = true;
            } 

            if(unit.getType() == barracksType &&
               unit.getPlayer() == player.getID() &&
               gameState.getActionAssignment(unit) == null)
            {
                // Make Ranged stuff
                if(balanced && player.getResources() >= rangedType.cost)
                {
                    train(unit, rangedType);
                    numRange++;
                }
                else
                {
                    // We want to have at least 2 ranged units
                    if(numRange < 2 && player.getResources() >= rangedType.cost)
                    {
                        train(unit, rangedType);
                        numRange++;
                    }
                    else
                    {
                        // Ranged balance
                        if(minNum == numRange && player.getResources() >= rangedType.cost)
                        {
                            train(unit, rangedType);
                            numRange++;
                        }
                        // Light balance
                        else if(minNum == numLight && player.getResources() >= lightType.cost)
                        {
                            train(unit, lightType);
                            numLight++;
                        }
                        // Heavy balance
                        else if(minNum == numHeavy && player.getResources() >= heavyType.cost)
                        {
                            train(unit, heavyType);
                            numHeavy++;
                        }
                    }
                }
            }
        }

        // // Light
        // for (Unit unit : physicalGS.getUnits())
        // {
        //     if(unit.getType() == baseType &&
        //        unit.getPlayer() == player.getID() &&
        //        gameState.getActionAssignment(unit) == null)
        //     {
        //     }
        // }

        // // Ranged
        // for (Unit unit : physicalGS.getUnits())
        // {
        //     if(unit.getType() == baseType &&
        //        unit.getPlayer() == player.getID() &&
        //        gameState.getActionAssignment(unit) == null)
        //     {
        //     }
        // }

        // // Heavy
        // for (Unit unit : physicalGS.getUnits())
        // {
        //     if(unit.getType() == baseType &&
        //        unit.getPlayer() == player.getID() &&
        //        gameState.getActionAssignment(unit) == null)
        //     {
        //         // Make workers if conditions allow
        //     }
        // }

        return translateActions(p, gameState);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    public void reset() { super.reset(); }

    @Override
    public void reset(UnitTypeTable utt) {
        m_utt = utt;
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        lightType  = utt.getUnitType("Light");
        rangedType = utt.getUnitType("Ranged");
        heavyType  = utt.getUnitType("Heavy");

        pawnCluster = new Cluster();
        homeCluster = new Cluster();
    }
    
    public void ClusterCloser(GameState gs, List<Unit> Cluster, int maxDistance)
    {
        assert(Cluster.size() > 0);
        Unit leader = Cluster.get(0);
        if(gs.getActionAssignment(leader) != null) return;
        for (int idx = 1; idx < Cluster.size()-1; idx++)
        {
            if(gs.getActionAssignment(leader) != null) continue;

            Unit follower = Cluster.get(idx);
            if(leader.getX() - follower.getX() > maxDistance ||
               leader.getY() - follower.getY() > maxDistance)
            {
                for(int stride = 1; stride < maxDistance+1; stride++)
                {
                    for (int xOffset = 1; xOffset < 3; xOffset++)
                    {
                        for (int yOffset = 1; yOffset < 3; yOffset++)
                        {
                            int potentialX = leader.getX();
                            int potentialY = leader.getY();
                            boolean isSpotFree = gs.free(potentialX, potentialY);
                            if(isSpotFree)
                            {
                                move(follower, potentialX, potentialX);
                            }
                        }
                    }

                }
            }
        }
    }

    public void ClusterAttack(GameState gs, List<Unit> Cluster, Unit Target)
    {
        assert(Cluster.size() != 0);
        for(int idx = 0; idx < Cluster.size(); idx++)
        {
            attack(Cluster.get(idx), Target);
        }
    }

    public void CluserHarvest(List<Unit> Cluster, Unit Res, Unit Stock)
    {
        assert(Cluster.size() != 0);
        for(int idx = 0; idx < Cluster.size(); idx++)
        {
            harvest(Cluster.get(idx), Res, Stock);
        }
    }

    public boolean IsClusterReady(GameState gs, List<Unit> Cluster)
    {
        assert(Cluster.size() != 0);
        Unit leader = Cluster.get(0);
        return gs.getActionAssignment(leader) == null;
    }

        // while(missingWorkers > 0 && !workers.isEmpty())
        // {
        //     Unit unit = workers.get(0);

        //     if(!cluster.contains(unit))
        //     {
        //         cluster.add(workers.get(0));
        //         missingWorkers--;
        //     }
        //     workers.remove(0);
        // }

    void ClusterMove(GameState gs, List<Unit> cluster)
    {
        Unit leader = cluster.get(0);
        move(leader, gs.getPhysicalGameState().getWidth()/2, gs.getPhysicalGameState().getHeight()/2);
    }

    public void AssignLeaderAction(GameState gs, List<Unit> Cluster, int maxDistance)
    {
        Unit leader = Cluster.get(0);
        for (int idx = 1; idx < Cluster.size()-1; idx++)
        {
            Unit follower = Cluster.get(idx);
            if(gs.getActionAssignment(follower) == null)
            {
            }
        }
    }
}
