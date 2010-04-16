package org.rhq.NagiosMonitor;

public interface NagiosData 
{
	public void setRequestType();
	public String getRequestType();
	
	public void fillWithData(String[] ressourceNames, String[] metricNames, String[] metricValues);
	public Metric getSingleMetricForRessource(String metricName, String ressourceName) 
		throws InvalidMetricRequestException, InvalidServiceRequestException;
}
