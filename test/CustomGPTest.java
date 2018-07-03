
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
import jasima.core.experiment.MultipleReplicationExperiment;
import jasima.core.experiment.TestConfiguration;
import jasima.core.random.continuous.DblConst;
import jasima.core.random.discrete.IntEmpirical;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.Simulation.SimEvent;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.ExcelSaver;
import jasima.core.util.Util;
import jasima.core.util.observer.NotifierListener;
import jasima.shopSim.core.PR;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment.Scenario;
import jasima.shopSim.prioRules.basic.CR;
import jasima.shopSim.prioRules.basic.EDD;
import jasima.shopSim.prioRules.basic.FASFS;
import jasima.shopSim.prioRules.basic.FCFS;
import jasima.shopSim.prioRules.basic.MDD;
import jasima.shopSim.prioRules.basic.SPT;
import jasima.shopSim.prioRules.basic.SRPT;
import jasima.shopSim.prioRules.gp.GECCO2010_genSeed_10reps;
import jasima.shopSim.prioRules.gp.GECCO2010_genSeed_2reps;
import jasima.shopSim.prioRules.gp.MyCustomGP;
import jasima.shopSim.prioRules.gp.NormalizedBrankeRule;
import jasima.shopSim.prioRules.gp.NormalizedBrankeRule_StringExecution;
import jasima.shopSim.prioRules.gp.TempRule;
import jasima.shopSim.prioRules.meta.IgnoreFutureJobs;
import jasima.shopSim.prioRules.upDownStream.PTPlusWINQPlusNPT;
import jasima.shopSim.prioRules.upDownStream.WINQ;
import jasima.shopSim.util.BasicJobStatCollector;
import jasima.shopSim.util.DeviationJobStatCollector;
import jasima.shopSim.util.TardinessDeviationJobStatCollector;
import jasima.shopSim.util.Tardiness_ExtendedJobStatCollector;
import jasima.shopSim.util.ExtendedJobStatCollector;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import ec.gp.GPIndividual;

/**
 * 
 * @author Cheng
 * @version "$Id$"
 */
@SuppressWarnings("deprecation")
public class CustomGPTest {

	public long InitialSeed = 8238723;

	public Random seedStream = new Random(this.InitialSeed);
	public int NumOfReplications = 1;
	public String curGPRule;

	public enum EObjectives {
		CMAX, FlowTime,
	}

	public EObjectives obj = EObjectives.CMAX;

	ArrayList<DynamicShopExperiment> multipleDynamicExps = new ArrayList<DynamicShopExperiment>();

	public double ExecuteExpriment(DynamicShopExperiment de, String rule) {

		PR sr = new MyCustomGP(rule);
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		de.setSequencingRule(sr2);
		de.runExperiment();

		// de.printResults();

		Map<String, Object> res = de.getResults();

		double fitness = Double.MAX_VALUE;

		if (obj == EObjectives.CMAX) {
			fitness = (double) res.get("cMax");
		} else if (obj == EObjectives.FlowTime) {
			SummaryStat flowtime = (SummaryStat) res.get("flowtime");

			fitness = flowtime.mean();
		}

		return fitness;
	}

	public void Initialize_MultipleReplications() {
		long seed;
		for (int i = 0; i < NumOfReplications; i++) {
			if (seedStream == null)
				seedStream = new Random(this.InitialSeed);

			seed = seedStream.nextLong();

			DynamicShopExperiment e = new DynamicShopExperiment();

			e.setInitialSeed(seed);
			// remove default BasicJobStatCollector
			NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
			assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
			e.setShopListener(null);

			BasicJobStatCollector basicJobStatCollector = new BasicJobStatCollector();
			// basicJobStatCollector.setInitialPeriod(500);
			basicJobStatCollector.setIgnoreFirst(500);

			e.addShopListener(basicJobStatCollector);

			e.setNumMachines(10);
			e.setNumOps(10, 10);
			e.setDueDateFactor(new DblConst(4.0));
			e.setUtilLevel(0.95d);
			e.setStopAfterNumJobs(2500);
			e.setScenario(Scenario.JOB_SHOP);

			multipleDynamicExps.add(e);

			System.out.println("Seed: " + seed);

		}
	}

