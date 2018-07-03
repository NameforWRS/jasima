package jasima.core.util;

public enum EJobShopObjectives {
	MeanFlowTime("flowtime"),
	CMAX("Cmax"),
	TotalTardiness("tardiness"),
	TotalWeightedTardiness("tardiness");	
	
	private final String text;
    private EJobShopObjectives(final String text){
        this.text=text;
    }
    @Override
    public String toString(){
        return text;
    }
}
