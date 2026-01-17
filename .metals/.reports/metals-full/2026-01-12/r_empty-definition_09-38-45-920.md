error id: file://<HOME>/T%C3%A9l%C3%A9chargements/simovies20251201/src/algorithms/TeamAMainBotXXX.java:
file://<HOME>/T%C3%A9l%C3%A9chargements/simovies20251201/src/algorithms/TeamAMainBotXXX.java
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 8878
uri: file://<HOME>/T%C3%A9l%C3%A9chargements/simovies20251201/src/algorithms/TeamAMainBotXXX.java
text:
```scala
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class TeamAMainBotXXX extends Brain {

    /* ===================== ENUMS ===================== */

    enum Role { A, B, C }
    enum State {
        INIT,
        DEPLOY,
        ORIENT,
        SEARCH,
        AVOID_WALL,
        AVOID_TEAM,
        ALERT_ENEMY,
        LEADER_CIRCLE,
        JOIN_CIRCLE,
        FIRE,
        SACRIFICE,
        VICTORY,
        SURVIVAL
    }

    /* ===================== VARIABLES ===================== */

    private State state;
    private Role role;

    private int enemyLostCounter = 0;
    private static final int ENEMY_LOST_THRESHOLD = 50;

    private static boolean leaderExists = false;


    private double enemyX, enemyY;
    private boolean isLeader = false;
    private boolean sacrificeAssigned = false;

    private double healthThreshold = 0.3; // 30%

    /* ===================== ACTIVATE ===================== */

    @Override
    public void activate() {
        state = State.INIT;
    }

    /* ===================== STEP ===================== */

    @Override
    public void step() {

        /* ---- Messages ---- */
        handleMessages();

        switch (state) {

            /* ===== E0 INIT ===== */
            case INIT:
                assignRole();
                state = State.DEPLOY;
                break;

            /* ===== E1 DEPLOY ===== */
            case DEPLOY:
                deployByRole();
                if (deploymentDone()) {
                    state = State.ORIENT;
                }
                break;

            /* ===== E2 ORIENT ===== */
            case ORIENT:
                orientByTeam();
                if (orientationDone()) {
                    state = State.SEARCH;
                }
                break;

            /* ===== E3 SEARCH ===== */
            case SEARCH:
                if (detectEnemy()) {
                    state = State.ALERT_ENEMY;
                } else if (detectWall()) {
                    state = State.AVOID_WALL;
                } else if (detectTeammate()) {
                    state = State.AVOID_TEAM;
                } else {
                    move();
                }
                break;

            /* ===== E4 AVOID WALL ===== */
            case AVOID_WALL:
                avoidWall();
                state = State.SEARCH;
                break;

            /* ===== E5 AVOID TEAM ===== */
            case AVOID_TEAM:
                avoidTeammate();
                state = State.SEARCH;
                break;

            /* ===== E6 ALERT ENEMY ===== */
            case ALERT_ENEMY:
                if (!leaderExists) {
                    isLeader = true;
                    leaderExists = true;
                    broadcastEnemy();
                    state = State.LEADER_CIRCLE;
                } else {
                    state = State.JOIN_CIRCLE;
                }
                break;


            /* ===== E7 LEADER CIRCLE ===== */
            case LEADER_CIRCLE:
                circleEnemy();

                if (safeToFire()) {
                    fireAtEnemy();
                }
            
                if (enemyDead()) {
                    state = State.VICTORY;
                } else if (lowHealth() && !sacrificeAssigned) {
                    state = State.SACRIFICE;
                }
                break;
            

            /* ===== E8 JOIN CIRCLE ===== */
            case JOIN_CIRCLE:
                joinCircle();
                if (inCirclePosition()) {
                    state = State.FIRE;
                }
                break;

            /* ===== E11 FIRE ===== */
            case FIRE:
                if (safeToFire()) {
                    fireAtEnemy();
                }
                if (enemyDead()) {
                    state = State.VICTORY;
                } else if (alone()) {
                    state = State.SURVIVAL;
                }
                break;

            /* ===== E9 SACRIFICE ===== */
            case SACRIFICE:
                announceSacrifice();
                chargeEnemy();
                break;

            /* ===== E10 VICTORY ===== */
            case VICTORY:
                broadcastVictory();
                randomMove();
                state = State.SEARCH;
                break;

            /* ===== E12 SURVIVAL ===== */
            case SURVIVAL:
                survivalMode();
                break;
        }
    }

    /* ===================== BEHAVIORS ===================== */

    private void assignRole() {

        boolean teammateNorth = false;
        boolean teammateSouth = false;

        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.TeamMainBot ||
                r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {

                // Détection relative par direction
                if (Math.abs(r.getObjectDirection() - Parameters.NORTH) < 0.2) {
                    teammateNorth = true;
                }
                if (Math.abs(r.getObjectDirection() - Parameters.SOUTH) < 0.2) {
                    teammateSouth = true;
                }
            }
        }

        if (!teammateNorth) {
            role = Role.A;
            sendLogMessage("[ROLE] Assigned role A (North position)");
        } 
        else if (!teammateSouth) {
            role = Role.C;
            sendLogMessage("[ROLE] Assigned role C (South position)");
        } 
        else {
            role = Role.B;
            sendLogMessage("[ROLE] Assigned role B (Center position)");
        }
    }


    private void deployByRole() {

        switch (role) {

            case A:
                // Aller vers le NORD
                if (Math.abs(getHeading() - Parameters.NORTH) > Parameters.teamAMainBotStepTurnAngle) {
                    stepTurn(Parameters.Direction.LEFT);
                    sendLogMessage("[DEPLOY] Role A turning NORTH");
                } else {
                    move();
                    sendLogMessage("[DEPLOY] Role A moving NORTH");
                }
                break;

            case B:
                // Aller vers le SUD
                if (Math.abs(getHeading() - Parameters.SOUTH) > Parameters.teamAMainBotStepTurnAngle) {
                    stepTurn(Parameters.Direction.RIGHT);
                    sendLogMessage("[DEPLOY] Role B turning SOUTH");
                } else {
                    move();
                    sendLogMessage("[DEPLOY] Role B moving SOUTH");
                }
                break;

            case C:
                // Rester sur place
                sendLogMessage("[DEPLOY] Role C holding position");
                break;
        }
    }


    private boolean deploymentDone() {

        // Role C ne bouge pas → déploiement immédiat
        if (role == Role.C) {
            return true;
        }

        // Role A ou B : déploiement terminé lorsqu'on touche un mur
        return detectFront().getObjectType() == IFrontSensorResult.Types.WALL;
    }



private void orientByTeam() {

    if (Math.abs(getHeading() - Parameters.EAST) > Parameters.teamAMainBotStepTurnAngle) {
        stepTurn(Parameters.Direction.RIGHT);
    } else {
        move(); // ← IMPORTANT
    }
}


    private boolean orientationDone() {
        return Math.abs(getHeading() - Parameters.EAST) < Parameters.teamAMainBotStepTurnAngle;
    }


private boolean detectEnemy() {
    for (IRadarResult r : detectRadar()) {
        if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
            r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {

            if (isLeader || state == State.ALERT_ENEMY) {
                enemyX = r.getObjectDistance() * Math.cos(r.getObjectDirection());
                enemyY = r.getObjectDistance() * Math.sin(r.getObjectDirection());
            }
            return true;
        }
    }
    return false;
}


    private boolean detectWall() {
        return detectFront().getObjectType() == IFrontSensorResult.Types.WALL;
    }

    private boolean detectTeammate() {
        return detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot ||
               detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot;
    }

    private void avoidWall() {
        stepTurn(Math.random() < 0.5 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
    }

    private void avoidTeammate() {
        moveBack();
    }

    private void broadcastEnemy() {
        broadcast("ENEMY:" + enemyX + ":" + enemyY);
    }

private void joinCircle() {

    if (detectWall()) {
        stepTurn(Parameters.Direction.LEFT);
        return;
    }

    double@@ targetAngle = Math.atan2(enemyY, enemyX);
    double angleDiff = Math.sin(targetAngle - getHeading());

    if (Math.abs(angleDiff) > Parameters.teamAMainBotStepTurnAngle) {
        stepTurn(angleDiff > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        return;
    }

    move();
}

sendLogMessage("[CIRCLE] Moving toward enemy");

private void circleEnemy() {

    // Sécurité mur
    if (detectWall()) {
        stepTurn(Parameters.Direction.LEFT);
        return;
    }

    double distance = Math.hypot(enemyX, enemyY);

    if (!isLeader && distance < 600) {
        moveBack();
        return;
    }

    if (distance > 900) {
        move();
        return;
    }

    stepTurn(Parameters.Direction.LEFT);
    move();
}



    private boolean inCirclePosition() {
        double distance = Math.hypot(enemyX, enemyY);
        return distance > 600 && distance < 900;
    }


    private boolean safeToFire() {

        double fireAngle = Math.atan2(enemyY, enemyX);

        for (IRadarResult r : detectRadar()) {

            if (r.getObjectType() == IRadarResult.Types.TeamMainBot ||
                r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {

                double teammateAngle = r.getObjectDirection();

                // Si un teammate est trop proche de l’axe de tir → danger
                if (Math.abs(Math.sin(teammateAngle - fireAngle)) < 0.1) {
                    sendLogMessage("[FIRE] Friendly fire risk detected");
                    return false;
                }
            }
        }

        return true;
    }


    private void fireAtEnemy() {
        fire(Math.atan2(enemyY, enemyX));
    }

    private boolean enemyDead() {

        boolean enemySeen = false;

        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                enemySeen = true;
                break;
            }
        }

        if (enemySeen) {
            enemyLostCounter = 0;
            return false;
        } else {
            enemyLostCounter++;
            return enemyLostCounter > ENEMY_LOST_THRESHOLD;
        }
    }


    private boolean lowHealth() {
        return getHealth() < Parameters.teamAMainBotHealth * healthThreshold;
    }

    private boolean alone() {

        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.TeamMainBot ||
                r.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                return false;
            }
        }

        return true;
    }


    private void announceSacrifice() {
        broadcast("SACRIFICE");
        sacrificeAssigned = true;
    }

    private void chargeEnemy() {
        move();
    }

    private void broadcastVictory() {
        broadcast("ENEMY_DOWN");
    }

    private void randomMove() {
        stepTurn(Math.random() < 0.5 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        move();
    }

    private void survivalMode() {
        if (detectEnemy()) {
            fireAtEnemy();
        } else {
            avoidWall();
            move();
        }
    }

    private void handleMessages() {
        ArrayList<String> messages = fetchAllMessages();
        for (String msg : messages) {
            if (msg.startsWith("ENEMY") && !isLeader) {
                state = State.JOIN_CIRCLE;
            }
            if (msg.startsWith("SACRIFICE")) {
                sacrificeAssigned = true;
            }
            if (msg.startsWith("ENEMY_DOWN")) {
                state = State.SEARCH;
            }
        }
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 