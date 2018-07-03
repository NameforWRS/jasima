/*******************************************************************************
 * This file is part of jasima, v1.3, the Java simulator for manufacturing and 
 * logistics.
 *  
 * Copyright (c) 2015 		jasima solutions UG
 * Copyright (c) 2010-2015 Torsten Hildebrandt and jasima contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package jasima.core.experiment;

import jasima.core.experiment.Experiment.UniqueNamesCheckingHashMap;
import jasima.core.experiment.OCBAExperiment.ProblemType;
import jasima.core.random.continuous.DblConst;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.Simulation.SimEvent;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.ConsolePrinter;
import jasima.core.util.EJobShopObjectives;
import jasima.core.util.Util;
import jasima.core.util.observer.NotifierListener;
import jasima.shopSim.core.JobShopExperiment;
import jasima.shopSim.core.PR;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment.Scenario;
import jasima.shopSim.prioRules.basic.FASFS;
import jasima.shopSim.prioRules.basic.SPT;
import jasima.shopSim.prioRules.gp.MyCustomGP;
import jasima.shopSim.prioRules.meta.IgnoreFutureJobs;
import jasima.shopSim.prioRules.upDownStream.WINQ;
import jasima.shopSim.util.BasicJobStatCollector;
import jasima.shopSim.util.DeviationJobStatCollector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import ec.app.batch.JavaCallCPlus;
import ec.util.Output;

public class AOAPNewRun extends BaseEvaluator {

	public enum ProblemType {
		MINIMIZE, MAXIMIZE
	};

	// Dynamic JobShop parameters
	public int NumberOfJobs = 2500;
	public int NumberOfMachines = 10;
	public int NumberOfOperations = 10;
	public double DuedateFactor = 4.0d;
	public double UtilizationLevel = 0.95d;
	public int MaxJobsInSystem = 500;
	public int InitialBudget = 5;
	protected Map<String, Object> resultMap = new UniqueNamesCheckingHashMap();;
	//
	// experiment parameters
	//
	private ProblemType problemType = ProblemType.MINIMIZE;
	private EJobShopObjectives objective = EJobShopObjectives.MeanFlowTime;
	private int minReplicationsPerConfiguration = 5;
	private int numReplications = 10;
	private double pcsLevel = 0.0;
	private boolean detailedResults = true;
	public int NumberOfDR = 0;
	public long InitilizeSeed = 20111205;
	private int numTaskExecuted = 0;
	protected Random seedStream;
	public int objectives = 1;

	//
	// fields used during experiment run
	//

	private int totalBudget, iterationBudget, budgetUsed;
	private ArrayList<MultipleReplicationExperiment> configurations;
	public ArrayList<String> DispatchingRules;
	public SummaryStat[] stats;
	private double finalPCS;
	private int currBest;
	public int[] newRuns;
	
	public int warmupStageBudget = 10;
	public int firstStageBudget = 5, firstStageTotalBudget;
	
	public AOAPNewRun() {
		DispatchingRules = new ArrayList<String>();
		configurations = new ArrayList<MultipleReplicationExperiment>();
		seedStream = null;
	
		}

	public void AddDispatchingRule(String pr) throws CloneNotSupportedException {
		
		this.DispatchingRules.add(pr);

		NumberOfDR++;
	}

	public void init()
	{
		numTaskExecuted = 0;
		newRuns = new int[NumberOfDR];
		iterationBudget = 0;
	}
	
	public void init(int wb, int fb, int ib) {
		
		numTaskExecuted = 0;
		budgetUsed = 0;
		newRuns = new int[NumberOfDR];
		iterationBudget = ib;
		warmupStageBudget = wb;
		firstStageBudget = fb;

		totalBudget = this.NumberOfDR * warmupStageBudget;
		firstStageTotalBudget = this.NumberOfDR * firstStageBudget;
	}
	
	//Initialization of OCBARun
	public DynamicShopExperiment initBaseExperiment(String pr) {		
		
		DynamicShopExperiment baseExperiment = new DynamicShopExperiment();		
		return baseExperiment;
	}

	protected long getExperimentSeed() {
	
			if (seedStream == null)
				seedStream = new Random(InitilizeSeed);
			return seedStream.nextLong();
		
	}
	
	protected void doOneRun() {

		int index = this.CalculateBestIndex();

		MultipleReplicationExperiment mre = this.configurations.get(index);
		mre.setMaxReplications(1);
		mre.setInitialSeed(this.getExperimentSeed());
		//run evaluation
		JavaCallCPlus shp1 = new JavaCallCPlus();  
		double obj=shp1.getobj(mre.getPR(), this.InitilizeSeed);
		SummaryStat test1=new SummaryStat();
		this.stats[index].combine(test1.value(obj));			

		budgetUsed++;
	}
	
	protected void doRankingAndSelection() {
		while (this.hasMoreTasks()) {
			this.doOneRun();
		}
	}
	
	protected void doWarmup() {
		if (stats == null)
			stats = Util.initializedArray(this.NumberOfDR, SummaryStat.class);
		else
			return;

		int i = 0;
		for (String pr : this.DispatchingRules) {
			//这里需要把种子和pr传到C++中
			DynamicShopExperiment baseExperiment = this.initBaseExperiment(pr);
			MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
			mre.setInitialSeed(InitilizeSeed);
			mre.setMaxReplications(iterationBudget);
			mre.setBaseExperiment(baseExperiment);
			newRuns[i] = iterationBudget;
			i++;
		}
	}
	
	protected void createExperiments() {

		if (numTaskExecuted == 0) {
		
			
			totalBudget = this.NumberOfDR * getNumReplications();

			// set iteration budget based on total number of configurations
			//iterationBudget = Math.round(0.1f * this.getNumReplications());
			// min. replications for a configuration
			if (iterationBudget < getMinReplicationsPerConfiguration())
				iterationBudget = getMinReplicationsPerConfiguration();		
			
			if(stats != null)
				iterationBudget = 1;

			budgetUsed = 0;

			int i = 0;
			for(String pr : this.DispatchingRules)
			{
				DynamicShopExperiment baseExperiment = this.initBaseExperiment(pr);
				MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
				mre.setInitialSeed(InitilizeSeed);
				mre.setMaxReplications(iterationBudget);
				mre.setBaseExperiment(baseExperiment);
				newRuns[i] = iterationBudget;
				i++;
				configurations.add(mre);
			}	
			
			if(stats == null)
				stats = Util.initializedArray(this.NumberOfDR, SummaryStat.class);
			else
				numTaskExecuted = 1;
		}	
	}
	
	public void  performOneRun()
	{
		
		long curSeed = this.InitilizeSeed;
		
		if(numTaskExecuted > 0)
		{
			curSeed = this.getExperimentSeed();
			
			int index = this.CalculateBestIndex();
			//int index = 0;
			
			MultipleReplicationExperiment mre = this.configurations.get(index);
			
			//step one
			mre.setMaxReplications(1);
			
			//new seed
			mre.setInitialSeed(curSeed);
						
			//run evaluation
			JavaCallCPlus shp1 = new JavaCallCPlus();  
			double obj=shp1.getobj(mre.getPR(), curSeed);
			SummaryStat test1=new SummaryStat();
			this.stats[index].combine(test1.value(obj));			
			budgetUsed++;
		}
		else
		{
			for(int i = 0; i < this.NumberOfDR; i++)
			{			
				MultipleReplicationExperiment mre = this.configurations.get(i);
							
				//run evaluation			
				JavaCallCPlus shp1 = new JavaCallCPlus();  
				double obj=shp1.getobj(mre.getPR(), this.InitilizeSeed);
				SummaryStat test1=new SummaryStat();
				this.stats[i].combine(test1.value(obj));	
				budgetUsed += mre.getMaxReplications();
				System.out.println("budgetUsed=" + budgetUsed  + " total Budget=" + this.totalBudget + " Init");
			}
		}		
		System.out.println("budgetUsed=" + budgetUsed  + " total Budget=" + this.totalBudget);
		
		numTaskExecuted++;
	}
	
	public String printResultsToString()
	{
		StringBuilder outputBuilder = new StringBuilder();
		ArrayList<String> valStatNames = new ArrayList<String>();
		ArrayList<String> otherNames = new ArrayList<String>();

		for (String k : resultMap.keySet()) {
			if (k != Experiment.RUNTIME) {
				Object v = resultMap.get(k);
				if (v instanceof SummaryStat) {
					valStatNames.add(k);
				} else {
					otherNames.add(k);
				}
			}
		}

		// sort by name, ignoring upper and lower case
		Collections.sort(valStatNames, new Comparator<String>() {

			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});
		Collections.sort(otherNames, new Comparator<String>() {

			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		// output ValueStat-objects
		if (valStatNames.size() > 0) {			
			outputBuilder.append("Name\tMean\tMin\tMax\tStdDev\tCount\tSum");

			for (String k : valStatNames) {
				SummaryStat vs = (SummaryStat) resultMap.get(k);
				outputBuilder.append(String.format(
						"%s\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%.4f%n", k, vs.mean(),
						vs.min(), vs.max(), vs.stdDev(), vs.numObs(), vs.sum()));
			}
		}

		// output all other objects (except runtime)
		if (otherNames.size() > 0) {
			outputBuilder.append("\n");
			outputBuilder.append("Name\tValue");

			for (String k : otherNames) {
				Object v = resultMap.get(k);
				if (v != null) {
					if (v.getClass().isArray())
						v = Util.arrayToString(v);
				
				}
				outputBuilder.append("\n" + k + "\t" + v);
			}
		}

		return outputBuilder.toString();
	}

	public void produceResults() {
		
		finalPCS = calcPCS();		
	
		resultMap.put("bestIndex", currBest);
		resultMap.put("bestPerformance", stats[currBest].mean());

		resultMap.put("numEvaluations", budgetUsed);
		resultMap.put("pcs", finalPCS);

		if (isDetailedResults()) {
			// allocation of evaluations to configurations
			int[] numRuns = new int[configurations.size()];
			double[] means = new double[stats.length];
			Experiment[] exps = new Experiment[configurations.size()];
			for (int i = 0; i < configurations.size(); i++) {
				exps[i] = configurations.get(i).getBaseExperiment();

				SummaryStat vs = stats[i];
				numRuns[i] = vs.numObs();
				means[i] = vs.mean();
			}
			resultMap.put("allocationVector", numRuns);
			resultMap.put("meansVector", means);
			//resultMap.put("configurations", exps);
			// probability of configuration assumed being best to be better than
			// another configuration
			resultMap.put("probBestBetter", calcPCSPriosPerConfiguration());
			resultMap.put("rank", findRank(means));
		}
	}	
	
public void printResults(File statisticsFile) {
	
	 	FileWriter fw;
		try {
			fw = new FileWriter(statisticsFile);
			
			PrintWriter out = new PrintWriter(fw, true);
			ArrayList<String> valStatNames = new ArrayList<String>();
			ArrayList<String> otherNames = new ArrayList<String>();

			for (String k : resultMap.keySet()) {
				if (k != Experiment.RUNTIME) {
					Object v = resultMap.get(k);
					if (v instanceof SummaryStat) {
						valStatNames.add(k);
					} else {
						otherNames.add(k);
					}
				}
			}

			// sort by name, ignoring upper and lower case
			Collections.sort(valStatNames, new Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareToIgnoreCase(s2);
				}
			});
			Collections.sort(otherNames, new Comparator<String>() {

				public int compare(String s1, String s2) {
					return s1.compareToIgnoreCase(s2);
				}
			});

			// output ValueStat-objects
			if (valStatNames.size() > 0) {
				out.println();
				out.println("Name\tMean\tMin\tMax\tStdDev\tCount\tSum");

				for (String k : valStatNames) {
					SummaryStat vs = (SummaryStat) resultMap.get(k);
					out.printf(Util.DEF_LOCALE,
							"%s\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%.4f%n", k, vs.mean(),
							vs.min(), vs.max(), vs.stdDev(), vs.numObs(), vs.sum());
				}
			}

			// output all other objects (except runtime)
			if (otherNames.size() > 0) {
				out.println();
				out.println("Name\tValue");

				for (String k : otherNames) {
					Object v = resultMap.get(k);
					if (v != null) {
						if (v.getClass().isArray())
							v = Util.arrayToString(v);
					
					}
					out.println(k + "\t" + v);
				}
			}

			out.println();	

			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	
		
	}
	
	public void printResults() {
		
		PrintWriter out = new PrintWriter(System.out, true);
		ArrayList<String> valStatNames = new ArrayList<String>();
		ArrayList<String> otherNames = new ArrayList<String>();

		for (String k : resultMap.keySet()) {
			if (k != Experiment.RUNTIME) {
				Object v = resultMap.get(k);
				if (v instanceof SummaryStat) {
					valStatNames.add(k);
				} else {
					otherNames.add(k);
				}
			}
		}

		// sort by name, ignoring upper and lower case
		Collections.sort(valStatNames, new Comparator<String>() {

			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});
		Collections.sort(otherNames, new Comparator<String>() {

			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
			}
		});

		// output ValueStat-objects
		if (valStatNames.size() > 0) {
			out.println();
			out.println("Name\tMean\tMin\tMax\tStdDev\tCount\tSum");

			for (String k : valStatNames) {
				SummaryStat vs = (SummaryStat) resultMap.get(k);
				out.printf(Util.DEF_LOCALE,
						"%s\t%.4f\t%.4f\t%.4f\t%.4f\t%d\t%.4f%n", k, vs.mean(),
						vs.min(), vs.max(), vs.stdDev(), vs.numObs(), vs.sum());
			}
		}

		// output all other objects (except runtime)
		if (otherNames.size() > 0) {
			out.println();
			out.println("Name\tValue");

			for (String k : otherNames) {
				Object v = resultMap.get(k);
				if (v != null) {
					if (v.getClass().isArray())
						v = Util.arrayToString(v);
				
				}
				out.println(k + "\t" + v);
			}
		}

		out.println();	

		out.flush();
	}
	
	public Map<String, Object> getResults() {
		return resultMap;
	}

	private int[] findRank(final double[] means) {
		Integer[] idx = new Integer[means.length];
		for (int i = 0; i < idx.length; i++) {
			idx[i] = i;
		}

		Arrays.sort(idx, new Comparator<Integer>() {
			@Override
			public int compare(Integer i1, Integer i2) {
				return (getProblemType() == ProblemType.MAXIMIZE ? -1 : +1)
						* Double.compare(means[i1.intValue()], means[i2.intValue()]);
			}
		});

		int[] ranks = new int[idx.length];
		for (int i = 0; i < ranks.length; i++) {
			ranks[idx[i].intValue()] = i + 1;
		}

		return ranks;
	}
	
	public void runEvaluation() {

		long start;
		long end;
		double diff;
		
		//Warming up period
		System.out.println("Warming Up Started");
		start = System.currentTimeMillis();
		doWarmup();
		end = System.currentTimeMillis();
		diff = (end - start) / 1000.0d;
		System.out.println("Warming Up Finishied in " + diff + " seconds");		
	

		//Ranking and selection
		System.out.println("Budget Allocation Stage Started");		
		this.budgetUsed = 0;
		this.totalBudget = this.firstStageTotalBudget;
		start = System.currentTimeMillis();
		this.doRankingAndSelection();
		end = System.currentTimeMillis();
		diff = (end - start) / 1000.0d;
		System.out.println("Budget Allocation Stage Finishied in " + diff + " seconds");	
		
		System.out.println("WarmingBudget=" + this.warmupStageBudget*this.NumberOfDR + " BudgetAllocationStageBudget=" + this.firstStageTotalBudget);

	}

	protected boolean hasMoreTasks() {		
		
		// identify currently best system
		currBest = 0;
		double bestMean = getProblemType() == ProblemType.MAXIMIZE ? stats[0].mean() : -stats[0].mean();
		for (int i = 1; i < stats.length; i++) {
			double v = getProblemType() == ProblemType.MAXIMIZE ? stats[i].mean() : -stats[i].mean();
			if (v > bestMean) {
				bestMean = v;
				currBest = i;
			}
		}

		// check stopping conditions
		if ((totalBudget > 0 && budgetUsed >= totalBudget))
			return false;

		// allocate new iterations
		int iter = 1;
		if (totalBudget > 0)
			iter = Math.min(iter, totalBudget - budgetUsed);	

		return true;
	}

	protected double calcPCS() {
		double[] prodTerms = calcPCSPriosPerConfiguration();

		double res = 1.0d;
		for (int i = 0; i < prodTerms.length; i++) {
			if (i == currBest)
				continue;

			res *= prodTerms[i];
		}

		return res;
	}

	protected double[] calcPCSPriosPerConfiguration() {
		final SummaryStat best = stats[currBest];
		final double bestMean = best.mean();

		double bestNormVariance = best.variance() / best.numObs();

		double[] prodTerms = new double[stats.length];
		for (int i = 0; i < stats.length; i++) {
			if (i == currBest)
				continue;

			SummaryStat vs = stats[i];
			prodTerms[i] = (bestMean - vs.mean()) / Math.sqrt(bestNormVariance + vs.variance() / vs.numObs());
		}

		NormalDistribution normalDist = new NormalDistribution();

		for (int i = 0; i < stats.length; i++) {
			if (i == currBest)
				continue;

			prodTerms[i] = normalDist.cumulativeProbability(prodTerms[i]);
			if (getProblemType() == ProblemType.MINIMIZE)
				prodTerms[i] = 1.0 - prodTerms[i];
		}

		return prodTerms;
	}
	
	protected int CalculateBestIndex()
	{
		int BestIndex = 0;
		
		final int nd = stats.length;

		double t_s_mean[] = new double[nd];
		if (getProblemType() == ProblemType.MAXIMIZE) { /* MAX problem */
			for (int i = 0; i < nd; i++)
				t_s_mean[i] = stats[i].mean();
		} else { /* MIN problem */
			for (int i = 0; i < nd; i++)
				t_s_mean[i] = -stats[i].mean();
		}

		int b = currBest;
		int s = second_best(t_s_mean, b);
		
	
		double A[] = new double[nd];
		
		
		double deltaBest = 0.0d;
		for(int i = 0; i < nd; i++)
		{
			if(i != b)
			{
				deltaBest += stats[b].variance() * stats[i].variance() / Math.pow(t_s_mean[i] - t_s_mean[b], 4);
			}
		}
		
		deltaBest = Math.pow(deltaBest, -0.25);
		
		double rDistance[] = new double[nd];
		double kValue[] = new double[nd];
		
		NormalDistribution normalDist = new NormalDistribution();
		double maxValue = -Double.MAX_VALUE;
		
		for(int i = 0; i < nd; i++)
		{
			double vi = stats[i].variance() / stats[i].numObs();
			double sqrtVariance = Math.sqrt(vi);

			if(i == b)
			{
				A[i] = t_s_mean[b] + deltaBest * Math.sqrt(stats[i].variance());
			}
			else
			{
				A[i] = t_s_mean[b];
			}
			
			rDistance[i] = -(t_s_mean[i] - A[i])/sqrtVariance;
			
			kValue[i] = sqrtVariance * normalDist.density(rDistance[i]) +  (t_s_mean[i] - A[i]) * (1 - normalDist.cumulativeProbability(rDistance[i]));
			
			if(kValue[i] > maxValue)
			{
				maxValue = kValue[i];
				BestIndex = i;
			}
		}
				
		return BestIndex;
	}

	/**
	 * This subroutine implements the optimal computation budget allocation
	 * (OCBA) algorithm presented in Chen et al. (2000) in the J of DEDS. It
	 * determines how many additional runs each design should have for the next
	 * iteration of simulation.
	 * 
	 * 
	 * @param add_budget
	 *            The total number of additional replications that can be
	 *            performed.
	 * 
	 * @return additional number of simulation replication assigned to design i,
	 *         i=0,1,..,ND-1
	 */
	// * @param s_mean
	// * [i]: sample mean of design i, i=0,1,..,ND-1
	// *
	// * @param s_var
	// * [i]: sample variance of design i, i=0,1,..,ND-1
	// *
	// * @param n
	// * [i]: number of simulation replication of design i,
	// * i=0,1,..,ND-1
	// *
	// * @param add_budget
	// * : the additional simulation budget
	// *
	// * @param type
	// * : type of optimization problem. type=1, MIN problem; type=2,
	// * MAX problem
	protected int[] ocba(int add_budget) {
		final int nd = stats.length;

		double t_s_mean[] = new double[nd];
		if (getProblemType() == ProblemType.MAXIMIZE) { /* MAX problem */
			for (int i = 0; i < nd; i++)
				t_s_mean[i] = -stats[i].mean();
		} else { /* MIN problem */
			for (int i = 0; i < nd; i++)
				t_s_mean[i] = stats[i].mean();
		}

		int t_budget = add_budget;

		for (int i = 0; i < nd; i++)
			t_budget += stats[i].numObs();

		int b = currBest;
		int s = second_best(t_s_mean, b);

		double ratio[] = new double[nd];
		ratio[s] = 1.0d;
		for (int i = 0; i < nd; i++)
			if (i != s && i != b) {
				double temp = (t_s_mean[b] - t_s_mean[s]) / (t_s_mean[b] - t_s_mean[i]);
				ratio[i] = temp * temp * stats[i].variance() / stats[s].variance();
			} /* calculate ratio of Ni/Ns */

		double temp = 0.0;
		for (int i = 0; i < nd; i++)
			if (i != b)
				temp += (ratio[i] * ratio[i] / stats[i].variance());
		ratio[b] = Math.sqrt(stats[b].variance() * temp); /* calculate Nb */

		int morerun[] = new int[nd];
		for (int i = 0; i < nd; i++)
			morerun[i] = 1;

		int t1_budget = t_budget;

		int[] an = new int[nd];
		boolean more_alloc;
		do {
			more_alloc = false;
			double ratio_s = 0.0f;
			for (int i = 0; i < nd; i++)
				if (morerun[i] == 1)
					ratio_s += ratio[i];

			for (int i = 0; i < nd; i++)
				if (morerun[i] == 1) {
					an[i] = (int) (t1_budget / ratio_s * ratio[i]);
					/* disable thoese design which have benn run too much */
					if (an[i] < stats[i].numObs()) {
						an[i] = stats[i].numObs();
						morerun[i] = 0;
						more_alloc = true;
					}
				}

			if (more_alloc) {
				t1_budget = t_budget;
				for (int i = 0; i < nd; i++)
					if (morerun[i] != 1)
						t1_budget -= an[i];
			}
		} while (more_alloc); /* end of WHILE */

		/* calculate the difference */
		t1_budget = an[0];

		for (int i = 1; i < nd; i++)
			t1_budget += an[i];

		an[b] += (t_budget - t1_budget); /* give the difference to design b */

		for (int i = 0; i < nd; i++)
			an[i] -= stats[i].numObs();

		return an;
	}

	/**
	 * This function determines the second best design based on current
	 * simulation results.
	 * 
	 * @param t_s_mean
	 *            [i]: temporary array for sample mean of design i,
	 *            i=0,1,..,ND-1
	 * @param b
	 *            : current best design design determined by function best()
	 */
	private static int second_best(final double t_s_mean[], int b) {
		int second_index = (b == 0) ? 1 : 0;

		for (int i = 0; i < t_s_mean.length; i++) {
			if (t_s_mean[i] > t_s_mean[second_index] && i != b) {
				second_index = i;
			}
		}

		return second_index;
	}								

	//
	//
	// getters and setters of parameters below
	//
	//

	/**
	 * Sets the minimum number of replications performed for each configuration.
	 * This has to be &gt;=3.
	 * 
	 * @param minReps
	 *            The minimum number of replications per configuration.
	 */
	public void setMinReplicationsPerConfiguration(int minReps) {
//		if (minReps < 3)
//			throw new IllegalArgumentException("Minimum number of replications has to be >=3.");
		this.minReplicationsPerConfiguration = minReps;
	}

	public int getMinReplicationsPerConfiguration() {
		return minReplicationsPerConfiguration;
	}

	/**
	 * Sets the name of the objective which defines "best". This has to be the
	 * name of a result produced by the base experiment.
	 * 
	 * @param objective
	 *            Result name to use as the objective function.
	 */
	public void setObjective(EJobShopObjectives objective) {
		this.objective = objective;
	}

	public EJobShopObjectives getObjective() {
		return objective;
	}

	/**
	 * Stop using more replications if this level of the probablity of correct
	 * selection is reached. This defines a dynamic stopping criterion.
	 * 
	 * @param pcsLevel
	 *            The desired confidence in the results (between 0 and 1).
	 */
	public void setPcsLevel(double pcsLevel) {
		if (pcsLevel < 0 || pcsLevel > 1)
			throw new IllegalArgumentException("Invalid probability: " + pcsLevel);
		this.pcsLevel = pcsLevel;
	}

	public double getPcsLevel() {
		return pcsLevel;
	}

	/**
	 * Whether to produce detailed results or just basic information of the best
	 * configuration.
	 * 
	 * @param detailedResults
	 *            Produce detailed results or not.
	 */
	public void setDetailedResults(boolean detailedResults) {
		this.detailedResults = detailedResults;
	}

	public boolean isDetailedResults() {
		return detailedResults;
	}

	/**
	 * Sets the total budget for each configuration.
	 * 
	 * @param numReplications
	 *            The number of replications to use.
	 */
	public void setNumReplications(int numReplications) {
		this.numReplications = numReplications;
	}

	public int getNumReplications() {
		return numReplications;
	}

	public ProblemType getProblemType() {
		return problemType;
	}

	/**
	 * Sets whether a minimization or maximization experiment should be solved.
	 * 
	 * @param problemType
	 *            Whether minimization or maximization are required.
	 */
	public void setProblemType(ProblemType problemType) {
		this.problemType = problemType;
	}

	/*public static void main(String[] args) throws CloneNotSupportedException
	{
		long t1 = System.currentTimeMillis(); 
		
		AOAPNewRun run = new AOAPNewRun();
		
		run.setNumReplications(100);
		run.setMinReplicationsPerConfiguration(10);
		run.InitilizeSeed = System.currentTimeMillis();
		
		
		String rule1 = "(0.0 - (If(div(RPT, RPT), 0.0 + If(TIQ, PT, TIS), If(0.0, PT, TIQ)) + ((WINQ * (0.0 + If(TIQ, PT, TIS))) * (NPT * 1.0)))) + (((div(If(1.0, WINQ, NPT), If(TIQ, PT, TIS) - ((If(If(WINQ * PT, If(OpsLeft, WINQ, PT), div(RPT, WINQ)) + (max(OpsLeft, 0.0) * div(RPT, 0.0)), If(OpsLeft, WINQ, PT), (WINQ * (0.0 + If(TIQ, PT, TIS))) * (NPT * 1.0)) + (max(OpsLeft, 0.0) * If(WINQ, max(div(PT, TIS), 0.0 + PT), 0.0))) - If(OpsLeft + RPT, WINQ, PT))) * If(0.0, WINQ, 0.0)) * (If(0.0, PT, 0.0 - PT) - (PT + PT))) - (TIS * PT))";
		String rule2 = "(((PT + PT) * (If(0.0, PT, PT) + (max(TIS, max(RPT, 0.0)) * If(0.0, PT, TIQ)))) * ((1.0 + PT) - (PT + PT))) + (((PT * (0.0 - PT)) * ((If(WINQ * max(PT, NPT), PT, max(RPT, 0.0)) + ((WINQ * max(PT, NPT)) * (NPT * 1.0))) + (max(RPT, 0.0) + max(PT, NPT)))) - (((((If(0.0, PT, TIQ) - PT) - ((PT + PT) - max(PT, NPT))) - PT) - PT) * ((PT + (PT + PT)) + (PT + PT))))";
		
		run.AddDispatchingRule(new MyCustomGP(rule1));
		run.AddDispatchingRule(new MyCustomGP(rule2));
		//run.setPcsLevel(0.99);
		run.init();
		run.runEvaluation();
		long t2 = System.currentTimeMillis();
		run.produceResults();
		run.printResults();
		System.out.println("Total seconds:" + (t2 - t1)/1000.0d );
	}*/
}
