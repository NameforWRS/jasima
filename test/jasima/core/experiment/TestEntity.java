package jasima.core.experiment;
import java.util.ArrayList;
import java.util.List;
import jasima.core.statistics.SummaryStat;

public class TestEntity
	{
		public TestEntity()
		{
			EvaluatedSummary = new SummaryStat();
			EvaluatedRawSummary = new SummaryStat();
		}
		public String GPRule;
		
		public String FileName;
		
		public String Evaluator;
		
		public SummaryStat EvaluatedSummary;
		
		public SummaryStat EvaluatedRawSummary;
	}
	
	
