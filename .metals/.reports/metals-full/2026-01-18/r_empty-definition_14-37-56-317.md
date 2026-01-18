error id: file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java:_empty_/IRadarResult#getObjectDistance#
file://<WORKSPACE>/src/algorithms/TeamAMainBotXXX.java
empty definition using pc, found symbol in pc: _empty_/IRadarResult#getObjectDistance#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 2108
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

    private boolean turning = false;
    private double targetHeading;
    private boolean avoidRight = true;


    // Cible donnée par secondary
    private boolean enemyTargetKnown = false;
    private double enemyTargetAngle;

    private static final double ANGLE_EPS = 0.05;

    @Override
    public void activate() {
        log("Main bot activated");
    }

    @Override
    public void step() {

        // 🔒 Rotation bloquante (mur ou orientation)
        if (turning) {
            if (sameDir(getHeading(), targetHeading)) {
                turning = false;
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
            return;
        }

        // 📩 Messages secondary
        handleMessages();

        // 🔍 Radar ennemi = PRIORITÉ ABSOLUE
        IRadarResult enemy = scanEnemy();
        if (enemy != null) {
            aimAndFire(enemy);
            return;
        }

        // 🚧 Mur
        if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
            startAvoid();
            return;
        }

        // 🎯 Aller vers la cible donnée par secondary
        if (enemyTargetKnown) {
            goToTarget();
            return;
        }

        // 🚶 Exploration simple
        move();
    }

    /* ================= ENEMY ================= */

    private IRadarResult scanEnemy() {
        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemyTargetKnown = false; // radar > message
                return r;
            }
        }
        return null;
    }

    private void aimAndFire(IRadarResult enemy) {

        double diff = Math.sin(enemy.getObjectDirection() - getHeading());
        if (enemy.get@@ObjectDistance() > 30) {
            fire(enemy.getObjectDirection());
            log("🔥 FIRING");
        }
        
        if (Math.abs(diff) > ANGLE_EPS) {
            stepTurn(diff > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        } else {
            fire(enemy.getObjectDirection());
            log("🔥 FIRING");
        }
    }

    /* ================= SECONDARY TARGET ================= */

    private void goToTarget() {

        double diff = Math.sin(enemyTargetAngle - getHeading());

        if (Math.abs(diff) > ANGLE_EPS) {
            stepTurn(diff > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        } else {
            move();
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

    avoidRight = !avoidRight; // alterne
    log("Avoiding obstacle (alternate)");
}


    /* ================= COMMS ================= */

    private void handleMessages() {
        ArrayList<String> msgs = fetchAllMessages();

        for (String m : msgs) {
            if (m.startsWith("ENEMY:")) {
                String[] p = m.split(":");

                // ENEMY:distance:angle
                double reportedAngle = Double.parseDouble(p[2]);
                            
                // On transforme ça en orientation RELATIVE
                enemyTargetAngle = getHeading() + reportedAngle;
                enemyTargetKnown = true;
                            
                log("Enemy target received → turning approx");


                log("Enemy target received (angle=" + enemyTargetAngle + ")");
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

empty definition using pc, found symbol in pc: _empty_/IRadarResult#getObjectDistance#