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
public class TempRule extends GPRuleBase {

	private static final long serialVersionUID = 8165075973248667950L;

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

		return ifte(ifte(RPT - PT, ifte(OpsLeft, PT, WINQ), WINQ - TIQ) + divProtected(max(TIS, PT), ifte(OpsLeft, TIQ, OpsLeft)), divProtected(max(OpsLeft, NPT), ifte(OpsLeft, WINQ, TIQ)) - (max(WINQ, RPT) - (TIS - TIS)), ifte(this.divProtected(WINQ * PT, TIQ + NPT), ifte(OpsLeft + TIS, 0.03657012705872442 + WINQ, RPT - 0.44474063277715326), (NPT * RPT) + (0.8793593843916789 * 0.19854442300511077)));
	}

}
