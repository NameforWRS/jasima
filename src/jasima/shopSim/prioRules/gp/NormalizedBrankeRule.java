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

import ec.app.batch.DoubleData;
import ec.gp.GPNode;
import jasima.shopSim.core.PrioRuleTarget;
import jasima.shopSim.prioRules.upDownStream.PTPlusWINQPlusNPT;

/**
 * A rule from "Towards Improved Dispatching Rules for Complex Shop Floor
 * Scenarios—a Genetic Programming Approach", Hildebrandt, Heger, Scholz-Reiter,
 * GECCO 2010, doi:10.1145/1830483.1830530
 * 
 * @author Torsten Hildebrandt
 * @version 
 *          "$Id$"
 */
public class NormalizedBrankeRule extends GPRuleBase {

	private static final long serialVersionUID = 8165075973248667950L;
	
	public GPNode rootNode;
	
	public NormalizedBrankeRule(GPNode node)
	{		
		rootNode = node;
	}

	@Override
	public double calcPrio(PrioRuleTarget j) {
	
//		double PT = j.getCurrentOperation().procTime;
//		double WINQ = jasima.shopSim.prioRules.upDownStream.XWINQ.xwinq(j);
//		double TIQ = j.getShop().simTime() - j.getArriveTime();
//		double TIS = j.getShop().simTime() - j.getRelDate();
//		double RPT = j.remainingProcTime();
//		double NPT = PTPlusWINQPlusNPT.npt(j);
//		double OpsLeft = j.numOpsLeft();
//		
//		DoubleData input = new DoubleData();
//		
//		//Begin to normalize feature values according to Branke 2015
//		
//		input.NPT = GetNormalizedValue(NPT, 0, 47);
//		input.OpsLeft = GetNormalizedValue(OpsLeft, 1, 10);
//		input.PT = GetNormalizedValue(PT, 1, 47);
//		input.WINQ = GetNormalizedValue(WINQ, 0, 410);
//		input.RPT = GetNormalizedValue(RPT, 1, 264);
//		input.TIQ = GetNormalizedValue(TIQ, 0, 1500);
//		input.TIS = GetNormalizedValue(TIS, 0, 2770);
//		
//		rootNode.evalSimple(input);

		//return input.x;
		return 0.0;
	}

}
