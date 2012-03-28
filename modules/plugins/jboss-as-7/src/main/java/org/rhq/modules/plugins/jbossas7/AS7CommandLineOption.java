package org.rhq.modules.plugins.jbossas7;

/**
 * The name(s) associated with an AS7 server command line option.
 *
 * @author Ian Springer
 */
public class AS7CommandLineOption {

    private Character shortName;
    private String longName;

    public AS7CommandLineOption(Character shortName, String longName) {
        if ((shortName == null) && (longName == null)) {
            throw new IllegalArgumentException("ShortName and longName cannot both be null.");
        }

        this.shortName = shortName;
        this.longName = longName;
    }

    public Character getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

}
