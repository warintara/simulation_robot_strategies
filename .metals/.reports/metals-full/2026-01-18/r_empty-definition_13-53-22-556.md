error id: file://<WORKSPACE>/src/algorithms/ScoutSecondaryBot.java:java/lang/Override#
file://<WORKSPACE>/src/algorithms/ScoutSecondaryBot.java
empty definition using pc, found symbol in pc: java/lang/Override#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 6725
uri: file://<WORKSPACE>/src/algorithms/ScoutSecondaryBot.java
text:
```scala
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

import java.util.ArrayList;

public class ScoutSecondaryBot extends Brain {

    /* ===================== CONSTANTES ===================== */

    private static final double ANGLE_PRECISION = 0.05;
    private static final double SAFE_DISTANCE = 500;   // distance de sécurité minimale
    private static final double MAX_DISTANCE = 800;    // distance maximale pour shadowing
    private static final int REPORT_COOLDOWN = 10;     // délai entre broadcasts (en steps)
    private static final int TEAM = 0xBADDAD;
    private static final int ENEMY = 0xE11E;
    private static final int OVER = 0xC00010FF;

    /* ===================== ÉTATS ===================== */

    enum State {
        INIT,
        EXPLORE,
        REPORT,
        EVADE,
        SHADOW
    }

    private State state;

    /* ===================== VARIABLES ===================== */

    private double myX, myY;
    private boolean isMoving;
    private int whoAmI;  // 1 pour Bot1, 2 pour Bot2

    private double enemyX, enemyY;
    private double enemyDistance;
    private double enemyDirection;

    private boolean turnTask = false;
    private double targetHeading;

    private int reportCooldown = 0;
    private int enemyLostCounter = 0;  // compteur si ennemi perdu en SHADOW

    /* ===================== ACTIVATE ===================== */

    @Override
    public void activate() {
        state = State.INIT;
        isMoving = false;

        // Détection whoAmI via radar (corrige bug position)
        whoAmI = 1;  // assume Bot1 par défaut
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) {
                whoAmI = 2;
            }
        }

        // Position initiale basée sur whoAmI
        if (whoAmI == 1) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        } else {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
        }

        log("Activated as Bot" + whoAmI);
    }

    /* ===================== STEP ===================== */

    @Override
    public void step() {

        // Odométrie simple
        if (isMoving) {
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMoving = false;
        }

        // Scan radar toujours actif
        IRadarResult enemy = scanEnemy();

        // Priorité : finir un virage en cours
        if (turnTask) {
            if (isHeading(targetHeading)) {
                turnTask = false;
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
            return;
        }

        // Gestion cooldown broadcast
        if (reportCooldown > 0) {
            reportCooldown--;
        }

        switch (state) {

            case INIT:
                // Tourner vers NORTH (géré incrémentalement)
                stepTurn(Parameters.Direction.LEFT);
                if (isHeading(Parameters.NORTH)) {
                    state = State.EXPLORE;
                    log("Oriented North, switching to EXPLORE");
                }
                return;

            case EXPLORE:
                // Ennemi détecté ?
                if (enemy != null) {
                    computeEnemyPosition(enemy);
                    if (enemyTooClose(enemy)) {
                        state = State.EVADE;
                    } else {
                        state = State.REPORT;
                    }
                    return;
                }

                // Évitement obstacle (murs, wrecks, team)
                if (avoidObstacle()) {
                    return;
                }

                // Avancer
                moveForward();
                return;

            case REPORT:
                broadcastEnemy();  // avec cooldown interne
                state = State.EVADE;
                return;

            case EVADE:
                // Recul pour créer espace
                moveBack();
                state = State.SHADOW;
                log("Evaded, switching to SHADOW");
                return;

            case SHADOW:
                // Si main devant → turn right full
                if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot) {
                    startAvoidWall();  // utilise turnTask
                    return;
                }

                // Mise à jour ennemi
                if (enemy != null) {
                    computeEnemyPosition(enemy);
                    broadcastEnemy();  // avec cooldown
                    enemyLostCounter = 0;

                    // Maintenir distance
                    if (enemyTooClose(enemy)) {
                        moveBack();
                        log("Enemy too close, backing off");
                        return;
                    } else if (enemyDistance > MAX_DISTANCE) {
                        moveForward();
                        log("Enemy too far, approaching");
                        return;
                    }
                    // Sinon, rester immobile ou micro-ajustement (ici : immobile pour simplicité)

                } else {
                    enemyLostCounter++;
                    if (enemyLostCounter > 5) {  // délai avant retour EXPLORE
                        state = State.EXPLORE;
                        log("Enemy lost, back to EXPLORE");
                        return;
                    }
                }

                // Évitement obstacles
                if (avoidObstacle()) {
                    return;
                }

                // Ne pas bloquer mains (radar <400)
                for (IRadarResult r : detectRadar()) {
                    if (r.getObjectType() == IRadarResult.Types.TeamMainBot &&
                        r.getObjectDistance() < 400) {
                        log("Main too close, backing off");
                        moveBack();
                        return;
                    }
                }

                // Si rien, rester (ou random micro-move si besoin)
                return;
        }
    }

    /* ===================== MÉTHODES UTILITAIRES ===================== */

    private void log(String msg) {
        System.out.println("[SCOUT Bot" + whoAmI + "] " + msg);
        sendLogMessage("[SCOUT Bot" + whoAmI + "] " + msg);
    }

    private void moveForward() {
        isMoving = true;
        move();
    }
    @@@Override
    private void moveBack() {
        isMoving = true;  // odométrie gère le sens via heading
        moveBack();
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(normalizeHeading(getHeading()) - normalizeHeading(dir))) < ANGLE_PRECISION;
    }

    private double normalizeHeading(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(normalizeHeading(dir1) - normalizeHeading(dir2)) < ANGLE_PRECISION;
    }

    private IRadarResult scanEnemy() {
        ArrayList<IRadarResult> radar = detectRadar();
        for (IRadarResult r : radar) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                return r;
            }
        }
        return null;
    }

    private void computeEnemyPosition(IRadarResult enemy) {
        if (enemy == null) return;
        enemyDistance = enemy.getObjectDistance();
        enemyDirection = enemy.getObjectDirection();
        enemyX = myX + enemyDistance * Math.cos(enemyDirection);
        enemyY = myY + enemyDistance * Math.sin(enemyDirection);
    }

    private void broadcastEnemy() {
        if (reportCooldown > 0) return;
        broadcast("SCOUT:" + TEAM + ":" + ENEMY + ":" + (int) enemyX + ":" + (int) enemyY + ":" + OVER);
        log("Enemy reported at (" + (int) enemyX + "," + (int) enemyY + ")");
        reportCooldown = REPORT_COOLDOWN;
    }

    private boolean enemyTooClose(IRadarResult enemy) {
        return enemy != null && enemy.getObjectDistance() < SAFE_DISTANCE;
    }

    private boolean startAvoidWall() {
        IFrontSensorResult.Types front = detectFront().getObjectType();
        if (!turnTask && isDangerousFront(front)) {
            turnTask = true;
            targetHeading = normalizeHeading(getHeading() + Parameters.RIGHTTURNFULLANGLE);
            stepTurn(Parameters.Direction.RIGHT);
            return true;
        }
        return false;
    }

    private boolean avoidObstacle() {
        if (startAvoidWall()) {
            return true;
        }
        return false;
    }

    private boolean isDangerousFront(IFrontSensorResult.Types t) {
        return t == IFrontSensorResult.Types.WALL ||
               t == IFrontSensorResult.Types.Wreck ||
               t == IFrontSensorResult.Types.TeamMainBot ||
               t == IFrontSensorResult.Types.TeamSecondaryBot;
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: java/lang/Override#