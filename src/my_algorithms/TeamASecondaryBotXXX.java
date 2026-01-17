package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class TeamASecondaryBotXXX extends Brain {

    /* ===================== ENUMS ===================== */

    enum State {
        INIT,
        PATROL,
        AVOID_WALL,
        AVOID_TEAM,
        DETECT_ENEMY,
        ESCAPE,
        HARASS,
        RETREAT
    }

    /* ===================== VARIABLES ===================== */

    private State state;

    private double enemyX, enemyY;
    private double enemyDistance;
    private double safeDistance = 500;   // Secondary must keep distance
    private double dangerDistance = 300; // Too close → escape
    private double healthThreshold = 0.3;

    /* ===================== ACTIVATE ===================== */

    @Override
    public void activate() {
        state = State.INIT;
    }

    /* ===================== STEP ===================== */

    @Override
    public void step() {

        switch (state) {

            /* ===== S0 INIT ===== */
            case INIT:
                state = State.PATROL;
                break;

            /* ===== S1 PATROL ===== */
            case PATROL:
                patrolMove();

                if (detectEnemy()) {
                    state = State.DETECT_ENEMY;
                } else if (detectWall()) {
                    state = State.AVOID_WALL;
                } else if (detectTeammate()) {
                    state = State.AVOID_TEAM;
                }
                break;

            /* ===== S2 AVOID WALL ===== */
            case AVOID_WALL:
                avoidWall();
                state = State.PATROL;
                break;

            /* ===== S4 AVOID TEAM ===== */
            case AVOID_TEAM:
                avoidTeammate();
                state = State.PATROL;
                break;

            /* ===== S3 DETECT ENEMY ===== */
            case DETECT_ENEMY:
                broadcastEnemy();

                if (enemyDistance < dangerDistance) {
                    state = State.ESCAPE;
                } else {
                    state = State.HARASS;
                }
                break;

            /* ===== S5 ESCAPE ===== */
            case ESCAPE:
                escapeEnemy();

                if (enemyDistance > safeDistance) {
                    state = State.HARASS;
                }
                break;

            /* ===== S6 HARASS ===== */
            case HARASS:
                harassEnemy();

                if (lowHealth()) {
                    state = State.RETREAT;
                } else if (!detectEnemy()) {
                    state = State.PATROL;
                } else if (enemyDistance < dangerDistance) {
                    state = State.ESCAPE;
                }
                break;

            /* ===== S7 RETREAT ===== */
            case RETREAT:
                retreat();

                if (!detectEnemy()) {
                    state = State.PATROL;
                }
                break;
        }
    }

    /* ===================== BEHAVIORS ===================== */

    private void patrolMove() {
        // Fast zig-zag exploration
        if (Math.random() < 0.1) {
            stepTurn(Math.random() < 0.5 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        }
        move();
    }

    private boolean detectEnemy() {
        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {

                enemyDistance = r.getObjectDistance();
                enemyX = r.getObjectDistance() * Math.cos(r.getObjectDirection());
                enemyY = r.getObjectDistance() * Math.sin(r.getObjectDirection());
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

    private void escapeEnemy() {
        moveBack();
        stepTurn(Math.random() < 0.5 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
    }

    private void harassEnemy() {
        // Keep distance and shoot occasionally
        stepTurn(Parameters.Direction.LEFT);
        move();

        if (Math.random() < 0.2) {
            fire(Math.atan2(enemyY, enemyX));
        }
    }

    private void retreat() {
        moveBack();
        stepTurn(Math.random() < 0.5 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
    }

    private boolean lowHealth() {
        return getHealth() < Parameters.teamASecondaryBotHealth * healthThreshold;
    }
}
