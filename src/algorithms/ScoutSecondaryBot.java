package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;

public class ScoutSecondaryBot extends Brain {

    private static final double ANGLE_EPS = 0.05;
    private static final int REPORT_COOLDOWN = 8;

    private static final int TEAM = 0xBADDAD;
    private static final int ENEMY = 0xE11E;
    private static final int OVER = 0xC00010FF;

    private int cooldown = 0;
    private int whoAmI = 1;

    // 🔒 Verrou de rotation
    private boolean turning = false;
    private double targetHeading = 0;

    @Override
    public void activate() {
        for (IRadarResult r : detectRadar()) {
            if (sameDir(r.getObjectDirection(), Parameters.NORTH)) {
                whoAmI = 2;
            }
        }
        log("Activated – Scout Bot " + whoAmI);
    }

    @Override
    public void step() {

        if (cooldown > 0) cooldown--;

        // 🔁 Si on est en train de tourner → priorité absolue
        if (turning) {
            if (sameDir(getHeading(), targetHeading)) {
                turning = false;
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
            return;
        }

        // 🔍 Scanner ennemis
        scanAndReportEnemies();

        // 👁️ Détection frontale
        IFrontSensorResult front = detectFront();

        if (front.getObjectType() == IFrontSensorResult.Types.NOTHING) {
            move();
        } else {
            // 🚧 Obstacle → virage COMPLET
            turning = true;
            targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.RIGHT);
            log("Obstacle → rotation engagée");
        }
    }

    // ─────────────────────────────
    private void scanAndReportEnemies() {
        for (IRadarResult r : detectRadar()) {
            if (r.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                r.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {

                if (cooldown == 0) {
                    double ex = r.getObjectDistance() * Math.cos(r.getObjectDirection());
                    double ey = r.getObjectDistance() * Math.sin(r.getObjectDirection());

                    broadcast("SCOUT:" + TEAM + ":" + ENEMY + ":" +
                              (int) ex + ":" + (int) ey + ":" + OVER);

                    log("Ennemi vu → broadcast");
                    cooldown = REPORT_COOLDOWN;
                }
                return;
            }
        }
    }

    private boolean sameDir(double a, double b) {
        return Math.abs(Math.sin(a - b)) < ANGLE_EPS;
    }

    private void log(String msg) {
        String p = "[SCOUT Bot" + whoAmI + "] ";
        System.out.println(p + msg);
        sendLogMessage(p + msg);
    }
}
