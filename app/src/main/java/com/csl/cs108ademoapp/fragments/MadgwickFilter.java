package com.csl.cs108ademoapp.fragments;

public class MadgwickFilter {
    private float beta;  // filter gain
    private int sampleFrequency = 840;
    private float q0, q1, q2, q3;  // quaternion elements

    public MadgwickFilter(float sampleFrequency, float beta) {
        this.beta = beta;
        this.q0 = 1;
        this.q1 = 0;
        this.q2 = 0;
        this.q3 = 0;
    }

    public void update(float gx, float gy, float gz, float ax, float ay, float az) {
        float recipNorm;
        float s0, s1, s2, s3;
        float qDot1, qDot2, qDot3, qDot4;
        float hx, hy;
        float _2bx, _2bz;
        float _4bx, _4bz;
        float _2q0mx, _2q0my, _2q0mz;
        float _2q1mx;
        float _2q0, _2q1, _2q2, _2q3;
        float _4q0, _4q1, _4q2;
        float _8q1, _8q2;
        float q0q0, q0q1, q0q2, q0q3;
        float q1q1, q1q2, q1q3;
        float q2q2, q2q3;
        float q3q3;

        // Rate of change of quaternion from gyroscope
        qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
        qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
        qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
        qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

        // Compute feedback only if accelerometer measurement valid
        if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {
            // Normalize accelerometer measurement
            recipNorm = (float) (1.0 / Math.sqrt(ax * ax + ay * ay + az * az));
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // Auxiliary variables to avoid repeated arithmetic
            _2q0 = 2.0f * q0;
            _2q1 = 2.0f * q1;
            _2q2 = 2.0f * q2;
            _2q3 = 2.0f * q3;
            _4q0 = 4.0f * q0;
            _4q1 = 4.0f * q1;
            _4q2 = 4.0f * q2;
            _8q1 = 8.0f * q1;
            _8q2 = 8.0f * q2;
            q0q0 = q0 * q0;
            q0q1 = q0 * q1;
            q0q2 = q0 * q2;
            q0q3 = q0 * q3;
            q1q1 = q1 * q1;
            q1q2 = q1 * q2;
            q1q3 = q1 * q3;
            q2q2 = q2 * q2;
            q2q3 = q2 * q3;
            q3q3 = q3 * q3;

            // Gradient decent algorithm corrective step
            s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
            s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * q1 - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
            s2 = 4.0f * q0q0 * q2 + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
            s3 = 4.0f * q1q1 * q3 - _2q1 * ax + 4.0f * q2q2 * q3 - _2q2 * ay;

            // Normalize step magnitude
            recipNorm = (float) (1.0 / Math.sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3));
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // Apply feedback step
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // Integrate rate of change of quaternion to yield quaternion
        q0 += qDot1 * (1.0f / sampleFrequency);
        q1 += qDot2 * (1.0f / sampleFrequency);
        q2 += qDot3 * (1.0f / sampleFrequency);
        q3 += qDot4 * (1.0f / sampleFrequency);

        // Normalize quaternion
        recipNorm = (float) (1.0 / Math.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3));
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;
    }

    public float[] getQuaternion() {
        return new float[]{q0, q1, q2, q3};
    }
}
