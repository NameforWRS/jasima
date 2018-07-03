import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CorrelationTesting {

	public List<TargetRule> benchmarkRules;
	public List<TargetRule> targetRules;
	
	public CorrelationTesting(String benchmarkFile, String targetFile)
	{
		benchmarkRules = ReadBestRuleFromFile(benchmarkFile);
		targetRules = ReadBestRuleFromFile(targetFile);
	}
	
	public List<TargetRule> ReadBestRuleFromFile(String strPath) {
		
		File file = new File(strPath);
		BufferedReader reader = null;
		List<TargetRule> result = new ArrayList<TargetRule>();
		try {

			reader = new BufferedReader(new FileReader(file));
			String tempString = null;
			int line = 1;
			// 一次读入一行，直到读入null为文件结束
			while ((tempString = reader.readLine()) != null) {
				
				if(!tempString.contains("DR:"))
				{
					continue;
				}
				
				TargetRule targetRule = new TargetRule();
				targetRule.rank = line;
				
				String[] strs = tempString.split(" ");
				
				if(strs.length > 1)
				{
					String[] substrs = strs[0].split(":");
					targetRule.mean = Double.parseDouble(substrs[1].trim());
					
					substrs = strs[3].split(":");
					targetRule.hashCode = Integer.parseInt(substrs[1].trim());				
				}
				
				strs = tempString.split("DR:");
				targetRule.rule = strs[1].trim();
				
				result.add(targetRule);
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
		
		return result;
	}
	
	public double getSpearmanRankCoefficient()
	{
		double result = 0;
		int n = this.benchmarkRules.size();
		
		int top = 0;
		for(int i = 0; i < n; i++)
		{
			TargetRule curBRule = this.benchmarkRules.get(i);
			String curBCode = curBRule.rule;
			int curRank = curBRule.rank;
			
			int curRefRank = curRank;
			
			for(int j = 0; j < n; j++)
			{
				TargetRule curRRule = this.targetRules.get(j);
				if(curRRule.rule.compareTo(curBCode)==0)
				{
					curRefRank = curRRule.rank;
					break;
				}
			}
			
			top += (curRank - curRefRank) * (curRank - curRefRank);
		}
		
		result = 1 - 6.0 * top / (n * (n * n - 1));
		
		return result;
	}
	
	public double getCorrelationCoefficient()
	{
		double mean_benchmark = 0.0d;
		double mean_target = 0.0d;
		double result;
		int n = this.benchmarkRules.size();
		
		for(int i = 0; i < this.benchmarkRules.size(); i++)
		{
			mean_benchmark += this.benchmarkRules.get(i).mean;
		}
		
		for(int i = 0; i < this.targetRules.size(); i++)
		{
			mean_target += this.targetRules.get(i).mean;
		}
		
		mean_benchmark = mean_benchmark / n;
		mean_target = mean_target / n;
		
		double top = 0;
		
		for(int i = 0; i < n; i++)
		{
			TargetRule curBRule = this.benchmarkRules.get(i);
			String curBCode = curBRule.rule;
			double curRank = curBRule.mean;
			
			double curRefRank = curRank;
			
			for(int j = 0; j < n; j++)
			{
				TargetRule curRRule = this.targetRules.get(j);
				if(curRRule.rule.compareTo(curBCode)==0)
				{
					curRefRank = curRRule.mean;
					break;
				}
			}
			
			top += (curRank - mean_benchmark)*(curRefRank - mean_target);
		}
		
		double bottom_B = 0;		
		for(int i = 0; i < n; i++)
		{
			TargetRule curBRule = this.benchmarkRules.get(i);
			bottom_B += (curBRule.mean - mean_benchmark) * (curBRule.mean - mean_benchmark);
		}
		
		double bottom_R = 0;	
		for(int i = 0; i < n; i++)
		{
			TargetRule curRRule = this.targetRules.get(i);
			bottom_R += (curRRule.mean - mean_target) * (curRRule.mean - mean_target);
		}
		
		double bottom = Math.sqrt(bottom_B * bottom_R);
		
		result = top / bottom;
		
		return result;	
		
	}
	
	public double getCorrelationCoefficient2()
	{
		double mean_benchmark = 0.0d;
		double mean_target = 0.0d;
		double result;
		int n = this.benchmarkRules.size();
		
		for(int i = 0; i < this.benchmarkRules.size(); i++)
		{
			mean_benchmark += this.benchmarkRules.get(i).mean;
		}
		
		for(int i = 0; i < this.targetRules.size(); i++)
		{
			mean_target += this.targetRules.get(i).mean;
		}
		
		mean_benchmark = mean_benchmark / n;
		mean_target = mean_target / n;
		
		double top = 0;
		
		for(int i = 0; i < n; i++)
		{
			TargetRule curBRule = this.benchmarkRules.get(i);
			String curBCode = curBRule.rule;
			double curRank = curBRule.rank;
			
			double curRefRank = curRank;
			
			for(int j = 0; j < n; j++)
			{
				TargetRule curRRule = this.targetRules.get(j);
				if(curRRule.rule.compareTo(curBCode)==0)
				{
					curRefRank = curRRule.rank;
					break;
				}
			}
			
			top += (curRank - mean_benchmark)*(curRefRank - mean_target);
		}
		
		double bottom_B = 0;		
		for(int i = 0; i < n; i++)
		{
			TargetRule curBRule = this.benchmarkRules.get(i);
			bottom_B += (curBRule.rank - mean_benchmark) * (curBRule.rank - mean_benchmark);
		}
		
		double bottom_R = 0;	
		for(int i = 0; i < n; i++)
		{
			TargetRule curRRule = this.targetRules.get(i);
			bottom_R += (curRRule.rank - mean_target) * (curRRule.rank - mean_target);
		}
		
		double bottom = Math.sqrt(bottom_B * bottom_R);
		
		result = top / bottom;
		
		return result;	
		
	}
	
}
