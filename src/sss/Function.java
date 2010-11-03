package sss;

import jamasam.*;

import java.util.*;


public interface Function<Result, Arg> {

	/**
	 * Assumes column vectors, therefore returning a single row.
	 */
	public static final class Distance {
		public Matrix call(Matrix x, Matrix y) {
			Matrix dist = new Matrix(1, x.getColumnDimension());
			for (int j = 0; j < x.getColumnDimension(); j++) {
				double total = 0;
				for (int i = 0; i < x.getRowDimension(); i++) {
					double diff = x.get(i, j) - y.get(i, j);
					total += diff * diff;
				}
				dist.set(0, j, Math.sqrt(total));
			}
			return dist;
		}
	}

	public static final class Difference {
		public Matrix call(Matrix x, Matrix y) {
			return x.minus(y);
		}
	}

	Result call(List<Arg> args);

}
