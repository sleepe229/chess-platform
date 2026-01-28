package com.chess.user.algorithm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Glicko-2 rating system implementation
 * Based on: http://www.glicko.net/glicko/glicko2.pdf
 */
@Component
public class Glicko2Calculator {

    private static final double TAU = 0.5;        // System constant (volatility)
    private static final double EPSILON = 0.000001;
    private static final double SCALE = 173.7178; // Glicko-2 scale factor

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingResult {
        private double rating;
        private double ratingDeviation;
        private double volatility;
    }

    public RatingResult calculateNewRating(
            double rating,
            double rd,
            double volatility,
            double opponentRating,
            double opponentRd,
            double score) {

        // Step 1: Convert to Glicko-2 scale
        double mu = (rating - 1500) / SCALE;
        double phi = rd / SCALE;
        double opponentMu = (opponentRating - 1500) / SCALE;
        double opponentPhi = opponentRd / SCALE;

        // Step 2: Compute the estimated variance
        double g = g(opponentPhi);
        double e = E(mu, opponentMu, opponentPhi);
        double v = 1.0 / (g * g * e * (1 - e));

        // Step 3: Compute the improvement in rating
        double delta = v * g * (score - e);

        // Step 4: Compute the new volatility
        double a = Math.log(volatility * volatility);
        Function f = x -> {
            double ex = Math.exp(x);
            double num1 = ex * (delta * delta - phi * phi - v - ex);
            double num2 = 2 * Math.pow(phi * phi + v + ex, 2);
            double num3 = (x - a) / (TAU * TAU);
            return num1 / num2 - num3;
        };

        double A = a;
        double B;

        if (delta * delta > phi * phi + v) {
            B = Math.log(delta * delta - phi * phi - v);
        } else {
            int k = 1;
            while (f.apply(a - k * TAU) < 0) {
                k++;
            }
            B = a - k * TAU;
        }

        double fA = f.apply(A);
        double fB = f.apply(B);

        while (Math.abs(B - A) > EPSILON) {
            double C = A + (A - B) * fA / (fB - fA);
            double fC = f.apply(C);

            if (fC * fB < 0) {
                A = B;
                fA = fB;
            } else {
                fA = fA / 2;
            }

            B = C;
            fB = fC;
        }

        double newVolatility = Math.exp(A / 2);

        // Step 5: Update rating deviation
        double phiStar = Math.sqrt(phi * phi + newVolatility * newVolatility);

        // Step 6: Update rating and RD
        double phiPrime = 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / v);
        double muPrime = mu + phiPrime * phiPrime * g * (score - e);

        // Step 7: Convert back to original scale
        double newRating = muPrime * SCALE + 1500;
        double newRd = phiPrime * SCALE;

        return RatingResult.builder()
                .rating(newRating)
                .ratingDeviation(newRd)
                .volatility(newVolatility)
                .build();
    }

    private double g(double phi) {
        return 1.0 / Math.sqrt(1 + 3 * phi * phi / (Math.PI * Math.PI));
    }

    private double E(double mu, double opponentMu, double opponentPhi) {
        return 1.0 / (1 + Math.exp(-g(opponentPhi) * (mu - opponentMu)));
    }

    @FunctionalInterface
    private interface Function {
        double apply(double x);
    }
}
