package org.rhq.augeas.util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LensFilter implements FileFilter{   
    private Pattern pattern;
    
	public LensFilter(String name){
	   pattern = Pattern.compile(name+".*"+LensHelper.TEMP_FILE_SUFFIX);
	}
	
	public boolean accept(File pathname) {
		String name = pathname.getName();
		Matcher match = pattern.matcher(name);
		if (match.matches())
		   return true;
		
		return false;
	}
}
