package com.csl.cs108ademoapp.fragments;

import java.util.ArrayList;
import java.util.Random;

public class RFIDLocalization {


    private static final int NUM_PARTICLES = 1000;
    private ArrayList<Particle> particles = new ArrayList<>();
    private Random random = new Random();

    public RFIDLocalization() {
        initializeParticles();
    }

    private void initializeParticles() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle(randomPosition(), randomPosition()));
        }
    }

    public double[] update(double measuredRSSI, float[] rotationMatrix) {
        executeSCANCommand(measuredRSSI, rotationMatrix);
        double estimatedAngle = getEstimatedAngle();
        double estimatedDistance = getEstimatedDistance(measuredRSSI);

        return new double[]{estimatedAngle, estimatedDistance};
    }

    private void executeSCANCommand(double measuredRSSI, float[] rotationMatrix) {
        for (Particle particle : particles) {
            particle.updateWeight(measuredRSSI, rotationMatrix);
        }
    }

    private double getEstimatedAngle() {
        double totalWeight = 0;
        double weightedAngleSum = 0;
        for (Particle particle : particles) {
            totalWeight += particle.weight;
            weightedAngleSum += particle.rotation * particle.weight;
        }
        return weightedAngleSum / totalWeight;
    }

    private double getEstimatedDistance(double measuredRSSI) {
        double measuredPower = -59; // Replace with the RSSI value at 1 meter distance
        double n = 2; // Path-loss exponent, change according to your environment
        return Math.pow(10, (measuredPower - measuredRSSI) / (10 * n));
    }

    private double randomPosition() {
        return random.nextDouble() * 100;  // Assuming a 100x100 area for simplicity
    }

    class Particle {
        double x, y;
        double rotation;
        double weight;

        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            this.rotation = randomRotation();
            this.weight = 1.0 / NUM_PARTICLES;
        }

        void updateWeight(double measuredRSSI, float[] rotationMatrix) {
            // Update the weight based on the difference between measured and estimated values.
            // For simplicity, we're using only the first element of the rotation matrix.
            // The weight is updated based on how close the particle's rotation is to the measured rotation.
            weight = 1.0 / (Math.abs(rotationMatrix[0] - rotation) + 1);
        }


        double estimateRSSI(double x, double y) {
            return -Math.sqrt(x * x + y * y);
        }

        double randomRotation() {
            return random.nextDouble() * 2 * Math.PI;  // Random rotation between 0 and 2*PI
        }
    }
}
