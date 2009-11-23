package org.rhq.augeas.config;

import java.util.ArrayList;
import java.util.List;

public class AugeasConfigurationSimple implements AugeasConfiguration{

	private String loadPath;
	private int mode;
	private String rootPath;
	private List<AugeasModuleConfig> modules;
	
	public void setLoadPath(String loadPath) {
		this.loadPath = loadPath;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public void setModules(List<AugeasModuleConfig> modules) {
		this.modules = modules;
	}

	public AugeasConfigurationSimple()
	{
	   modules = new ArrayList<AugeasModuleConfig>();	
	}
	
	public String getLoadPath() {
		return loadPath;
	}

	
	public int getMode() {	
		return mode;
	}
	
	public List<AugeasModuleConfig> getModules() {
		return modules;
	}

	
	public String getRootPath() {	
		return rootPath;
	}

	public void addModuleConfig(AugeasModuleConfig config)
	{
		if (modules.contains(config))
			return;
		modules.add(config);			
	}
}
