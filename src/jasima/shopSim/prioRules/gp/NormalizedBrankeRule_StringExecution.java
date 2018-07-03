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
package jasima.shopSim.prioRules.gp;

import jasima.shopSim.core.PrioRuleTarget;
import jasima.shopSim.prioRules.basic.SLK;
import jasima.shopSim.prioRules.upDownStream.PTPlusWINQPlusNPT;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
/**
 * A rule from "Towards Improved Dispatching Rules for Complex Shop Floor
 * Scenarios—a Genetic Programming Approach", Hildebrandt, Heger, Scholz-Reiter,
 * GECCO 2010, doi:10.1145/1830483.1830530
 * 
 * @author Torsten Hildebrandt
 * @version 
 *          "$Id$"
 */
public class NormalizedBrankeRule_StringExecution extends GPRuleBase {

	private static final long serialVersionUID = 8165075973248667950L;
	
	public String GPRuleExpression; 
	
	public Expression compiledExp;
	
	public Map<String, Object> env2 = new HashMap<String, Object>();
	
	
	public NormalizedBrankeRule_StringExecution(String rep)
	{		
     try {
    	 	GPRuleExpression = rep;
 		
    		AviatorEvaluator.addFunction(new If3Function());
    		AviatorEvaluator.addFunction(new MaxFunction());
    		AviatorEvaluator.addFunction(new DivProtectedFunction());
    		AviatorEvaluator.setOptimize(AviatorEvaluator.EVAL);
    	 	compiledExp = AviatorEvaluator.compile(GPRuleExpression, true);
        	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			  System.out.println(rep);
		}  
		
	}
		
	public void testCalculation()
	{
		
		env2.put("PT", 1.0);
		env2.put("WINQ", 1.0);
		env2.put("TIQ", 1.0);
		env2.put("TIS", 1.0);
		env2.put("RPT", 1.0);
		env2.put("NPT", 1.0);
		env2.put("OpsLeft", 1.0);
		
		for(int i =0; i < 1000000; i++)
		{
		//	AviatorEvaluator.execute(GPRuleExpression, env2);  
			compiledExp.execute(env2);
		}
	}
	

	@Override
	public double calcPrio(PrioRuleTarget j) {
	
		double PT = j.getCurrentOperation().procTime;
		double WINQ = jasima.shopSim.prioRules.upDownStream.XWINQ.xwinq(j);
		double TIQ = j.getShop().simTime() - j.getArriveTime();
		double TIS = j.getShop().simTime() - j.getRelDate();
		double RPT = j.remainingProcTime();
		double NPT = PTPlusWINQPlusNPT.npt(j);
		double OpsLeft = j.numOpsLeft();
		NPT = GetNormalizedValue(NPT, 0, 47);
		OpsLeft = GetNormalizedValue(OpsLeft, 1, 10);
		PT = GetNormalizedValue(PT, 1, 47);
		WINQ = GetNormalizedValue(WINQ, 0, 410);
		RPT = GetNormalizedValue(RPT, 1, 264);
		TIQ = GetNormalizedValue(TIQ, 0, 1500);
		TIS = GetNormalizedValue(TIS, 0, 2770);

		Map<String, Object> env = new HashMap<String, Object>();
		env.put("PT", PT);
		env.put("WINQ", WINQ);
		env.put("TIQ", TIQ);
		env.put("TIS", TIS);
		env.put("RPT", RPT);
		env.put("NPT", NPT);
		env.put("OpsLeft", OpsLeft);
		//env.put("SLACK", SLACK);
		//env.put("TD", TD);
		//env.put("ODD", ODD);	
		
		//System.out.println(GPRuleExpression);
		
		  try {
			    double result = (double) compiledExp.execute(env);	
				return result;
	        	
			} catch (Exception e) {
				System.out.println(e.getMessage() + " " + GPRuleExpression);
				// TODO Auto-generated catch block
				  return Double.MAX_VALUE;
			}  
	}

}
