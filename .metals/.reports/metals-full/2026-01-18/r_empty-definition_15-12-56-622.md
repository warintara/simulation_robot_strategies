error id: file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java:IFrontSensorResult/Types#
file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java
empty definition using pc, found symbol in pc: IFrontSensorResult/Types#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1620
uri: file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java
text:
```scala
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

import java.util.ArrayList;

public class TeamAMainBotXXX extends Brain {

    /* ===================== VARIABLES ===================== */

    private boolean turning = false;
    private double targetHeading;
    private boolean avoidRight = true;

    // Recherche après message secondary
    private boolean enemyTargetKnown = false;
    private int searchSweep = 0;

    private static final double ANGLE_EPS = 0.05;
    private int stuckCounter = 0;
    private static final int STUCK_LIMIT = 8;


    /* ===================== ACTIVATE ===================== */

    @Override
    public void activate() {
        log("Main bot activated");
    }

    /* ===================== STEP ===================== */

    @Override
    public void step() {

        /* 🔒 Rotation bloquante */
        if (turning) {
            if (sameDir(getHeading(), targetHeading)) {
                turning = false;
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
            return;
        }

        /* 📩 Messages des scouts */
        handleMessages();

        /* 🔍 Radar ennemi = priorité absolue */
        IRadarResult enemy = scanEnemy();
        if (enemy != null) {
            aimAndFire(enemy);
            return;
        }

        /* 🚧 Mur ou obstacle devant */
IFrontSensorResult.Types front = detectFront().getObjectType();

if (front == IFrontSensorResult.Types.WALL ||
    front == IFrontSensorResult.Type@@s.Wreck) {

    stuckCounter++;

    // 🟥 Coin / blocage détecté
    if (stuckCounter >= STUCK_LIMIT) {
        log("⚠️ Stuck detected → backing off");
        moveBack();
        stuckCounter = 0;
        return;
    }

    startAvoid();
    return;
}

// ✅ Si on avance normalement → reset
stuckCounter = 0;


        /* 🔄 Recherche déclenchée par scout */
        if (enemyTargetKnown) {
            searchFromSecondary();
            return;
        }

        /* 🚶 Exploration normale */
        move();
    }

    /* ================= RADAR ENEMY ================= */

    private IRadarResult scanEnemy() {
        for (IRadarResult r : detectRadar()) {
            if ((r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                 r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot)
                && r.getObjectDistance() > 50) {

                enemyTargetKnown = false; // radar > message
                return r;
            }
        }
        return null;
    }

    private void aimAndFire(IRadarResult enemy) {

        double diff = Math.sin(enemy.getObjectDirection() - getHeading());

        if (Math.abs(diff) > ANGLE_EPS) {
            stepTurn(diff > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        } else {
            fire(enemy.getObjectDirection());
            log("🔥 FIRING");
        }
    }

    /* ================= SECONDARY SEARCH ================= */

    private void searchFromSecondary() {

        if (searchSweep > 0) {
            stepTurn(Parameters.Direction.LEFT);
            searchSweep--;
        } else {
            enemyTargetKnown = false;
            log("Search aborted");
        }
    }

    /* ================= AVOID ================= */

    private void startAvoid() {
        turning = true;

        if (avoidRight) {
            targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.RIGHT);
        } else {
            targetHeading = getHeading() - Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.LEFT);
        }

        avoidRight = !avoidRight;
        log("Avoiding obstacle");
    }

    /* ================= COIN DETECTION ================= */

    private boolean stuckInCorner() {
        int wallCount = 0;

        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.WALL &&
                r.getObjectDistance() < 200) {
                wallCount++;
            }
        }
        return wallCount >= 2;
    }

    /* ================= COMMS ================= */

    private void handleMessages() {
        ArrayList<String> msgs = fetchAllMessages();

        for (String m : msgs) {
            if (m.startsWith("ENEMY:")) {
                enemyTargetKnown = true;
                searchSweep = 25;
                log("Enemy signal received → starting search");
            }
        }
    }

    /* ================= UTILS ================= */

    private boolean sameDir(double a, double b) {
        return Math.abs(Math.sin(a - b)) < ANGLE_EPS;
    }

    private void log(String msg) {
        System.out.println("[MAIN] " + msg);
        sendLogMessage("[MAIN] " + msg);
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: IFrontSensorResult/Types#