	public void testMultipleDJSSP() {

		double sum_CMax = 0;
		double[] cmaxArray = new double[NumOfReplications];
		int index = 0;

		for (DynamicShopExperiment dje : multipleDynamicExps) {
			cmaxArray[index] = ExecuteExpriment(dje, curGPRule);
			index++;
		}

		for (double value : cmaxArray) {
			sum_CMax += value;
		}

		double mean_cMax = sum_CMax / NumOfReplications;

		double variance = 0;

		for (int i = 0; i < this.NumOfReplications; i++) {
			variance += (cmaxArray[i] - mean_cMax) * (cmaxArray[i] - mean_cMax);
		}

		variance = variance / NumOfReplications - 1;
		double dev = Math.sqrt(variance);

		System.out.println("avg=" + mean_cMax + " dev=" + dev);

	}

	public static void runSingleExperiment(PR rule, long seed) {
		DynamicShopExperiment e = new DynamicShopExperiment();

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		TardinessDeviationJobStatCollector basicJobStatCollector = new TardinessDeviationJobStatCollector();
		basicJobStatCollector.setInitialPeriod(500);
		basicJobStatCollector.setIgnoreFirst(500);
		basicJobStatCollector.initialSeed = seed;

		e.addShopListener(basicJobStatCollector);
		e.setMaxJobsInSystem(500);
		e.setNumMachines(10);
		e.setNumOps(10, 10);
		e.setDueDateFactor(new DblConst(4.0));
		e.setUtilLevel(0.95d);
		e.setStopAfterNumJobs(2500);
		e.setScenario(Scenario.JOB_SHOP);
		PR sr = rule;
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);
		e.setInitialSeed(seed);

		e.runExperiment();

		SummaryStat stat = (SummaryStat) e.getResults().get("flowtimeDev");
		
