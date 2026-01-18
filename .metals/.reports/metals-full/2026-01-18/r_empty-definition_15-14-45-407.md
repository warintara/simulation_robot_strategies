error id: file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java:java/lang/String#startsWith(+1).
file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java
empty definition using pc, found symbol in pc: java/lang/String#startsWith(+1).
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 4135
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

    // Recherche après message scout
    private boolean enemyTargetKnown = false;
    private int searchSweep = 0;

    private static final double ANGLE_EPS = 0.05;

    // Détection de blocage
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

        /* 📩 Messages scouts */
        handleMessages();

        /* 🔍 Radar ennemi = priorité */
        IRadarResult enemy = scanEnemy();
        if (enemy != null) {
            aimAndFire(enemy);
            return;
        }

        /* 🚧 Mur ou obstacle devant */
        IFrontSensorResult.Types front = detectFront().getObjectType();

        if (front == IFrontSensorResult.Types.WALL ||
            front == IFrontSensorResult.Types.Wreck) {

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

        /* 🔄 Recherche suite message scout */
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

    /* ================= SEARCH FROM SCOUT ================= */

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

    /* ================= COMMS ================= */

    private void handleMessages() {
        ArrayList<String> msgs = fetchAllMessages();

        for (String m : msgs) {
            if (m.s@@tartsWith("ENEMY:")) {
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

empty definition using pc, found symbol in pc: java/lang/String#startsWith(+1).