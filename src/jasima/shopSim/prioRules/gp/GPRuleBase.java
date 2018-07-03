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

import jasima.shopSim.core.PR;
import jasima.shopSim.core.PrioRuleTarget;

import java.util.Map;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;

/**
 * Abstract base class that can be used for rules generated with Genetic
 * Programming. In addition to standard {@code PR} it only defines static
 * methods for some commonly used arithmetic functions.
 * 
 * @author Torsten Hildebrandt
 * @version 
 *          "$Id$"
 */
@SuppressWarnings("serial")
public abstract class GPRuleBase extends PR {

	@Override
	public abstract double calcPrio(PrioRuleTarget j);

	public static final double ifte(final double cond, final double ifVal,
			final double elseVal) {
		if (cond >= 0.0d)
			return ifVal;
		else
			return elseVal;
	}

	protected double GetNormalizedValue(double curValue, double min, double max)
	{
		double result = 2*(curValue - min)/(max - min);
		
		return result;
	}
	
	public static final double add(final double v1, final double v2) {
		return v1 + v2;
	}

	public static final double mul(final double v1, final double v2) {
		return v1 * v2;
	}

	public static final double div(final double v1, final double v2) {
		return v1 / v2;
	}

	public static final double divProtected(final double v1, final double v2) {
		if (v2 == 0.0)
			return 1.0;
		else
			return v1 / v2;
	}

	public static final double sub(final double v1, final double v2) {
		return v1 - v2;
	}

	public static final double max(final double v1, final double v2) {
		return Math.max(v1, v2);
	}

	public static final double min(final double v1, final double v2) {
		return Math.min(v1, v2);
	}

	public static final double neg(final double v1) {
		return -v1;
	}

	public static final double abs(double v1) {
		return Math.abs(v1);
	}
	
	public class MaxFunction extends AbstractFunction {
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
	
	public class DivProtectedFunction extends AbstractFunction {
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
	
	public class MultiplyFunction extends AbstractFunction {
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
	
	public class AddFunction extends AbstractFunction {
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
	
	public class SubtractFunction extends AbstractFunction {
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
	
	public class If3Function extends AbstractFunction {
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

}