		e.printResults();

	}
	
	public static void RunDE(String rule, long seed)
	{
		DynamicShopExperiment dsExp;
		
		dsExp = new DynamicShopExperiment();
		dsExp.setInitialSeed(seed);

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = dsExp.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		dsExp.setShopListener(null);
	
		BasicJobStatCollector basicJobStatCollector = new BasicJobStatCollector();
//		basicJobStatCollector.setIgnoreFirst(100);
//		basicJobStatCollector.setInitialPeriod(100);
		
		dsExp.addShopListener(basicJobStatCollector);		
		
		dsExp.setNumMachines(5);
		dsExp.setNumOps(5, 5);
		dsExp.setDueDateFactor(new DblConst(4.0));
		dsExp.setUtilLevel(0.95d);
		dsExp.setScenario(Scenario.JOB_SHOP);	
		dsExp.setStopAfterNumJobs(300);		
	
		PR sr = new NormalizedBrankeRule_StringExecution(rule);
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		dsExp.setSequencingRule(sr2);
		
		//dsExp.setSequencingRule(new NormalizedBrankeRule_StringExecution(rule));
		
		dsExp.runExperiment();
		
		Map<String, Object> res = dsExp.getResults();
		SummaryStat flowtime = (SummaryStat)res.get("flowtime");
		dsExp.printResults();
	}

	public static void RunMRE(PR rule, long seed, int NumOfReplications) {
		CustomGPTest test = new CustomGPTest();
		test.obj = EObjectives.FlowTime;
		test.InitialSeed = seed;
		test.NumOfReplications = NumOfReplications;

		DynamicShopExperiment e = new DynamicShopExperiment();

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		DeviationJobStatCollector basicJobStatCollector = new DeviationJobStatCollector();
		//BasicJobStatCollector basicJobStatCollector = new BasicJobStatCollector();
		basicJobStatCollector.setInitialPeriod(500);
		basicJobStatCollector.setIgnoreFirst(500);	
	

		ExtendedJobStatCollector extJobStatCollector = new ExtendedJobStatCollector();
		extJobStatCollector.setInitialPeriod(500);
		extJobStatCollector.setIgnoreFirst(500);

		e.addShopListener(basicJobStatCollector);

		//e.addShopListener(extJobStatCollector);
		
		

		e.setMaxJobsInSystem(500);

		e.setNumMachines(10);
		e.setNumOps(10, 10);
		e.setDueDateFactor(new DblConst(4.0));
		e.setWeights(new IntEmpirical(new double[] { 0.20, 0.60, 0.20 }, new int[] { 1, 2, 4 }));
		e.setUtilLevel(0.95d);
		e.setStopAfterNumJobs(2500);
		e.setScenario(Scenario.JOB_SHOP);
		// e.setWeights(new IntUniformRange(1, 10));

		MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
		mre.setBaseExperiment(e);

		PR sr = rule;

		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);
		mre.setMaxReplications(test.NumOfReplications);
		mre.setInitialSeed(test.InitialSeed);

		mre.runExperiment();

		mre.getResults();
		mre.printResults();
	}

	public static void RunMRE(String rule, int normal, long seed, int NumOfReplications) {
		CustomGPTest test = new CustomGPTest();
		test.obj = EObjectives.FlowTime;
		test.InitialSeed = seed;
		test.NumOfReplications = NumOfReplications;

		test.curGPRule = rule;
		
		DynamicShopExperiment e = Util.getBaseExperiment(normal, 2500, 10, 10, 0.95, 11, seed);
		
		
//		DynamicShopExperiment e = new DynamicShopExperiment();
//
//		// remove default BasicJobStatCollector
//		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
//		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
//		e.setShopListener(null);
//
//		DeviationJobStatCollector basicJobStatCollector = new DeviationJobStatCollector();
//		basicJobStatCollector.setInitialPeriod(500);
//		basicJobStatCollector.setIgnoreFirst(500);
//
//		ExtendedJobStatCollector extJobStatCollector = new ExtendedJobStatCollector();
//		extJobStatCollector.setInitialPeriod(500);
//		extJobStatCollector.setIgnoreFirst(500);
//
//		e.addShopListener(basicJobStatCollector);
//
//
//		e.setNumMachines(10);
//		e.setNumOps(10, 10);
//		e.setDueDateFactor(new DblConst(4.0));
//		//e.setWeights(new IntEmpirical(new double[] { 0.20, 0.60, 0.20 }, new int[] { 1, 2, 4 }));
//		e.setUtilLevel(0.95d);
//		e.setStopAfterNumJobs(2500);
//		e.setScenario(Scenario.JOB_SHOP);
//		// e.setWeights(new IntUniformRange(1, 10));
		

		MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
		mre.setBaseExperiment(e);

		PR sr = null;
		//sr = new PTPlusWINQPlusNPT();
		// sr = new TempRule();
		if (normal == 0)
			sr = new MyCustomGP(test.curGPRule);
		else
			sr = new NormalizedBrankeRule_StringExecution(test.curGPRule);

		
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);
		mre.setMaxReplications(test.NumOfReplications);
		mre.setInitialSeed(test.InitialSeed);

