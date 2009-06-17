package org.rhq.plugins.jbossas5;

public enum ApplicationServerShutdownMethod {

	/** Shutdown via the configured JMX MBean operation. */
	JMX,

	/** Shutdown via the configured Shutdown Script */
	SCRIPT
}
