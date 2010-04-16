package org.rhq.NagiosMonitor;

public class Host 
{
	private String hostName;
	private ServiceData serviceData;
	private Status status;
	
	public Host(String hostName)
	{
		this.hostName = hostName;
	}
}