//		for (int i = 0; i < test.NumOfReplications; i++) {
//			mre.addKeepResultName("flowtime");
//		}

		mre.runExperiment();

		mre.printResults();
	}
	
	public static void RunMRE_NoDeviation(String rule, int normal, long seed, int NumOfReplications) {
		CustomGPTest test = new CustomGPTest();
		test.obj = EObjectives.FlowTime;
		test.InitialSeed = seed;
		test.NumOfReplications = NumOfReplications;

		test.curGPRule = rule;
		DynamicShopExperiment e = new DynamicShopExperiment();

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		BasicJobStatCollector basicJobStatCollector = new BasicJobStatCollector();
		basicJobStatCollector.setInitialPeriod(500);
		basicJobStatCollector.setIgnoreFirst(500);

		ExtendedJobStatCollector extJobStatCollector = new ExtendedJobStatCollector();
		extJobStatCollector.setInitialPeriod(500);
		extJobStatCollector.setIgnoreFirst(500);

		e.addShopListener(basicJobStatCollector);


		e.setNumMachines(10);
		e.setNumOps(10, 10);
		e.setDueDateFactor(new DblConst(4.0));
		//e.setWeights(new IntEmpirical(new double[] { 0.20, 0.60, 0.20 }, new int[] { 1, 2, 4 }));
		e.setUtilLevel(0.95d);
		e.setStopAfterNumJobs(2500);
		e.setScenario(Scenario.JOB_SHOP);
		// e.setWeights(new IntUniformRange(1, 10));

		MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
		mre.setBaseExperiment(e);

		PR sr = null;
		//sr = new PTPlusWINQPlusNPT();
		// sr = new TempRule();
		if (normal == 0)
			sr = new MyCustomGP(test.curGPRule);
		else
			sr = new NormalizedBrankeRule_StringExecution(test.curGPRule);

		
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);
		mre.setMaxReplications(test.NumOfReplications);
		mre.setInitialSeed(test.InitialSeed);

