package jasima.core.experiment;

import java.util.ArrayList;
import java.util.List;

import jasima.core.statistics.SummaryStat;


	
	public class TestConfiguration
	{
		public TestConfiguration()
		{
			InstanceList = new ArrayList<TestEntity>();
		}
		
		public String filePath;
		
		public String ConfigDescription;
		
		public int InstanceCount;
		
		public int BestHit;
		
		public List<TestEntity> InstanceList;
	}
