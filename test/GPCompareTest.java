
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
import jasima.core.experiment.TestEntity;
import jasima.core.random.continuous.DblConst;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.Simulation.SimEvent;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.Util;
import jasima.core.util.observer.NotifierListener;
import jasima.shopSim.core.PR;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment.Scenario;
import jasima.shopSim.prioRules.basic.FASFS;
import jasima.shopSim.prioRules.gp.MyCustomGP;
import jasima.shopSim.prioRules.gp.NormalizedBrankeRule_StringExecution;
import jasima.shopSim.prioRules.meta.IgnoreFutureJobs;
import jasima.shopSim.util.TardinessDeviationJobStatCollector;
import jasima.shopSim.util.BasicJobStatCollector;
import jasima.shopSim.util.DeviationJobStatCollector;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author Cheng
 * @version "$Id$"
 */

// It is a testing class, help us to evaluate the final GP rule's performance
public class GPCompareTest {

	public long InitialSeed = 778899;
	public int NumOfReplications = 200;
	public int Normal = 1;
	
	public double utilizationLevel = 0.95d;
	public int minOPs = 2;
	public int maxOPs = 10;
	public int numMachines = 10;

	public int NumOfInstance;
	public String outputExcelFile = "result";
	
	public int objectives = 11;

	private CompareEntity compareEntity;

	public GPCompareTest(int instance) {
		NumOfInstance = instance;

	}

	public GPCompareTest() {

	}
	
	public void testMultipleRules()
	{
		
	}

