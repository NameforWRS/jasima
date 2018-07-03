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
import static org.junit.Assert.assertEquals;

import jasima.core.random.RandomFactory;
import jasima.core.random.RandomFactoryOld;
import jasima.core.random.continuous.DblConst;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.Simulation.SimEvent;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.observer.NotifierListener;
import jasima.shopSim.core.PR;
import jasima.shopSim.core.batchForming.HighestJobBatchingMBS;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment.Scenario;
import jasima.shopSim.prioRules.basic.ATC;
import jasima.shopSim.prioRules.basic.FASFS;
import jasima.shopSim.prioRules.basic.FCFS;
import jasima.shopSim.prioRules.basic.MDD;
import jasima.shopSim.prioRules.basic.MOD;
import jasima.shopSim.prioRules.basic.ODD;
import jasima.shopSim.prioRules.basic.SLK;
import jasima.shopSim.prioRules.basic.SPT;
import jasima.shopSim.prioRules.gp.GECCO2010_genSeed_10reps;
import jasima.shopSim.prioRules.gp.GECCO2010_genSeed_2reps;
import jasima.shopSim.prioRules.gp.MyCustomGP;
import jasima.shopSim.prioRules.meta.IgnoreFutureJobs;
import jasima.shopSim.prioRules.upDownStream.PTPlusWINQPlusNPT;
import jasima.shopSim.util.BasicJobStatCollector;
import jasima.shopSim.util.BatchStatCollector;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import util.ExtendedJobStatCollector;

/**
 * 
 * @author Cheng
 * @version "$Id$"
 */
@SuppressWarnings("deprecation")
public class MyfirstDynamicJSTest {

	@Before
	public void setUp() {
		System.out.println("setting up");
		System.setProperty(RandomFactory.RANDOM_FACTORY_PROP_KEY,
				RandomFactoryOld.class.getName());
	}

	public void DoDJSSP(String curGPRule)
	{	
		DynamicShopExperiment e = new DynamicShopExperiment();
		e.setInitialSeed(83461);

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		//e.addShopListener(new ExtendedJobStatCollector());

		PR sr = new MyCustomGP(curGPRule);

		PR sr2 = new IgnoreFutureJobs(sr);
		// PR<Object, Job> sr2 = new InversRule(new InversRule(sr));

		PR sr3 = new FASFS();
		// sr.setTieBreaker(sr2);
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);

		e.setNumMachines(10);
		e.setNumOps(10, 10);
		e.setDueDateFactor(new DblConst(4.0));
		e.setUtilLevel(0.95d);
		e.setStopArrivalsAfterNumJobs(1);
		e.setScenario(Scenario.JOB_SHOP);

		Map<String, Object> res = e.getResults();

