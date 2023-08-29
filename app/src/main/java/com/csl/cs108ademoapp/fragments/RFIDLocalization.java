package com.csl.cs108ademoapp.fragments;

/*import java.util.ArrayList;*/
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class RFIDLocalization {

    private static final int WINDOW_SIZE = 5;
    private Queue<Double> rssiWindow = new LinkedList<>();
    private double prevRSSI = Double.NEGATIVE_INFINITY;
    private double bestAngle = 0;
    private double bestRSSI = Double.POSITIVE_INFINITY;
    private double estimatedDistance = 0;
    private double velocityX = 0;
    private double velocityY = 0;

    public double[] update(double measuredRSSI, float[] rotationMatrix, double linearAccelerationX, double linearAccelerationY, double deltaTime) {
        // Update the moving average of the RSSI
        if (rssiWindow.size() >= WINDOW_SIZE) {
            rssiWindow.poll();
        }
        rssiWindow.add(measuredRSSI);
        double avgRSSI = rssiWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // Update velocity based on linear acceleration and time delta
        velocityX += linearAccelerationX * deltaTime;
        velocityY += linearAccelerationY * deltaTime;

        // Extract yaw (azimuth) from rotation matrix
        double currentAngle = Math.atan2(rotationMatrix[1], rotationMatrix[0]);

        // Update best angle and best RSSI
        if (avgRSSI < bestRSSI) {
            bestRSSI = avgRSSI;
            bestAngle = currentAngle;
        }

        // Calculate the relative angle
        double relativeAngle = bestAngle - currentAngle;

        // Update the estimated distance based on RSSI and velocity
        estimatedDistance = getEstimatedDistance(avgRSSI, Math.sqrt(velocityX * velocityX + velocityY * velocityY));

        prevRSSI = avgRSSI;

        return new double[]{relativeAngle, estimatedDistance};
    }

    // Function to convert RSSI and velocity to distance
    private double getEstimatedDistance(double avgRSSI, double velocityMagnitude) {
        double measuredPower = -59;  // Replace with a configurable value
        double n = 2;  // Replace with a configurable value
        double basicDistance = Math.pow(10, (measuredPower - avgRSSI) / (10 * n));

        // Modify the estimated distance based on the velocity magnitude
        return basicDistance * (1 + 0.1 * velocityMagnitude);
    }
}