//		for (int i = 0; i < test.NumOfReplications; i++) {
//			mre.addKeepResultName("flowtime");
//		}

		mre.runExperiment();

		mre.printResults();
	}

	public static void testFinalEvaluation(String cpath,String outputFile, double utilLevel, int numMachines, int minOPs,
			int maxOPs, long seed) {
		//String path = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA+Final\\14-30";		

		GPCompareTest test = new GPCompareTest();
		test.InitialSeed = seed;// 778899;
		test.Normal = 1;
		test.NumOfReplications = 200;
		test.outputExcelFile = outputFile;
		test.numMachines = numMachines;
		test.minOPs = minOPs;
		test.maxOPs = maxOPs;
		test.utilizationLevel=utilLevel;

		int instance = 4;
		int ComparedCount = instance;
		
		TestConfiguration t00 = new TestConfiguration();
		t00.ConfigDescription = "Original";
		t00.InstanceCount = instance;

		TestConfiguration t0 = new TestConfiguration();
		t0.ConfigDescription = "EA";
		t0.InstanceCount = instance;

		TestConfiguration t1 = new TestConfiguration();
		t1.ConfigDescription = "IDOCBA";
		t1.InstanceCount = instance;

		TestConfiguration t2 = new TestConfiguration();
		t2.ConfigDescription = "OCBA";
		t2.InstanceCount = instance;

		TestConfiguration t3 = new TestConfiguration();
		t3.ConfigDescription = "KG";
		t3.InstanceCount = instance;

		test.NumOfInstance = ComparedCount;
		test.TestingFinalEvaluation(cpath, t00, t0, t1,t2);
		//test.TestingFinalEvaluation(cpath, t00);

		System.out.println("The seed is " + test.InitialSeed);
	}
	
	public static void testFinalEvaluationNew(long seed, String path, int minInstance, int maxInstance, int minops, int maxops, double utilizationlevel)
	{
		GPCompareTest test = new GPCompareTest();
		
		for(int i = minInstance; i <= maxInstance; i++)
		{
			String filename = path + "\\job." + i + ".Result_Evaluation.stat";
//			test.testEachGenerationRule(new File(filename),
//					path);
		}
	}

	public static void testOldSeperateEvaluation(long seed, String path, int numofInstance, int minops, int maxops, double utilizationlevel) {
		GPCompareTest test = new GPCompareTest();
		test.InitialSeed = seed;
		test.Normal = 1;
		test.NumOfReplications = 200;
		test.minOPs = minops;
		test.maxOPs = maxops;
		test.utilizationLevel = utilizationlevel;


		String path1 = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\100Ind+20R\\W12(5)+8+0.45\\50";
		String path2 = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\EA-40R\\1429";
		String path3 = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\RS10_10_0.7";
		//String path3 = "E:\\开发必备\\Genetic Programming\\实验数据\\GP Optimization\\500IND+10Gen+STD+Normal+InGenEvaluation+FinalEvaluation+Deviation\\TSIDOCBA\\Compare\\iOCBA";
		String path4 = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\20171122修正TOCBA\\OCBA_New_DuplicationRemoval+10W+10RS+5Iteration";
		String path5 = "E:\\开发必备\\Genetic Programming\\实验数据\\Random Search\\500IND+Normal+DuplicateRemoval+1Gen\\150Run+ERC+Seed123456789\\AOAPlog";

		int instance = numofInstance;

		int ComparedCount = instance;

		TestConfiguration t0 = new TestConfiguration();
		t0.filePath = path;
		t0.ConfigDescription = "500Ind+10R+EA+1R";
		t0.InstanceCount = instance;

		TestConfiguration t1 = new TestConfiguration();
		t1.filePath = path2;
		t1.ConfigDescription = "500Ind+10R+OCBA";
		t1.InstanceCount = instance;

		TestConfiguration t2 = new TestConfiguration();
		t2.filePath = path3;
		t2.ConfigDescription = "500Ind+10R+iOCBA";
		t2.InstanceCount = instance;

		TestConfiguration t3 = new TestConfiguration();
		t3.filePath = path4;
		t3.ConfigDescription = "500Ind+10R+AOAP";
		t3.InstanceCount = instance;

		TestConfiguration t4 = new TestConfiguration();
		t4.filePath = path4;
		t4.ConfigDescription = "500Ind+10R+KG";
		t4.InstanceCount = instance;

		TestConfiguration t5 = new TestConfiguration();
		t5.filePath = path5;
		t5.ConfigDescription = "500Ind+15R+AOAP";
		t5.InstanceCount = instance;

		test.NumOfInstance = ComparedCount;

		test.CompareAndLogForEvaluation(path +"\\Result.Raw" + minops +"." + maxops+"." + utilizationlevel + "." + seed +".xls", 
				t0);
		
		System.out.println(test.InitialSeed); 
	}

	@Test
	public static void main(String[] args) throws Exception {	
	
		
//		GPCompareTest test = new GPCompareTest();
//		test.doReadProgramLength("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Pre5+RS12_8_0.4+final20\\1449");
		
		String path = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\TSOCBA+Greedy+CorrectElite\\7-9";
//		
		testFinalEvaluation(path, "test-10.10.95", 0.95d, 10, 10, 10, 778899);
		
//		testFinalEvaluation(path,
//				"test-10.10.95", 0.95d, 10, 10, 10, 778899);
		
//		testFinalEvaluation("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA+Elite\\Modified修正top的选取\\Final Multiple",
//				"test-2.10.95", 0.95d, 10, 2, 10, 4456);
//		
//		testFinalEvaluation("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA+Elite\\Modified修正top的选取\\Final Multiple",
//				"test-2.10.85", 0.85d, 10, 2, 10, 4456);
		
//		String path = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\EA+10R";
//		long seed = 778899;
//		int count = 30;
//		testOldSeperateEvaluation(seed, path, count, 10,10,0.95);
//		testOldSeperateEvaluation(seed, path, count, 10,10,0.85);
//		testOldSeperateEvaluation(seed, path, count, 2,10,0.95);
//		testOldSeperateEvaluation(seed, path, count, 2,10,0.85);
//		//testOldSeperateEvaluation(778899, "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\EA+10R\\17-19", 3);
		
//		Random r = new Random(181085);
//		for(int i = 0; i <=19; i++)
//		{
//			long seed = r.nextLong();
//			
//			testFinalEvaluation("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA+Final",
//			"result.full", 0.95d, 10, 10, 10, seed);
//			
//			testOldSeperateEvaluation(seed);
//		}
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		System.out.println(df.format(new Date()));// new Date()为获取当前系统时间
	}
		
	public static void main44(String[] args) {
	
	
		String rule = "((0.0 - PT) - ((WINQ * NPT) * (PT * RPT))) * (((div((PT * (NPT + PT)) * ((max(NPT, PT) * (WINQ * NPT)) + max((If(1.0, RPT, 1.0) + (NPT + PT)) - (NPT * If(WINQ, NPT + PT, WINQ)), 0.0)), max(OpsLeft, PT * RPT)) + (((div(PT + max(0.0, TIS), max(OpsLeft, TIS)) + PT) + div(WINQ * NPT, PT * RPT)) * div(max(NPT, PT) + max(0.0, div(max(OpsLeft, TIS), If(OpsLeft, 1.0, WINQ) + div(1.0, 0.0)) + div(div(max((WINQ * NPT) * max(PT, TIQ), TIQ), NPT + PT), max(OpsLeft, TIS))), NPT + PT))) * max(NPT, PT)) + ((((max(OpsLeft, TIS) * (WINQ * NPT)) + max(TIS, 0.0)) * (PT * RPT)) * max(OpsLeft, TIS)))";
		String rule2 = "(((0.0 - PT) - ((WINQ * NPT) * (PT * RPT))) * (((div(((NPT + PT) * max(PT, TIQ)) * ((max(OpsLeft, TIS) * (WINQ * NPT)) + max((If(OpsLeft, max(PT, TIQ), WINQ) + (NPT + PT)) - (NPT * If(WINQ, NPT + PT, WINQ)), 0.0)), max(OpsLeft, TIS)) + If(TIQ, 0.0, TIS)) * max(NPT, PT)) + ((max(TIS, WINQ) * TIS) * (If(OpsLeft, 1.0, WINQ) + max(PT, TIQ))))) * (((max(NPT, PT) + max(0.0, ((max(NPT, PT) + div(max(PT, TIQ), max(OpsLeft, TIS))) * div(max(OpsLeft, TIS), If(1.0, RPT, 1.0) * If(WINQ, NPT + PT, WINQ))) * max(PT, TIQ))) * (If(OpsLeft, 1.0, WINQ) + div(1.0, 0.0))) + ((NPT + 1.0) * (((NPT * (((div(((max(OpsLeft, TIS) * (WINQ * NPT)) + max(TIS, 0.0)) * (PT * RPT), (div((NPT + PT) + max(0.0, TIS), max(OpsLeft, TIS)) + PT) + div(max((WINQ * NPT) * (PT * RPT), TIQ), NPT + PT)) + PT) + div(max((WINQ * NPT) * (PT * RPT), TIQ), PT * RPT)) * div(max(NPT, PT) + max(OpsLeft, TIS), NPT + PT))) * (((div((NPT + PT) + max(0.0, TIS), max(OpsLeft, TIS)) + PT) + div(max((WINQ * NPT) * (PT * RPT), TIQ), NPT + PT)) * div(PT * RPT, NPT + PT))) * (max(WINQ, WINQ) + If(TIQ, 0.0, TIS)))))";
		String rule3= "((0.0 - PT) - ((((WINQ * NPT) + ((max(PT, TIQ) * div(max(PT, TIQ), (((((NPT + PT) + max(0.0, TIS)) * If(WINQ, PT, WINQ)) + (max(WINQ, WINQ) + max(NPT, PT))) * div(max(PT, TIQ), max(OpsLeft, TIS))) + PT)) * div(max(PT, TIQ), (((If(1.0, RPT, 1.0) * If(WINQ, NPT + ((NPT + PT) * max(PT, TIQ)), WINQ)) + (max(WINQ, WINQ) + If(TIQ, 0.0, TIS))) * div(NPT + max(NPT + PT, TIQ), div(PT * RPT, NPT + PT) + PT)) + PT))) * max(NPT, PT)) * (PT * RPT))) * (((div((PT * max(PT, TIQ)) * ((max(NPT, PT) * (WINQ * NPT)) + max((max(PT, TIQ) + max(PT, TIQ)) - (NPT * If(WINQ, NPT + PT, WINQ)), 0.0)), max(OpsLeft, PT * RPT)) + (max(PT, TIQ) * div(PT, max(OpsLeft, TIS)))) * max(NPT, PT)) + ((((max(OpsLeft, TIS) * (WINQ * NPT)) + max(TIS, 0.0)) * (PT * RPT)) * max(OpsLeft, TIS)))";
		//CalculateRanking();
		int count = 200;
		long t1 =  548755333333L;
		
		RunMRE(rule, 1, t1, count);
		RunMRE(rule2, 1, t1, count);
		//RunMRE(rule3, 1, t1, count);
		//RunMRE_NoDeviation(rule2, 1, t1, count);
		
//		RunMRE(new FCFS(), t1, count);
//		RunMRE(new EDD(), t1, count);
//		RunMRE(new SPT(), t1, count);
//		RunMRE(new SRPT(), t1, count);
//		RunMRE(new WINQ(), t1, count);
		//RunMRE(new PTPlusWINQPlusNPT(), t1, count);
		//RunMRE(rule2, 1, t1, count);
		//RunMRE(new PTPlusWINQPlusNPT() , t1, count);
		//RunMRE(rule2, 1, t1, count);
//		RunDE(rule, 8888);p
//		RunDE(rule2, 8888);
		//RunMRE(new MDD(), 1);
		//runSingleExperiment(new MDD(), t1);


		//System.out.println(t2 - t1);
	}
	
	public static void CalculateRanking()
	{
		String benchmarkFile = "E:\\开发必备\\Genetic Programming\\ECJ\\jar\\20171118RS测试\\1000Replications.txt";
		String targetFile = "E:\\开发必备\\Genetic Programming\\ECJ\\jar\\20171118RS测试\\Details_Evaluation.stat";
		//targetFile = "E:\\开发必备\\Genetic Programming\\ECJ\\jar\\20171118RS测试\\RS_热启10R_RS100R_热启种子不同.txt";
		//targetFile = "E:\\开发必备\\Genetic Programming\\ECJ\\jar\\20171118RS测试\\OCBA_10+10_热启种子相同.txt";
		
		//CorrelationTesting test = new CorrelationTesting(benchmarkFile, "E:\\开发必备\\Genetic Programming\\ECJ\\jar\\20171118RS测试\\RS_热启10R_RS10R_热启种子相同.txt");
		CorrelationTesting test = new CorrelationTesting(benchmarkFile, targetFile);
		
		System.out.println(test.getSpearmanRankCoefficient());
		System.out.println(test.getCorrelationCoefficient2());
	}
	
	public static void main33(String[] args) throws RowsExceededException, WriteException, IOException {
	
		GPCompareTest test = new GPCompareTest();
		//test.readDetailsBudgetDistribution(new File("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA1224\\20\\job.0.Details_Evaluation.stat"));
		
		String path = "E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\EA+10R";
		
		int count = 10;
		
		for(int i = 0; i < count; i++)
		{
			String filename = path + "\\job." + i + ".Result_Evaluation.stat";
			test.testEachGenerationRule(new File(filename),
					path);
		}
		
//		List<String> rules = test.testEachGenerationRule(new File("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\EA-20R\\job.0.Result_Evaluation.stat"),
//				"E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\EA-20R\\");
		
	
	}
}
