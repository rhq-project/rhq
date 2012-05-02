/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.istack.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Parses a java command line and provides easy access to its parts.
 * <p/>
 * A Java command line looks like this:
 * <pre><code>
 *   Usage: java [-options] class [args...]
 *         (to execute a class)
 *      or  java [-options] -jar jarfile [args...]
 *         (to execute a jar file)
 * </code></pre>
 *
 * @author Ian Springer
 */
public class JavaCommandLine {
    /** 
     * When parsing command line options, specifies the valid option value delimiter(s).
     *
     * @see JavaCommandLine#getClassOption(CommandLineOption)
     */
    public enum OptionValueDelimiter {
        /**
         * The option value is separated from the option name by whitespace (hence it is actually a separate command
         * line argument, e.g. "-f FILE" or "--file FILE".
         */
        WHITESPACE,

        /**
         * The option value is separated from the option name by an equals sign, e.g. "-f=FILE" or "--file=FILE".
         */
        EQUALS_SIGN
    }

    private static final String SHORT_OPTION_PREFIX = "-";
    private static final String LONG_OPTION_PREFIX = "--";

    private final Log log = LogFactory.getLog(JavaCommandLine.class);

    private List<String> arguments;
    private File javaExecutable;
    private List<String> classPath;
    private Map<String, String> systemProperties;
    private List<String> javaOptions;
    private String mainClassName;
    private File executableJarFile;
    private List<String> classArguments;
    private boolean includeSystemPropertiesFromClassArguments;
    private Set<OptionValueDelimiter> shortClassOptionValueDelims;
    private Set<OptionValueDelimiter> longClassOptionValueDelims;
    private Map<String, String> shortClassOptionNameToOptionValueMap;
    private Map<String, String> longClassOptionNameToOptionValueMap;

    /**
     * Same as <code>JavaCommandLine(args, false, OptionFormat.POSIX, OptionFormat.POSIX)</code>
     */
    public JavaCommandLine(String... args) {
        this(args, false);
    }

    /**
     * Same as <code>JavaCommandLine(args, includeSystemPropertiesFromClassArguments, OptionFormat.POSIX, OptionFormat.POSIX)</code>
     */
    public JavaCommandLine(String[] args, boolean includeSystemPropertiesFromClassArguments) {
        this(args, includeSystemPropertiesFromClassArguments, null, null);
    }

    public JavaCommandLine(String[] args, boolean includeSystemPropertiesFromClassArguments,
        Set<OptionValueDelimiter> shortClassOptionValueDelims, Set<OptionValueDelimiter> longClassOptionValueDelims) {
        if (args == null) {
            throw new IllegalArgumentException("'args' parameter is null.");
        }

        if (args.length == 0) {
            throw new IllegalArgumentException("'args' parameter is an empty array.");
        }

        this.includeSystemPropertiesFromClassArguments = includeSystemPropertiesFromClassArguments;

        // Default to GNU-style short options (e.g. "-f FILE").
        this.shortClassOptionValueDelims = (shortClassOptionValueDelims != null) ? shortClassOptionValueDelims :
                EnumSet.of(OptionValueDelimiter.WHITESPACE);
        if (this.shortClassOptionValueDelims.isEmpty()) {
            throw new IllegalArgumentException("'shortClassOptionValueDelims' parameter is an empty set.");
        }

        // Default to GNU-style long options (e.g. "--file=FILE").
        this.longClassOptionValueDelims = (longClassOptionValueDelims != null) ? longClassOptionValueDelims :
                EnumSet.of(OptionValueDelimiter.EQUALS_SIGN);
        if (this.longClassOptionValueDelims.isEmpty()) {
            throw new IllegalArgumentException("'longClassOptionValueDelims' parameter is an empty set.");
        }

        // Wrap as list and store as field for use by getArguments() and toString().
        this.arguments = Arrays.asList(args);

        parseCommandLine(args);
    }