	public void TestingFinalEvaluation(String directory, TestConfiguration... configurationsArray) {

		int count = 0;
		int CompareInstanceCount = 0;
		List<TestConfiguration> ConfigurationList = new ArrayList<TestConfiguration>();
		List<SummaryStat> SummaryStatList = new ArrayList<SummaryStat>();
		String outputPath = directory + "\\" + outputExcelFile + "." + this.InitialSeed + ".xls";

		List<File> curFiles = getFileList(directory, "Result", "stat");

		int curConIndex = 0;
		for (int i = 0; i < curFiles.size(); i++) {
			List<String> ruleSet = ReadMultipleRule(curFiles.get(i));

			int j = 0;
			for (TestConfiguration conf : configurationsArray) {

				TestEntity ent = new TestEntity();
				ent.GPRule = ruleSet.get(j).trim();
				ent.FileName = curFiles.get(i).getName();
				j++;

				conf.InstanceList.add(ent);

			}
		}

		for (TestConfiguration conf : configurationsArray) {
			if (conf.InstanceList.size() != conf.InstanceCount) {
				System.out.println("Error: Instance count is not match with file count in Configuration "
						+ conf.ConfigDescription);
				return;
			}

			SummaryStat stat = new SummaryStat();
			SummaryStatList.add(stat);
			ConfigurationList.add(conf);
			CompareInstanceCount = conf.InstanceCount;
			curConIndex++;
		}

		// Now, we get all the instances of test configurations, go to test them
		for (int i = 0; i < NumOfInstance; i++) {
//			Boolean flag = true;
//
//			String curRule = "";
//			ArrayList fitnessArray = new ArrayList();
//			int confIndex = 0;
//			for (TestConfiguration conf : ConfigurationList) {
//				TestEntity ent = conf.InstanceList.get(i);
//
//				// Initialization
//				if (confIndex == 0)
//					curRule = ent.GPRule;
//				else {
//					if (!curRule.equals(ent.GPRule)) {
//						flag = false;
//						break;
//					}
//				}
//
//				confIndex++;
//			}

			// The generated rules are not same, then turn to evaluation
			if (true) {

				double minValue = Double.MAX_VALUE;
				int index = 0;
				List<Integer> BestIndex = new ArrayList<Integer>();

				HashMap hMap = new HashMap();
				for (TestConfiguration conf : ConfigurationList) {
					TestEntity ent = conf.InstanceList.get(i);
					SummaryStat curStat = SummaryStatList.get(index);

					if (hMap.containsKey(ent.GPRule)) {
						ent.EvaluatedSummary = (SummaryStat) hMap.get(ent.GPRule);
					} else {
						ent.EvaluatedSummary = RunMulitipleReplicationTest(ent.GPRule);

						hMap.put(ent.GPRule, ent.EvaluatedSummary);
					}

					curStat.combine(ent.EvaluatedSummary);

					if (ent.EvaluatedSummary.mean() < minValue) {
						minValue = ent.EvaluatedSummary.mean();
						BestIndex.clear();
						BestIndex.add(index);
					} else if (ent.EvaluatedSummary.mean() == minValue) {
						BestIndex.add(index);
					}

					index++;
					
					System.out.println( ent.EvaluatedSummary.mean() + " " + ent.FileName);
				}

				for (int j = 0; j < BestIndex.size(); j++) {
					int ind = (int) BestIndex.get(j);

					ConfigurationList.get(ind).BestHit++;
				}
				
			
			}

		
			System.out.println("Finished Evaluation " + i);
		}

		WritableWorkbook writeBook = null;
		WritableSheet firstSheet = null;
		try {
			writeBook = Workbook.createWorkbook(new File(outputPath));

			// 2、新建工作表(sheet)对象，并声明其属于第几页
			firstSheet = writeBook.createSheet("FirstSheet", 1);// 第一个参数为工作簿的名称，第二个参数为页数
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < NumOfInstance; i++) {
			String curFitness = "";
			int column = 0;
			for (TestConfiguration conf : ConfigurationList) {
				TestEntity ent = conf.InstanceList.get(i);

				curFitness += ent.EvaluatedSummary.mean() + "   ";

				Label label1 = new Label(column, i, Double.toString(ent.EvaluatedSummary.mean()));
				column++;
				Label label2 = new Label(column, i, Double.toString(ent.EvaluatedSummary.stdDev()));
				column++;
				Label label3 = new Label(column, i, Double.toString(ent.EvaluatedSummary.min()));
				column++;
				Label label4 = new Label(column, i, Double.toString(ent.EvaluatedSummary.max()));
				column++;
				column++;
				try {
					firstSheet.addCell(label1);
					firstSheet.addCell(label2);
					firstSheet.addCell(label3);
					firstSheet.addCell(label4);
				} catch (RowsExceededException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WriteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				column++;
			}

			System.out.println("Current Run: " + i);
			System.out.println(curFitness);

		}

		// Output
		String title = "";
		for (TestConfiguration conf : ConfigurationList) {
			title += conf.ConfigDescription + "   ";

			System.out.println(conf.BestHit + "/" + conf.InstanceCount);

			int index = ConfigurationList.indexOf(conf);
			SummaryStat curStat = SummaryStatList.get(index);

			System.out.println(
					"Configuration " + index + "  Mean:" + curStat.mean() + "   Variance: " + curStat.variance());
		}
		System.out.println(title);

		// 4、打开流，开始写文件
		try {
			writeBook.write();
			// 5、关闭流
			writeBook.close();
		} catch (IOException | WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * log final evaluation, special case is t0 information is come from GP run
	 */
	public void CompareAndLogForEvaluation(String log, TestConfiguration... configurationsArray) {
		int count = 0;
		int CompareInstanceCount = 0;
		List<TestConfiguration> ConfigurationList = new ArrayList<TestConfiguration>();

		List<SummaryStat> SummaryStatList = new ArrayList<SummaryStat>();
		int curConIndex = 0;
		for (TestConfiguration conf : configurationsArray) {
			List<File> curFiles = getFileList(conf.filePath, "Result", "stat");

			SummaryStat stat = new SummaryStat();
			SummaryStatList.add(stat);

			for (int i = 0; i < curFiles.size(); i++) {
				try {

					if (curConIndex == 0) {
						// 读取倒数第7行
						//String baseLine = readLastLineFromN(curFiles.get(i), "gbk", 9);
						String baseLine =  readLastLine(curFiles.get(i), "gbk");
						TestEntity ent = new TestEntity();
						ent.GPRule = baseLine.trim();
						ent.FileName = curFiles.get(i).getName();

						conf.InstanceList.add(ent);
					} else 
					{
						String lastLine = readLastLine(curFiles.get(i), "gbk");

						TestEntity ent = new TestEntity();
						ent.GPRule = lastLine.trim();
						ent.FileName = curFiles.get(i).getName();

						conf.InstanceList.add(ent);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (conf.InstanceList.size() != conf.InstanceCount) {
				System.out.println("Error: Instance count is not match with file count in Configuration "
						+ conf.ConfigDescription);
				//return;
			}

			ConfigurationList.add(conf);
			CompareInstanceCount = conf.InstanceCount;
			curConIndex++;

		}

		// Now, we get all the instances of test configurations, go to test them
		for (int i = 0; i < NumOfInstance; i++) {
			Boolean flag = true;

			String curRule = "";
			ArrayList fitnessArray = new ArrayList();
			int confIndex = 0;
			for (TestConfiguration conf : ConfigurationList) {
				TestEntity ent = conf.InstanceList.get(i);

				// Initialization
				if (confIndex == 0)
					curRule = ent.GPRule;
				else {
					if (!curRule.equals(ent.GPRule)) {
						flag = false;
						break;
					}
				}

				confIndex++;
			}

			// The generated rules are not same, then turn to evaluation
			if (true) {

				double minValue = Double.MAX_VALUE;
				int index = 0;
				List<Integer> BestIndex = new ArrayList<Integer>();

				HashMap hMap = new HashMap();
				for (TestConfiguration conf : ConfigurationList) {
					TestEntity ent = conf.InstanceList.get(i);
					SummaryStat curStat = SummaryStatList.get(index);

					if (hMap.containsKey(ent.GPRule)) {
						ent.EvaluatedSummary = (SummaryStat) hMap.get(ent.GPRule);
					} else {
						ent.EvaluatedSummary = RunMulitipleReplicationTest(ent.GPRule);

						hMap.put(ent.GPRule, ent.EvaluatedSummary);
					}

					curStat.combine(ent.EvaluatedSummary);

					if (ent.EvaluatedSummary.mean() < minValue) {
						minValue = ent.EvaluatedSummary.mean();
						BestIndex.clear();
						BestIndex.add(index);
					} else if (ent.EvaluatedSummary.mean() == minValue) {
						BestIndex.add(index);
					}

					index++;
					System.out.println( ent.EvaluatedSummary.mean() + " " + ent.FileName);
				}

				for (int j = 0; j < BestIndex.size(); j++) {
					int ind = (int) BestIndex.get(j);

					ConfigurationList.get(ind).BestHit++;
				}
			}

			System.out.println("Finished Evaluation " + i);
		}

		WritableWorkbook writeBook = null;
		WritableSheet firstSheet = null;
		try {
			writeBook = Workbook.createWorkbook(new File(
					log));

			// 2、新建工作表(sheet)对象，并声明其属于第几页
			firstSheet = writeBook.createSheet("FirstSheet", 1);// 第一个参数为工作簿的名称，第二个参数为页数
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < NumOfInstance; i++) {
			String curFitness = "";
			int column = 0;
			for (TestConfiguration conf : ConfigurationList) {
				TestEntity ent = conf.InstanceList.get(i);

				curFitness += ent.EvaluatedSummary.mean() + "   ";

				Label label1 = new Label(column, i, Double.toString(ent.EvaluatedSummary.mean()));
				Label label2 = new Label(column + 1, i, Double.toString(ent.EvaluatedSummary.stdDev()));
				Label label3 = new Label(column + 2, i, Double.toString(ent.EvaluatedSummary.min()));
				Label label4 = new Label(column + 3, i, Double.toString(ent.EvaluatedSummary.max()));
				try {
					firstSheet.addCell(label1);
					firstSheet.addCell(label2);
					firstSheet.addCell(label3);
					firstSheet.addCell(label4);
				} catch (RowsExceededException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WriteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				column++;
			}

			System.out.println("Current Run: " + i);
			System.out.println(curFitness);

		}

		// Output
		String title = "";
		for (TestConfiguration conf : ConfigurationList) {
			title += conf.ConfigDescription + "   ";

			System.out.println(conf.BestHit + "/" + conf.InstanceCount);

			int index = ConfigurationList.indexOf(conf);
			SummaryStat curStat = SummaryStatList.get(index);

			System.out.println(
					"Configuration " + index + "  Mean:" + curStat.mean() + "   Variance: " + curStat.variance());
		}
		System.out.println(title);

		// 4、打开流，开始写文件
		try {
			writeBook.write();
			// 5、关闭流
			writeBook.close();
		} catch (IOException | WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void CompareAndLog(TestConfiguration... configurationsArray) {
		int count = 0;
		int CompareInstanceCount = 0;
		List<TestConfiguration> ConfigurationList = new ArrayList<TestConfiguration>();

		List<SummaryStat> SummaryStatList = new ArrayList<SummaryStat>();
		for (TestConfiguration conf : configurationsArray) {
			List<File> curFiles = getFileList(conf.filePath, "Result", "stat");

			SummaryStat stat = new SummaryStat();
			SummaryStatList.add(stat);

			for (int i = 0; i < curFiles.size(); i++) {
				try {
					String lastLine = readLastLine(curFiles.get(i), "gbk");

					TestEntity ent = new TestEntity();
					ent.GPRule = lastLine.trim();
					ent.FileName = curFiles.get(i).getName();

					conf.InstanceList.add(ent);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (conf.InstanceList.size() != conf.InstanceCount) {
				System.out.println("Error: Instance count is not match with file count in Configuration "
						+ conf.ConfigDescription);
				return;
			}

			ConfigurationList.add(conf);
			CompareInstanceCount = conf.InstanceCount;
		}

		// Now, we get all the instances of test configurations, go to test them
		for (int i = 0; i < NumOfInstance; i++) {
			Boolean flag = true;

			String curRule = "";
			ArrayList fitnessArray = new ArrayList();
			int confIndex = 0;
			for (TestConfiguration conf : ConfigurationList) {
				TestEntity ent = conf.InstanceList.get(i);

				// Initialization
				if (confIndex == 0)
					curRule = ent.GPRule;
				else {
					if (!curRule.equals(ent.GPRule)) {
						flag = false;
						break;
					}
				}

				confIndex++;
			}

			// The generated rules are not same, then turn to evaluation
			if (true) {

				double minValue = Double.MAX_VALUE;
				int index = 0;
				List<Integer> BestIndex = new ArrayList<Integer>();

				HashMap hMap = new HashMap();
				for (TestConfiguration conf : ConfigurationList) {
					TestEntity ent = conf.InstanceList.get(i);
					SummaryStat curStat = SummaryStatList.get(index);

					if (hMap.containsKey(ent.GPRule)) {
						ent.EvaluatedSummary = (SummaryStat) hMap.get(ent.GPRule);
					} else {
						ent.EvaluatedSummary = RunMulitipleReplicationTest(ent.GPRule);

						hMap.put(ent.GPRule, ent.EvaluatedSummary);
					}

					curStat.combine(ent.EvaluatedSummary);

					if (ent.EvaluatedSummary.mean() < minValue) {
						minValue = ent.EvaluatedSummary.mean();
						BestIndex.clear();
						BestIndex.add(index);
					} else if (ent.EvaluatedSummary.mean() == minValue) {
						BestIndex.add(index);
					}

					index++;
				}

				for (int j = 0; j < BestIndex.size(); j++) {
					int ind = (int) BestIndex.get(j);

					ConfigurationList.get(ind).BestHit++;
				}
			}

			System.out.println("Finished Evaluation " + i);
		}

		WritableWorkbook writeBook = null;
		WritableSheet firstSheet = null;
		try {
			writeBook = Workbook.createWorkbook(new File(
					"E:\\开发必备\\Genetic Programming\\实验数据\\GP Optimization\\500IND+10Gen+STD+Normal+FinalEvaluation\\MeanFlowTime\\ComparedResult.xls"));

			// 2、新建工作表(sheet)对象，并声明其属于第几页
			firstSheet = writeBook.createSheet("FirstSheet", 1);// 第一个参数为工作簿的名称，第二个参数为页数
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < NumOfInstance; i++) {
			String curFitness = "";
			int column = 0;
			for (TestConfiguration conf : ConfigurationList) {
				TestEntity ent = conf.InstanceList.get(i);

				curFitness += ent.EvaluatedSummary.mean() + "   ";

				Label label1 = new Label(column, i, Double.toString(ent.EvaluatedSummary.mean()));
				try {
					firstSheet.addCell(label1);
				} catch (RowsExceededException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WriteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				column++;
			}

			System.out.println("Current Run: " + i);
			System.out.println(curFitness);

		}

		// Output
		String title = "";
		for (TestConfiguration conf : ConfigurationList) {
			title += conf.ConfigDescription + "   ";

			System.out.println(conf.BestHit + "/" + conf.InstanceCount);

			int index = ConfigurationList.indexOf(conf);
			SummaryStat curStat = SummaryStatList.get(index);

			System.out.println(
					"Configuration " + index + "  Mean:" + curStat.mean() + "   Variance: " + curStat.variance());
		}
		System.out.println(title);

		// 4、打开流，开始写文件
		try {
			writeBook.write();
			// 5、关闭流
			writeBook.close();
		} catch (IOException | WriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void WriteLog(String logFilePath) {

	}

	public void Read(String path1, String path2) {
		int count = 0;
		List<File> firstFiles = getFileList(path1, "Result", "stat");
		List<File> secondFiles = getFileList(path2, "Result", "stat");

		List<TestEntity> firstEntities = new ArrayList<TestEntity>();
		List<TestEntity> secondEntities = new ArrayList<TestEntity>();

		for (int i = 0; i < firstFiles.size(); i++) {
			try {
				String lastLine = readLastLine(firstFiles.get(i), "gbk");

				TestEntity ent = new TestEntity();
				ent.GPRule = lastLine.trim();
				ent.FileName = firstFiles.get(i).getName();

				firstEntities.add(ent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (int i = 0; i < secondFiles.size(); i++) {
			try {
				String lastLine = readLastLine(secondFiles.get(i), "gbk");

				TestEntity ent = new TestEntity();
				ent.GPRule = lastLine.trim();
				ent.FileName = secondFiles.get(i).getName();

				secondEntities.add(ent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		int t1Win = 0, t2Win = 0;
		for (int i = 0; i < Math.min(firstEntities.size(), secondEntities.size()); i++) {
			Boolean flag = false;

			TestEntity t1 = firstEntities.get(i);
			TestEntity t2 = secondEntities.get(i);

			if (t1.GPRule.equals(t2.GPRule)) {
				// System.out.println("first is same as second");
				// flag = true;
				t1Win++;
				t2Win++;
				continue;

			} else {
				count++;
				System.out.println("Found Different!!!");
			}

			double firstMean = 0.0, secondMean = 0.0;

			t1.EvaluatedSummary = RunMulitipleReplicationTest(t1.GPRule);

			firstMean = t1.EvaluatedSummary.mean();
			if (!flag) {
				t2.EvaluatedSummary = RunMulitipleReplicationTest(t2.GPRule);
				secondMean = t2.EvaluatedSummary.mean();
			}

			if (firstMean < secondMean) {
				t1Win++;
			} else if (firstMean > secondMean) {
				t2Win++;
			} else {
				t1Win++;
				t2Win++;
			}

			System.out.println(t1.FileName + " " + t2.FileName);
			System.out.println("Rule1: " + t1.GPRule);
			System.out.println("Rule2: " + t2.GPRule);
			System.out.println("Configuration " + i + ":" + firstMean + " " + secondMean);

			System.out.println();
		}

		System.out.println("Total Different count:" + count + "/" + firstEntities.size());
		System.out.println("Rule1 win = " + t1Win + "   Rule2 win =" + t2Win);
	}

	public SummaryStat RunMulitipleReplicationTest(String rule) {
		DynamicShopExperiment e = new DynamicShopExperiment();

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		BasicJobStatCollector basicJobStatCollector = new BasicJobStatCollector();
		basicJobStatCollector.setIgnoreFirst(500);
		basicJobStatCollector.setInitialPeriod(500);

		DeviationJobStatCollector devJobStatCollector = new DeviationJobStatCollector();
		devJobStatCollector.setIgnoreFirst(500);
		devJobStatCollector.setInitialPeriod(500);

		if (this.objectives == 1) {
			e.addShopListener(basicJobStatCollector);
		} else if (this.objectives == 11) {
			e.addShopListener(devJobStatCollector);
		}

		e.setMaxJobsInSystem(500);

		PR sr = null;
		if (Normal == 0)
			sr = new MyCustomGP(rule);
		else
			sr = new NormalizedBrankeRule_StringExecution(rule);

		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);
		e.setNumMachines(numMachines);
		e.setNumOps(this.minOPs, this.maxOPs);
		e.setDueDateFactor(new DblConst(4.0));
		e.setUtilLevel(this.utilizationLevel);
		e.setStopAfterNumJobs(2500);
		e.setScenario(Scenario.JOB_SHOP);
		//e.setInitialSeed(this.InitialSeed);

		MultipleReplicationExperiment mre = new MultipleReplicationExperiment();
		mre.setBaseExperiment(e);
		mre.setMaxReplications(this.NumOfReplications);	
		mre.setInitialSeed(this.InitialSeed);

		mre.runExperiment();

		String obj = Util.getObjectiveString(this.objectives);

		SummaryStat flowtime = (SummaryStat) mre.getResults().get(obj);
		return flowtime;
	}
	
	public void doReadProgramLength(String Path)
	{
		List<File> files = this.getFileList(Path, "Details", "stat");
		
		readStatistics(files.get(0));
	}
	
	public List<Double> readStatistics(File file)
	{
		List<Double> list = new ArrayList<Double>();
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			// 一次读入一行，直到读入null为文件结束
			int i = 0;
			while ((tempString = reader.readLine()) != null) {
				if(tempString.contains("["))
				{
					Pattern p = Pattern.compile("(\\[[^\\]]*\\])");
					Matcher m = p.matcher(tempString);
					while(m.find())
					{
						if(i == 0)						
							i++;
						else
						{
							i = 0;
							String size = m.group(0).substring(2, m.group(0).length() - 2);							
							
							try
							{
								Double dsize = Double.parseDouble(size);
								list.add(Double.parseDouble(size));
								System.out.println(size);
								
							}
							catch(Exception e)
							{
								break;
							}
						}
					}
				}

			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
			
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
		
		return list;
	}

	public String readLastLineFromN(File file, String charset, int n) {
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return null;
		}

		ArrayList q = new ArrayList();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LineNumberReader reader = new LineNumberReader(in);
		String s;
		int lines = 0;
		try {
			s = reader.readLine();

			while (s != null) {
				lines++;
				s = reader.readLine();
				q.add(s);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (String) q.get(lines - n + 1);
	}

	//This method reads the last line where the best rule found
	public String readLastLine(File file, String charset) throws IOException {
		if (!file.exists() || file.isDirectory() || !file.canRead()) {
			return null;
		}
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			long len = raf.length();
			if (len == 0L) {
				return "";
			} else {
				long pos = len - 1;
				while (pos > 0) {
					pos--;
					raf.seek(pos);
					if (raf.readByte() == '\n') {
						break;
					}
				}
				if (pos == 0) {
					raf.seek(0);
				}
				byte[] bytes = new byte[(int) (len - pos)];
				raf.read(bytes);
				if (charset == null) {
					return new String(bytes);
				} else {
					return new String(bytes, charset);
				}
			}
		} catch (FileNotFoundException e) {
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Exception e2) {
				}
			}
		}
		return null;
	}

	
	//This method is proposed to read different rules found by various evaluator, you need to know the order of evaluator first
	public List<String> ReadMultipleRule(File file) {
		List<String> ruleSet = new ArrayList();

		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {

				if (tempString.trim().contains("Original Best Rule")) {
					// 读取原始rule
					String originalRule = reader.readLine().trim();
					ruleSet.add(originalRule);
				} else if (tempString.trim().contains("Evaluator OCBA")) {
					String ocbaRule = reader.readLine().trim();
					ruleSet.add(ocbaRule);
				} else if (tempString.trim().contains("Evaluator KG")) {
					String kgRule = reader.readLine().trim();
					ruleSet.add(kgRule);
				} else if (tempString.trim().contains("Evaluator AOAP")) {
					String aoapRule = reader.readLine().trim();
					ruleSet.add(aoapRule);
				}else if (tempString.trim().contains("Evaluator IDOCBA")) {
					String aoapRule = reader.readLine().trim();
					ruleSet.add(aoapRule);
				}
				else if (tempString.trim().contains("Evaluator EA")) {
					String eaRule = reader.readLine().trim();
					ruleSet.add(eaRule);
				}
				else if (tempString.trim().contains("Evaluator RS")) {
					String rsRule = reader.readLine().trim();
					ruleSet.add(rsRule);
				}

			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}

		return ruleSet;
	}
	
	public List<String>  testEachGenerationRule(File file, String path) throws IOException, WriteException
	{
		WritableWorkbook writeBook = null;
		WritableSheet firstSheet = null;
		WritableSheet secondSheet = null;
		
		List<String> rules = new ArrayList();
	
		writeBook = Workbook.createWorkbook(new File(path +"\\" + file.getName()+  ".xls"));
		firstSheet = writeBook.createSheet("Samples", 1);// 第一个参数为工作簿的名称，第二个参数为页数
		
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int i = 1;
			int gen = 0;
			while ((tempString = reader.readLine()) != null) {
				if(tempString.contains("Tree 0:"))
				{
					tempString = reader.readLine();
					rules.add(tempString);
					
					SummaryStat s = RunMulitipleReplicationTest(tempString.trim());
					s.mean();
					
					Label label1 = new Label(1, i, Double.toString(s.mean()));

					firstSheet.addCell(label1);
					i++;
				}
			}
			reader.close();
			
			writeBook.write();
			
			rules.remove(rules.size() - 1);
	
			writeBook.close();
			
		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
		
		return rules;
	}
	
	public void readDetailsBudgetDistribution(File file) throws IOException, RowsExceededException, WriteException
	{
		
		WritableWorkbook writeBook = null;
		WritableSheet firstSheet = null;
		WritableSheet secondSheet = null;
	
		writeBook = Workbook.createWorkbook(new File("E:\\开发必备\\Genetic Programming\\实验数据\\测试TSIDOCBA\\Experiment\\Full GP Run\\Full+TSOCBA1224\\20\\0.xls"));

			// 2、新建工作表(sheet)对象，并声明其属于第几页
	    firstSheet = writeBook.createSheet("Samples", 1);// 第一个参数为工作簿的名称，第二个参数为页数
	    secondSheet = writeBook.createSheet("Fitness", 1);// 第一个参数为工作簿的名称，第二个参数为页数
	

		
		
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int i = 1;
			int gen = 0;
			while ((tempString = reader.readLine()) != null) {
				if(tempString.contains("["))
				{
					i = 1;	
					gen++;
				}
				else
				{
					if(i > 400)
					{
						continue;
					}
					
					if (!tempString.contains("Evaluations") && i == 1)
						break;

					String[] ts = tempString.split("Evaluations:");

					String mean = ts[0].split("stddev")[0];

					String meand = mean.replace("mean:", "").trim();

					String[] ds = ts[1].split("DR");

					System.out.println(ds[0].trim());

					Label label1 = new Label(gen, i, ds[0]);

					firstSheet.addCell(label1);

					Label label2 = new Label(gen, i,  meand);

					secondSheet.addCell(label2);

					i++;
					
				}

			}
			reader.close();
			
			writeBook.write();
			// 5、关闭流
			writeBook.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	public void ReadBestRuleFromFile(File file) {
		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int line = 1;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				// 显示行号
				//System.out.println("line " + line + ": " + tempString);
				line++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	}

	public List<File> getFileList(String strPath, String prefix, String endfix) {
		List<File> filelist = new ArrayList<File>();
		File dir = new File(strPath);
		File[] files = dir.listFiles(); // 该文件目录下文件全部放入数组

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				long diff = f1.lastModified() - f2.lastModified();
				if (diff > 0)
					return 1;
				else if (diff == 0)
					return 0;
				else
					return -1;
			}

			public boolean equals(Object obj) {
				return true;
			}

		});

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i].getName();
				if (files[i].isDirectory()) { // 判断是文件还是文件夹
					getFileList(files[i].getAbsolutePath(), prefix, endfix); // 获取文件绝对路径
				} else if (fileName.contains(prefix) && fileName.endsWith(endfix)) { // 判断文件名是否以.stat结尾
					String strFileName = files[i].getAbsolutePath();
					// System.out.println("---" + strFileName);
					filelist.add(files[i]);
				} else {
					continue;
				}
			}

		}
		return filelist;
	}
}
