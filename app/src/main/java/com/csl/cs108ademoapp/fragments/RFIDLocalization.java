package com.csl.cs108ademoapp.fragments;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class RFIDLocalization {
    private static final int WINDOW_SIZE = 5;  // Size of the moving average window
    private Queue<Double> rssiWindow = new LinkedList<>();
    private double prevRSSI = 0;  // Initialize with a reasonable value
    private double prevAngle = 0;  // Initialize with a starting angle
    private double estimatedDistance = 0;
    private double velocityX = 0;
    private double velocityY = 0;

    // Update the estimated angle and distance based on the new RSSI reading, rotation matrix, and linear acceleration
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

        // Convert the rotation matrix to an angle (assuming rotationMatrix[0] is the angle in radians)
        double currentAngle = rotationMatrix[0];

        double rssiRateOfChange = avgRSSI - prevRSSI;
        double angleRateOfChange = currentAngle - prevAngle;

        // If the RSSI is increasing and the angle is changing, assume the tag is in that direction
        if (rssiRateOfChange > 0 && angleRateOfChange != 0) {
            prevAngle = currentAngle;
        }

        // Calculate the relative angle by which the device needs to rotate to point towards the RFID tag
        double relativeAngle = prevAngle - currentAngle;

        // Update the estimated distance
        estimatedDistance = getEstimatedDistance(avgRSSI);

        prevRSSI = avgRSSI;

        return new double[]{relativeAngle, estimatedDistance};
    }

    // Function to convert RSSI to distance
    private double getEstimatedDistance(double avgRSSI) {
        double measuredPower = -59;  // Replace with a configurable value
        double n = 2;  // Replace with a configurable value
        return Math.pow(10, (measuredPower - avgRSSI) / (10 * n));
    }
}
