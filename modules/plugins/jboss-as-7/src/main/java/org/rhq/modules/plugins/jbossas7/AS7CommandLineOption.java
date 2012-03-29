package org.rhq.modules.plugins.jbossas7;

/**
 * The name(s) associated with an AS7 server command line option.
 *
 * @author Ian Springer
 */
public class AS7CommandLineOption {

    private String shortName;
    private String longName;

    public AS7CommandLineOption(char shortName, String longName) {
        this(new String(new char[] {shortName}), longName);
    }
    
    public AS7CommandLineOption(String shortName, String longName) {
        if ((shortName == null) && (longName == null)) {
            throw new IllegalArgumentException("ShortName and longName cannot both be null.");
        }

        this.shortName = shortName;
        this.longName = longName;
    }

    
    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    @Override
    public String toString() {
        return "AS7CommandLineOption{" +
                "shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                '}';
    }

}
