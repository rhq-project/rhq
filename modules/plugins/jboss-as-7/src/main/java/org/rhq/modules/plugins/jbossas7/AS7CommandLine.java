package org.rhq.modules.plugins.jbossas7;

import org.jetbrains.annotations.Nullable;

import org.rhq.core.pluginapi.util.JavaCommandLine;

/**
 * Parses a JBoss AS7 command line and provides easy access to its parts.
 *
 * @author Ian Springer
 */
public class AS7CommandLine extends JavaCommandLine {

    public AS7CommandLine(String[] args) {
        super(args, true);
    }

    @Nullable
    public String getClassOption(AS7CommandLineOption option) {
        String shortOptionPrefix;
        String shortOption;
        if (option.getShortName() != null) {
            shortOption = "-" + option.getShortName();
            shortOptionPrefix = shortOption + "=";
        } else {
            shortOption = null;
            shortOptionPrefix = null;
        }
        String longOptionPrefix;
        if (option.getLongName() != null) {
            longOptionPrefix = "--" + option.getLongName() + "=";
        } else {
            longOptionPrefix = null;
        }

        for (int i = 0, classArgsLength = getClassArguments().size(); i < classArgsLength; i++) {
            String classArg = getClassArguments().get(i);
            if (option.getShortName() != null) {
                if (classArg.startsWith(shortOptionPrefix)) {
                    return (shortOptionPrefix.length() < classArg.length()) ? classArg.substring(shortOptionPrefix.length()) : "";
                } else if (classArg.equals(shortOption)) {
                    return (i != (classArgsLength - 1)) ? getClassArguments().get(i + 1) : "";
                }
            }
            if (option.getLongName() != null) {
                if (classArg.startsWith(longOptionPrefix)) {
                    return (longOptionPrefix.length() < classArg.length()) ? classArg.substring(longOptionPrefix.length()) : "";
                }
            }
        }
        // If we reached here, the option wasn't on the command line.
        return null;
    }

}
