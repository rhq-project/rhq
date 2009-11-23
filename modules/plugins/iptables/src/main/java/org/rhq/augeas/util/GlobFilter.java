package org.rhq.augeas.util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * A filter implementing a glob pattern match.
 * 
 * @author Lukas Krejci
 */
public class GlobFilter implements FileFilter {

    private String globPattern;
    private Pattern regexPattern;
    
    public static final char[] WILDCARD_CHARS;
    
    static {
        if (File.separatorChar == '\\') {
            WILDCARD_CHARS = new char[] { '*', '?' };
        } else {
            WILDCARD_CHARS = new char[] { '*', '?', '[', ']'};
        }
    }
    
    public GlobFilter(String globPattern) {
        if (globPattern == null) {
            throw new IllegalArgumentException("The glob pattern cannot be null.");
        }
        
        this.globPattern = globPattern;
        this.regexPattern = convert(globPattern);
    }
    
    
    public String getGlobPattern() {
        return globPattern;
    }

    /* (non-Javadoc)
     * @see java.io.FileFilter#accept(java.io.File)
     */
    public boolean accept(File pathname) {
        return regexPattern.matcher(pathname.getAbsolutePath()).matches();
    }

    private static Pattern convert(String globPattern) {
        StringBuilder regexPattern = new StringBuilder("^");
        int i = 0;
        while (i < globPattern.length()) {
            switch (globPattern.charAt(i)) {
            case '\\' :
                if (File.separatorChar == '\\') {
                    //we're on windows, \ is a separator
                    regexPattern.append("\\\\");
                } else {
                    //anywhere else, \ is a escape sequence
                    if (i == globPattern.length() - 1) {
                        throw new IllegalArgumentException("Illegal glob pattern: " + globPattern);
                    }
                    regexPattern.append("\\").append(globPattern.charAt(i + 1));
                    i += 1; //just skip the next character
                }
                break;
            case '*':
                regexPattern.append(".*");
                break;
            case '?':
                regexPattern.append(".?");
                break;
            case '.':
                regexPattern.append("\\.");
                break;
            case '/':
                regexPattern.append("\\/");
                break;
            default:
                regexPattern.append(globPattern.charAt(i));
                break;
            }
            i++;
        }
        
        regexPattern.append("$");
        
        return Pattern.compile(regexPattern.toString());
    }
}
