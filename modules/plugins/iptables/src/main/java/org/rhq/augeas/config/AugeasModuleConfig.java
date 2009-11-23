package org.rhq.augeas.config;

import java.util.ArrayList;
import java.util.List;

public class AugeasModuleConfig {

	private String moduletName;
	private String lensPath;
	private List<String> excludedGlobs;
	private List<String> includedGlobs;
	
	public AugeasModuleConfig(){
		excludedGlobs = new ArrayList<String>();
		includedGlobs = new ArrayList<String>();
	}

	public String getModuletName() {
		return moduletName;
	}

	public void setModuletName(String moduletName) {
		this.moduletName = moduletName;
	}

	public String getLensPath() {
		return lensPath;
	}

	public void setLensPath(String lensPath) {
		this.lensPath = lensPath;
	}

	public List<String> getExcludedGlobs() {
		return excludedGlobs;
	}

	public void setExcludedGlobs(List<String> excludedGlobs) {
		this.excludedGlobs = excludedGlobs;
	}

	public List<String> getIncludedGlobs() {
		return includedGlobs;
	}

	public void setIncludedGlobs(List<String> includedGlobs) {
		this.includedGlobs = includedGlobs;
	}

	public void addIncludedGlob(String name){
		if (!includedGlobs.contains(name))
			this.includedGlobs.add(name);
	}
	
	public void addExcludedGlob(String name){
		if (!excludedGlobs.contains(name))
			this.excludedGlobs.add(name);
	}
	
	
	 public boolean equals(Object obj) {
	        if (this == obj)
	            return true;
	        if (obj == null || getClass() != obj.getClass())
	            return false;

	        AugeasModuleConfig that = (AugeasModuleConfig) obj;

	        if (!this.moduletName.equals(that.getModuletName()))
	    		return false;
	        
	        if (!this.lensPath.equals(that.getLensPath()))
	    		return false;

	        return true;
	    }

}
