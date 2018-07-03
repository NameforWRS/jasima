package jasima.shopSim.util;

import java.math.BigDecimal;
import java.util.Map;

import jasima.core.random.continuous.DblConst;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.Simulation.SimEvent;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.observer.NotifierListener;
import jasima.shopSim.core.PR;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment;
import jasima.shopSim.models.dynamicShop.DynamicShopExperiment.Scenario;
import jasima.shopSim.prioRules.basic.FASFS;
import jasima.shopSim.prioRules.basic.FCFS;
import jasima.shopSim.prioRules.meta.IgnoreFutureJobs;

public class RunMultipleDynamicShopSimulation {
	
	public long InitialSeed = 8841;
	public DynamicShopExperiment dsExp;
	
	public SummaryStat runSimulation()
	{
		dsExp = new DynamicShopExperiment();
		dsExp.setInitialSeed(InitialSeed);

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
		
		PR sr = new FCFS();
		PR sr2 = new IgnoreFutureJobs(sr);
		PR sr3 = new FASFS();
		sr2.setTieBreaker(sr3);
		dsExp.setSequencingRule(sr2);
		
		dsExp.runExperiment();
		
		Map<String, Object> res = dsExp.getResults();
		SummaryStat flowtime = (SummaryStat)res.get("flowtime");
		
		return null;
	}

}