		System.out.println(res.get("cMax"));

	}
	
	public static void testMultipleDJSSP()
	{
		ArrayList<DynamicShopExperiment> multipleDynamicExps = new ArrayList<DynamicShopExperiment>();
		
		long InitialSeed = 83461; 
		
		
		for(int i = 0; i < 10; i++)
		{
			DynamicShopExperiment e = new DynamicShopExperiment(); 
			
			e.setInitialSeed(InitialSeed);
			

			// remove default BasicJobStatCollector
			NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
			assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
			e.setShopListener(null);

			e.addShopListener(new ExtendedJobStatCollector());


			PR sr = new GECCO2010_genSeed_2reps();
			PR sr2 = new IgnoreFutureJobs(sr);
			// PR<Object, Job> sr2 = new InversRule(new InversRule(sr));

			PR sr3 = new FASFS();
			sr2.setTieBreaker(sr3);
			e.setSequencingRule(sr2);

			e.setNumMachines(10);
			e.setNumOps(10, 10);
			e.setDueDateFactor(new DblConst(4.0));
			e.setUtilLevel(0.85d);
			e.setStopAfterNumJobs(2500);
			e.setScenario(Scenario.JOB_SHOP);
			
			e.runExperiment();
			//e.printResults();
			System.out.println(InitialSeed);
			Map<String, Object> res = e.getResults();
			System.out.println(sr.toString() + " "  + res.get("cMax"));
			Random seedStream = new Random(InitialSeed);
			InitialSeed = seedStream.nextLong();
			
			
		}
	}	

	@Test
	public static void main(String[] args) throws Exception {

		//testMultipleDJSSP();
		Map<String, Object> env = new HashMap<String, Object>();
		double PT = 1;
		double WINQ = 1;
		double TIQ = 1;
		double TIS = 1;
		double RPT = 1;
		double NPT = 1;
		double OpsLeft = 1;
		double SLACK = 1;
		double TD = 1;
		double ODD = 1;

		env.put("PT", PT);
		env.put("WINQ", WINQ);
		env.put("TIQ", TIQ);
		env.put("TIS", TIS);
		env.put("RPT", RPT);
		env.put("NPT", NPT);
		env.put("OpsLeft", OpsLeft);
		env.put("SLACK", SLACK);
		env.put("TD", TD);
		env.put("ODD", ODD);

		AviatorEvaluator.addFunction(new If3Function());
		AviatorEvaluator.addFunction(new MaxFunction());
		AviatorEvaluator.addFunction(new DivProtectedFunction());
		AviatorEvaluator.addFunction(new AddFunction());
		AviatorEvaluator.addFunction(new SubtractFunction());
		AviatorEvaluator.addFunction(new MultiplyFunction());

		double result = (double) AviatorEvaluator.execute("1.2", env);		

		Expression compiledExp =
				AviatorEvaluator.compile("sub(add(max(add(1, TIQ), add(0, WINQ)), If(sub(TIQ, ODD), div(1, TD), div(TD, PT))), mul(If(max(0, NPT), div(TIQ, SLACK), mul(TD, TIQ)), sub(mul(1, NPT), If(TIS, PT, TIQ))))");
		//System.out.println(result);


		DynamicShopExperiment e = new DynamicShopExperiment();
		e.setInitialSeed(83461);

		// remove default BasicJobStatCollector
		NotifierListener<Simulation, SimEvent>[] l = e.getShopListener();
		assert l.length == 1 && l[0] instanceof BasicJobStatCollector;
		e.setShopListener(null);

		e.addShopListener(new ExtendedJobStatCollector());


		PR sr = new MyCustomGP("PT*NPT");
		PR sr2 = new IgnoreFutureJobs(sr);
		// PR<Object, Job> sr2 = new InversRule(new InversRule(sr));

		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		e.setSequencingRule(sr2);

		e.setNumMachines(10);
		e.setNumOps(10, 10);
		e.setDueDateFactor(new DblConst(4.0));
		e.setUtilLevel(0.85d);
		e.setStopAfterNumJobs(1000);
		e.setScenario(Scenario.JOB_SHOP);


		ArrayList<PR> ruleSet = new ArrayList<PR>();
		ruleSet.add(new MOD());
		ruleSet.add(new SPT());
		ruleSet.add(new MDD());
		ruleSet.add(new ODD());	
		ruleSet.add(new SLK());
		ruleSet.add(new ATC());

		/*for(int i = 0; i < 1; i++)
		{
		
		for(PR pr : ruleSet)
		{
			e.setSequencingRule(pr);
			e.runExperiment();
			//e.printResults();

			Map<String, Object> res = e.getResults();

			System.out.println(pr.toString() + " "  + res.get("cMax"));
		}
		}*/

		
		e.runExperiment();
		e.printResults();

		Map<String, Object> res = e.getResults();

		System.out.println(res.get("cMax"));

	}



}

class MaxFunction extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		Number left = FunctionUtils.getNumberValue(arg1, env);
		Number right = FunctionUtils.getNumberValue(arg2, env);
		return new AviatorDouble(Math.max(left.doubleValue(), right.doubleValue()));
	}
	public String getName() {
		return "max";
	}
}

class DivProtectedFunction extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		Number left = FunctionUtils.getNumberValue(arg1, env);
		Number right = FunctionUtils.getNumberValue(arg2, env);

		if(right.doubleValue() == 0.0)
			return new AviatorDouble(1.0);
		else
			return new AviatorDouble(left.doubleValue() / right.doubleValue());

	}
	public String getName() {
		return "div";
	}
}

class If3Function extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {

		Number a = FunctionUtils.getNumberValue(arg1, env);
		Number b = FunctionUtils.getNumberValue(arg2, env);
		Number c = FunctionUtils.getNumberValue(arg3, env);

		if(a.doubleValue() >= 0.0)
		{
			return new AviatorDouble(b.doubleValue());
		}
		else
		{
			return new AviatorDouble(c.doubleValue());
		}
	}

	public String getName() {
		return "If";
	}
}

class MultiplyFunction extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		Number left = FunctionUtils.getNumberValue(arg1, env);
		Number right = FunctionUtils.getNumberValue(arg2, env);

		return new AviatorDouble(left.doubleValue() + right.doubleValue());

	}
	public String getName() {
		return "mul";
	}
}

class AddFunction extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		Number left = FunctionUtils.getNumberValue(arg1, env);
		Number right = FunctionUtils.getNumberValue(arg2, env);


		return new AviatorDouble(left.doubleValue() + right.doubleValue());

	}
	public String getName() {
		return "add";
	}
}

class SubtractFunction extends AbstractFunction {
	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		Number left = FunctionUtils.getNumberValue(arg1, env);
		Number right = FunctionUtils.getNumberValue(arg2, env);

		return new AviatorDouble(left.doubleValue() - right.doubleValue());

	}
	public String getName() {
		return "sub";
	}
}

