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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.istack.Nullable;

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
     * When parsing command line options, determines the valid format.
     * <p>POSIX for short command line options is equivalent to SPACE_ONLY.</P>
     * <p>POSIX for long command line options is equivalent to EQUALS_ONLY.</P>     * 
     * 
     * @see JavaCommandLine#getClassOption(JavaCommandLineOption)
     */
    public enum OptionFormat {
        POSIX, SPACE_OR_EQUALS
    };

    private List<String> arguments;
    private File javaExecutable;
    private List<String> classPath;
    private Map<String, String> systemProperties;
    private List<String> javaOptions;
    private String mainClassName;
    private File executableJarFile;
    private List<String> classArguments;
    private boolean includeSystemPropertiesFromClassArguments;
    private OptionFormat shortClassOptionFormat;
    private OptionFormat longClassOptionFormat;

    /**
     * Same as <code>JavaCommandLine(args, false, OptionFormat.POSIX, OptionFormat.POSIX)</code>
     */
    public JavaCommandLine(String... args) {
        this(args, false, OptionFormat.POSIX, OptionFormat.POSIX);
    }

    /**
     * Same as <code>JavaCommandLine(args, includeSystemPropertiesFromClassArguments, OptionFormat.POSIX, OptionFormat.POSIX)</code>
     */
    public JavaCommandLine(boolean includeSystemPropertiesFromClassArguments, String... args) {
        this(args, includeSystemPropertiesFromClassArguments, OptionFormat.POSIX, OptionFormat.POSIX);
    }

    /**
     * Same as <code>JavaCommandLine(args, includeSystemPropertiesFromClassArguments, OptionFormat.POSIX, OptionFormat.POSIX)</code>
     */
    public JavaCommandLine(String[] args, boolean includeSystemPropertiesFromClassArguments) {
        this(args, includeSystemPropertiesFromClassArguments, OptionFormat.POSIX, OptionFormat.POSIX);
    }

    public JavaCommandLine(String[] args, boolean includeSystemPropertiesFromClassArguments,
        OptionFormat shortClassOptionFormat, OptionFormat longClassOptionFormat) {
        if (args == null) {
            throw new IllegalArgumentException("'commandLine' parameter is null.");
        }

        if (args.length == 0) {
            throw new IllegalArgumentException("'commandLine' parameter is an empty array.");
        }

        this.includeSystemPropertiesFromClassArguments = includeSystemPropertiesFromClassArguments;
        this.shortClassOptionFormat = (shortClassOptionFormat == null) ? OptionFormat.POSIX : shortClassOptionFormat;
        this.longClassOptionFormat = (longClassOptionFormat == null) ? OptionFormat.POSIX : longClassOptionFormat;

        parseCommandLine(args);
    }

    private void parseCommandLine(String[] args) {
        this.arguments = Arrays.asList(args);
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
     * @param option
     * @return null if the option is not on the command line, "" if it is on the command line and
     * either has no value or expects no value, otherwise the non-empty value.
     */
    @Nullable
    public String getClassOption(JavaCommandLineOption option) {
        String shortOption = null;
        String shortOptionEquals = null;
        if (option.getShortName() != null) {
            shortOption = "-" + option.getShortName();
            shortOptionEquals = shortOption + "=";
        }

        String longOption = null;
        String longOptionEquals = null;
        if (option.getLongName() != null) {
            longOption = "--" + option.getLongName();
            longOptionEquals = longOption + "=";
        }

        for (int i = 0, classArgsLength = getClassArguments().size(); i < classArgsLength; i++) {
            String classArg = getClassArguments().get(i);

            // If we hit a "stop processing options" option then don't look further, the rest of the
            // options are actually deferred to a subsequent command.
            // TODO: This behavior may need to be configurable.
            if ("--".equals(classArg)) {
                break;
            }

            if (option.getShortName() != null && classArg.startsWith(shortOption)) {
                if (!option.isExpectsValue()) {
                    return "";

                } else {
                    switch (shortClassOptionFormat) {
                    case POSIX:
                        return (i != (classArgsLength - 1)) ? getClassArguments().get(i + 1) : "";

                    case SPACE_OR_EQUALS:
                    default:
                        int len = shortOptionEquals.length();
                        if (classArg.startsWith(shortOptionEquals)) {
                            return (len < classArg.length()) ? (classArg.substring(len).trim()) : "";
                        }
                        break;
                    }
                }

            } else if (option.getLongName() != null && classArg.startsWith(longOption)) {
                if (!option.isExpectsValue()) {
                    return "";

                } else {
                    switch (longClassOptionFormat) {
                    case POSIX:
                        int len = longOptionEquals.length();
                        if (classArg.startsWith(longOptionEquals)) {
                            return (len < classArg.length()) ? (classArg.substring(len).trim()) : "";
                        }
                        break;

                    case SPACE_OR_EQUALS:
                    default:
                        return (i != (classArgsLength - 1)) ? getClassArguments().get(i + 1) : "";
                    }
                }
            }
        }

        // If we reached here, the option wasn't on the command line.
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(this.arguments.get(0));
        for (int i = 1, argumentsSize = this.arguments.size(); i < argumentsSize; i++) {
            buffer.append(' ').append(this.arguments.get(i));
        }
        return buffer.toString();
    }

}
