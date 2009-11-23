package org.rhq.augeas.config;

import java.util.List;

public interface AugeasConfiguration {

	public List<AugeasModuleConfig> getModules();
	public String getRootPath();
	public String getLoadPath();
	public int getMode();
}
