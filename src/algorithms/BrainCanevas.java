package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;

public class BrainCanevas extends Brain {

    private boolean turningRight = false;
    private double targetHeading = 0;
    private static final double PRECISION = 0.002;

    public BrainCanevas() { super(); }

    public void activate() {
        move();

    }

    public void step() {

        IFrontSensorResult.Types front = detectFront().getObjectType();

        // --- Continue right turn until aligned ---
        if (turningRight) {
            if (isHeading(targetHeading)) {
                turningRight = false;
                return;
            }
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

        // --- Wall in front → turn right ---
        if (front == IFrontSensorResult.Types.WALL || front == IFrontSensorResult.Types.Wreck) {
            turningRight = true;
            targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

        // --- NOTHING IN FRONT → MOVE + MICRO-TURN RIGHT ---
        if (front == IFrontSensorResult.Types.NOTHING) {
            // micro correction to stay VERY close to wall
            stepTurn(Parameters.Direction.RIGHT); 
            move();
            return;
        }

        // --- NOTHING IN FRONT → MOVE + MICRO-TURN RIGHT ---
        if (front == IFrontSensorResult.Types.OpponentMainBot || front == IFrontSensorResult.Types.Wreck) {
            turningRight = true;
            targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

         // --- NOTHING IN FRONT → MOVE + MICRO-TURN RIGHT ---
        if (front == IFrontSensorResult.Types.OpponentSecondaryBot || front == IFrontSensorResult.Types.Wreck) {
            turningRight = true;
            targetHeading = getHeading() + Parameters.RIGHTTURNFULLANGLE;
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

        // Friendly bots → simply move forward
        move();
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < PRECISION;
    }
}
