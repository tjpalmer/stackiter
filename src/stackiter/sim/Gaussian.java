package stackiter.sim;

import static java.lang.Math.*;

/**
 * Represents a Gaussian PDF.
 */
public class Gaussian {

	private static final double INV_SQRT_TAU = 1.0 / sqrt(2.0 * Math.PI);

	private double mean;

	private double normalizer;

	private double spreader;

	public Gaussian() {
		this(0.0, 1.0);
	}

	public Gaussian(double mean, double deviation) {
		this.mean = mean;
		// Various forms of deviation.
		// TODO Provide access to mean and original deviation?
		normalizer = INV_SQRT_TAU / deviation;
		spreader = -0.5 / (deviation * deviation);
	}

	public double density(double x) {
		double difference = x - mean;
		double result = normalizer * exp(spreader * difference * difference);
		return result;
	}

}
