package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

import java.util.ArrayList;

public class TeamAMainBotXXX extends Brain {

    enum State {
        SEARCH,
        AVOID,
        AIM,
        FIRE
    }

    private State state = State.SEARCH;

    private boolean turning = false;
    private double targetHeading;

    private IRadarResult currentEnemy = null;

    private static final double ANGLE_EPS = 0.05;

    @Override
    public void activate() {
        log("Main bot activated");
    }

    @Override
    public void step() {

        // 🔒 Rotation bloquante
        if (turning) {
            if (sameDir(getHeading(), targetHeading)) {
                turning = false;
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
            return;
        }

        // 🔍 Scanner ennemi
        currentEnemy = scanEnemy();
        if (currentEnemy != null && state != State.AIM && state != State.FIRE) {
            state = State.AIM;
        }

        // 🚧 Mur
        if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING &&
            state != State.AIM && state != State.FIRE) {
            state = State.AVOID;
        }

        switch (state) {

            case SEARCH:
                move();
                break;

            case AVOID:
                startAvoid();
                break;

            case AIM:
                aimAtEnemy();
                break;

            case FIRE:
                fireAtEnemy();
                break;
        }
    }

    /* ================= ENEMY ================= */

    private IRadarResult scanEnemy() {
        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                return r;
            }
        }
        return null;
    }

    private void aimAtEnemy() {
        if (currentEnemy == null) {
            state = State.SEARCH;
            return;
        }

        double diff = Math.sin(currentEnemy.getObjectDirection() - getHeading());

        if (Math.abs(diff) > ANGLE_EPS) {
            stepTurn(diff > 0 ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
        } else {
            state = State.FIRE;
        }
    }

    private void fireAtEnemy() {
        if (currentEnemy != null) {
            fire(currentEnemy.getObjectDirection());
            log("🔥 FIRING");
        }
        state = State.SEARCH;
    }

    /* ================= AVOID ================= */

    private void startAvoid() {
        turning = true;
        targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
        stepTurn(Parameters.Direction.RIGHT);
        state = State.SEARCH;
        log("Avoiding obstacle");
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