    private void parseCommandLine(String[] args) {
        if (log.isDebugEnabled()) {
            log.debug("Parsing " + this + "...");
        }

        this.javaExecutable = new File(args[0]);
        this.classPath = new ArrayList<String>();
        this.systemProperties = new LinkedHashMap<String, String>();
        this.javaOptions = new ArrayList<String>();
        this.classArguments = new ArrayList<String>();

        boolean nextArgIsClassPath = false;
        boolean nextArgIsJarFile = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (nextArgIsClassPath) {
                this.classPath.addAll(Arrays.asList(arg.split(File.pathSeparator)));
                nextArgIsClassPath = false;
            } else if (nextArgIsJarFile) {
                this.executableJarFile = new File(arg);
                parseClassArguments(args, i + 1);
                break;
            } else if (arg.charAt(0) != '-') {
                this.mainClassName = arg;
                parseClassArguments(args, i + 1);
                break;
            } else if (arg.equals("-cp") || arg.equals("-classpath")) {
                if ((i + 1) == args.length) {
                    throw new IllegalArgumentException(arg + " option has no argument.");
                }
                nextArgIsClassPath = true;
            } else if (arg.equals("-jar")) {
                if ((i + 1) == args.length) {
                    throw new IllegalArgumentException(arg + " option has no argument.");
                }
                nextArgIsJarFile = true;
            } else {
                if (arg.matches("-D.+")) {
                    parseSystemPropertyArgument(arg);
                }
                this.javaOptions.add(arg);
            }
        }

        this.classPath = Collections.unmodifiableList(this.classPath);
        this.javaOptions = Collections.unmodifiableList(this.javaOptions);
        this.classArguments = Collections.unmodifiableList(this.classArguments);
        this.systemProperties = Collections.unmodifiableMap(this.systemProperties);

