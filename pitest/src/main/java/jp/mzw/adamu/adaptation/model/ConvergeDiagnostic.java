package jp.mzw.adamu.adaptation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bb.mcmc.analysis.GewekeConvergeStat;

public class ConvergeDiagnostic extends ModelBase {

	public static final int DEFAULT_N_MIN = 200;
	
	public static int getNMin(int N) {
		int min = DEFAULT_N_MIN;
		if (10000 < N) {
			min = 300;
		} else if (1000 < N) {
			min = 200;
		} else if (N < 1000){
			min = 100;
		}
		return min;
	}

	public static boolean converge(List<Double> values, int N) {
		return converge(toDoubleArray(values), N);
	}
	
	public static boolean converge(double[] values, int N) {
		// When sampling more than 50% of created mutants,
		// AdaMu makes a decision as RtMS time series become converged by design
		if (N * 0.5 < values.length) {
			return true;
		}
		
		// Determine sampling size for Geweke's convergence diagnostic
		// according to the number of created mutants
		int min_num_mutants = getNMin(N);
		
		// Ignore early mutants that test cases do not cover
		// because they are explicit biases causing invalid convergence diagnostic
		List<Double> manipulated_values = new ArrayList<>();
		boolean changed = false;
		double init_value = values[0];
		for (Double d : values) {
			if (!changed && d.equals(new Double(init_value))) {
				continue;
			} else if (!d.equals(init_value)) {
				changed = true;
			}
			manipulated_values.add(d);
		}
		// sampling
		List<Double> sampled_values = new ArrayList<>();
		int sample_sep = manipulated_values.size() / min_num_mutants;
		if (sample_sep == 0) sample_sep = 1;
		for (int i = 0; i < manipulated_values.size(); i++) {
			if (i % sample_sep == 0) {
				sampled_values.add(manipulated_values.get(i));
			}
		}
		
		// Converge diagnostic
		try {
			// Geweke's Nmin
			if (sampled_values.size() < min_num_mutants) {
				return false;
			}
			GewekeConvergeStat geweke = new GewekeConvergeStat();
			final String var_name = "adamu";
			geweke.setTestVariableName(new String[]{var_name});
			HashMap<String, double[]> sample_values = new HashMap<>();
			sample_values.put(var_name, toDoubleArray(sampled_values));
			geweke.updateValues(sample_values);
			geweke.calculateStatistic();
			boolean converge = geweke.haveAllConverged();
			return converge;
		} catch (JSci.maths.statistics.OutOfRangeException e) {
//			e.printStackTrace();
			return false;
		} catch (java.lang.IllegalArgumentException e) {
//			e.printStackTrace();
			return false;
		} catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
	
}
