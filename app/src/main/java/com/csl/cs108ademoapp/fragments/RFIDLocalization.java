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
            particles.add(new Particle(randomPosition(), randomPosition(), randomRotation()));
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
        double measuredPower = -59;
        double n = 2;
        return Math.pow(10, (measuredPower - measuredRSSI) / (10 * n));
    }

    private double randomPosition() {
        return random.nextDouble() * 100;
    }

    private double randomRotation() {
        return random.nextDouble() * 2 * Math.PI;
    }

    class Particle {
        double x, y;
        double rotation;
        double weight;

        Particle(double x, double y, double rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.weight = 1.0 / NUM_PARTICLES;
        }

        void updateWeight(double measuredRSSI, float[] rotationMatrix) {
            double estimatedRSSI = -Math.sqrt(x * x + y * y);
            double diffRSSI = Math.abs(measuredRSSI - estimatedRSSI);
            double diffRotation = Math.abs(rotationMatrix[0] - rotation);
            this.weight = Math.exp(-diffRSSI) * Math.exp(-diffRotation);
        }
    }

}