        parseClassOptions();
    }

    private void parseClassArguments(String[] args, int beginIndex) {
        for (int i = beginIndex; i < args.length; i++) {
            String classArg = args[i];
            if (this.includeSystemPropertiesFromClassArguments && classArg.matches("-D.+")) {
                parseSystemPropertyArgument(classArg);
            }
            this.classArguments.add(classArg);
        }
    }

    private void parseClassOptions() {
        this.shortClassOptionNameToOptionValueMap = new HashMap<String, String>();
        this.longClassOptionNameToOptionValueMap = new HashMap<String, String>();

        if (!this.classArguments.isEmpty()) {
            for (int i = 0, classArgumentsSize = this.classArguments.size(); i < classArgumentsSize; i++) {
                String classArg = this.classArguments.get(i);

                String optionString; // the option with the prefix stripped off
                Set<OptionValueDelimiter> optionValueDelims;
                Map<String, String> optionValueMap;
                // We must check if the arg starts with long prefix before short prefix, since the former is a subset of
                // the latter.
                if (classArg.startsWith(LONG_OPTION_PREFIX)) {
                    // long opt
                    if (classArg.length() == LONG_OPTION_PREFIX.length()) {
                        // arg is "--", which means to stop processing options
                        // TODO: make this configurable?
                        break;
                    }
                    optionString = classArg.substring(LONG_OPTION_PREFIX.length());
                    optionValueDelims = this.longClassOptionValueDelims;
                    optionValueMap = this.longClassOptionNameToOptionValueMap;
                } else if (classArg.startsWith(SHORT_OPTION_PREFIX)) {
                    // short opt
                    optionString = classArg.substring(SHORT_OPTION_PREFIX.length());
                    optionValueDelims = this.shortClassOptionValueDelims;
                    optionValueMap = this.shortClassOptionNameToOptionValueMap;
                } else {
                    // not an option
                    continue;
                }

                String optionName = null;
                String optionValue = null;
                if (optionValueDelims.contains(OptionValueDelimiter.WHITESPACE)) {
                    int equalsIndex = optionString.indexOf('=');
                    if (equalsIndex >= 1) {
                        if (optionValueDelims.contains(OptionValueDelimiter.EQUALS_SIGN)) {
                            optionName = optionString.substring(0, equalsIndex);
                            optionValue = (equalsIndex == (optionString.length() - 1)) ?
                                    "" : optionString.substring(equalsIndex + 1);
                        } else if (optionString.charAt(0) != 'D') {
                            // We don't log this warning for sysprops.
                            log.warn("Option [" + classArg
                                    + "] contains an equals sign, which is not a valid class option value delimiter for this command line.");
                        }
                    } else {
                        optionName = optionString;
                        if (((i + 1) < classArgumentsSize) && !this.classArguments.get(i + 1).startsWith("-")) {
                            // there is a next argument and it's not an option - assume it's an argument to this option
                            // and advance our loop index.
                            optionValue = this.classArguments.get(++i);
                        } else {
                            // the option has no argument - store an empty string as its value to indicate the option
                            // was present on the command line.
                            optionValue = "";
                        }
                    }
                } else if (optionValueDelims.contains(OptionValueDelimiter.EQUALS_SIGN)) {
                    int equalsIndex = optionString.indexOf('=');
                    if (equalsIndex == -1) {
                        // the option has no argument - store an empty string as its value to indicate the option
                        // was present on the command line.
                        optionName = optionString;
                        optionValue = "";
                    } else if (equalsIndex >= 1) {
                        optionName = optionString.substring(0, equalsIndex);
                        optionValue = (equalsIndex == (optionString.length() - 1)) ?
                                "" : optionString.substring(equalsIndex + 1);
                    } else {
                        log.warn("Ignoring malformed option [" + classArg + "] on command line [" + this + "]...");
                    }
                }

                if (optionName != null) {
                    optionValueMap.put(optionName, optionValue);
                }
            }
        }
    }

    private void parseSystemPropertyArgument(String arg) {
        String argValue = arg.substring(2);
        int equalsSignIndex = argValue.indexOf('=');
        String name;
        String value;
        if (equalsSignIndex >= 0) {
            name = argValue.substring(0, equalsSignIndex);
            value = (equalsSignIndex == (argValue.length() - 1)) ? "" : argValue.substring(equalsSignIndex + 1);
        } else {
            name = argValue;
            value = "";
        }
        this.systemProperties.put(name, value);
    }

    @NotNull
    public List<String> getArguments() {
        return arguments;
    }

    @NotNull
    public File getJavaExecutable() {
        return javaExecutable;
    }

    @NotNull
    public List<String> getClassPath() {
        return classPath;
    }

    @NotNull
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    @NotNull
    public List<String> getJavaOptions() {
        return javaOptions;
    }

    @Nullable
    public String getMainClassName() {
        return mainClassName;
    }

    @Nullable
    public File getExecutableJarFile() {
        return executableJarFile;
    }

    @NotNull
    public List<String> getClassArguments() {
        return classArguments;
    }

    /**
     * @param option the class option to look for
     *
     * @return null if the class option is not on the command line, "" if it is on the command line and
     *         either has no value or expects no value, and otherwise the non-empty value.
     */
    @Nullable
    public String getClassOption(CommandLineOption option) {
        String optionValue = null;
        // Note, we never store null values in either of the option value maps.

        if ((option.getLongName() != null) && this.longClassOptionNameToOptionValueMap.containsKey(option.getLongName())) {
            optionValue = this.longClassOptionNameToOptionValueMap.get(option.getLongName());
            if (!optionValue.isEmpty() && !option.isExpectsValue()) {
                // TODO: Store the delims used for each of the options in another set of maps, so we can handle
                //       things differently here depending on what delim was used.
                if (this.longClassOptionValueDelims.equals(EnumSet.of(OptionValueDelimiter.EQUALS_SIGN))) {
                    log.warn("Class option [" + option + "] does not expect a value, but a value was specified on command line ["
                                            + this + "].");
                } else if (this.longClassOptionValueDelims.equals(EnumSet.of(OptionValueDelimiter.WHITESPACE))) {
                    optionValue = "";
                }
            }
        }

        if ((optionValue == null) && (option.getShortName() != null) && this.shortClassOptionNameToOptionValueMap.containsKey(option.getShortName())) {
            optionValue = this.shortClassOptionNameToOptionValueMap.get(option.getShortName());
            if (!optionValue.isEmpty() && !option.isExpectsValue()) {
                // TODO: Store the delims used for each of the options in another set of maps, so we can handle
                //       things differently here depending on what delim was used.
                if (this.shortClassOptionValueDelims.equals(EnumSet.of(OptionValueDelimiter.EQUALS_SIGN))) {
                    log.warn("Class option [" + option + "] does not expect a value, but a value was specified on command line ["
                                            + this + "].");
                } else if (this.shortClassOptionValueDelims.equals(EnumSet.of(OptionValueDelimiter.WHITESPACE))) {
                    optionValue = "";
                }
            }
        }

        if (optionValue != null && optionValue.isEmpty() && option.isExpectsValue()) {
            log.warn("Class option [" + option + "] expects a value, but no value was specified on command line ["
                    + this + "].");
        }

        return optionValue;
    }

    public boolean isClassOptionPresent(CommandLineOption option) {
        String optionValue = getClassOption(option);
        return (optionValue != null);
    }

    @Override
    public String toString() {
        return "JavaCommandLine[arguments=" + this.arguments //
            + ", includeSystemPropertiesFromClassArguments=" + this.includeSystemPropertiesFromClassArguments //
            + ", shortClassOptionFormat=" + this.shortClassOptionValueDelims //
            + ", longClassOptionFormat=" + this.longClassOptionValueDelims + "]";
    }

}
