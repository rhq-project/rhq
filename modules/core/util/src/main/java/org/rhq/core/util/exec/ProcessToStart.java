/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.util.exec;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.util.UtilI18NResourceKeys;

/**
 * Data describing a process to start.
 *
 * @author John Mazzitelli
 */
public class ProcessToStart {
    private static final String PARAM_PROGRAM_TITLE = "programTitle";
    private static final String PARAM_PROGRAM_EXE = "programExecutable";
    private static final String PARAM_PROGRAM_DIR = "programDirectory";
    private static final String PARAM_ARGS = "arguments";
    private static final String PARAM_ENV = "environmentVariables";
    private static final String PARAM_WORKING_DIR = "workingDirectory";
    private static final String PARAM_OUTPUT_DIR = "outputDirectory";
    private static final String PARAM_OUTPUT_FILE = "outputFile";
    private static final String PARAM_OUTPUT_STREAM = "outputStream";
    private static final String PARAM_INPUT_DIR = "inputDirectory";
    private static final String PARAM_INPUT_FILE = "inputFile";
    private static final String PARAM_CAPTURE_OUTPUT = "captureOutput";
    private static final String PARAM_BACKUP_OUTPUT_FILE = "backupOutputFile";
    private static final String PARAM_WAIT_FOR_EXIT = "waitForExit";
    private static final String PARAM_KILL_ON_TIMEOUT = "killOnTimeout";
    private static final String PARAM_CHECK_EXECUTABLE_EXISTS = "checkExecutableExists";

    private Map<String, Object> map = new HashMap<String, Object>();

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns the title for this program. This is a simple name to help identify this start command configuration or to
     * just help identify the program being launched.
     *
     * <p>This is useful when, for example, a Java JVM needs to be started and you want to set the title to describe the
     * main class being launched (since all Java executables that are launched typically have the same executable name,
     * that of "java.exe").</p>
     *
     * <p>If the title was never {@link #setProgramTitle(String) set}, this will return {@link #getProgramExecutable()}
     * .</p>
     *
     * @return the title of the program
     */
    public String getProgramTitle() {
        String title = (String) map.get(PARAM_PROGRAM_TITLE);

        if (title == null) {
            title = getProgramExecutable();
        }

        return title;
    }

    /**
     * Sets the title for this program.
     *
     * @param value the title of the program (may be <code>null</code>)
     *
     * @see   #getProgramTitle()
     */
    public void setProgramTitle(String value) {
        // the title is optional but non-nullable; remove the param value if null
        if (value != null) {
            map.put(PARAM_PROGRAM_TITLE, value);
        } else {
            map.remove(PARAM_PROGRAM_TITLE);
        }
    }

    /**
     * Returns the name of the program to execute. This is usually just the executable file name without any path
     * information - for that information, see {@link #getProgramDirectory()}. If, however,
     * {@link #getProgramDirectory()} returns <code>null</code>, the full path to the executable may be returned by this
     * method.
     *
     * @return program executable name
     */
    public String getProgramExecutable() {
        return (String) map.get(PARAM_PROGRAM_EXE);
    }

    /**
     * Sets the name of the program to execute.
     *
     * @param value program executable name
     *
     * @see   #getProgramExecutable()
     */
    public void setProgramExecutable(String value) {
        map.put(PARAM_PROGRAM_EXE, value);
    }

    /**
     * Returns the full path to the {@link #getProgramExecutable() program executable}. This is just the directory where
     * the executable file is located without the name of the executable itself - for that information, see
     * {@link #getProgramExecutable()}. If this returns <code>null</code>, then the full path may be included in the
     * {@link #getProgramExecutable() executable name itself}.
     *
     * @return program executable directory location
     */
    public String getProgramDirectory() {
        return (String) map.get(PARAM_PROGRAM_DIR);
    }

    /**
     * Sets the full path to the program executable.
     *
     * @param value program executable directory location
     *
     * @see   #getProgramExecutable()
     */
    public void setProgramDirectory(String value) {
        map.put(PARAM_PROGRAM_DIR, value);
    }

    /**
     * Returns an array of strings that are the argument values passed on the command line to the program executable. If
     * <code>null</code> or empty, no arguments will be passed to the program.
     *
     * @return array of program arguments
     */
    public String[] getArguments() {
        return (String[]) map.get(PARAM_ARGS);
    }

    /**
     * Sets the argument values that are to be passed on the command line to the program executable. If <code>
     * null</code> or empty, no arguments will be passed to the program.
     *
     * @param value array of program arguments (may be <code>null</code> or empty)
     */
    public void setArguments(String[] value) {
        // args are optional but non-nullable; remove the param value if null
        if (value != null) {
            map.put(PARAM_ARGS, value);
        } else {
            map.remove(PARAM_ARGS);
        }
    }

    /**
     * Returns environment variable name/value pairs that define the environment to be passed to the started process. A
     * <code>null</code> will allow the subprocess to inherit the parent process environment. Each string must be in the
     * format: <code>name=value</code>.
     *
     * @return environment variables (may be <code>null</code> or empty)
     */
    public String[] getEnvironment() {
        return (String[]) map.get(PARAM_ENV);
    }

    /**
     * Convenience method that not only returns the environment variables as a String array, but the environment
     * variables are also populated in the given <code>Properties</code> map so the caller can more easily look up
     * environment variables by name. Note that any properties in the <code>properties</code> object will be cleared out
     * - only the environment variables in this object will populate the <code>properties</code> object once this method
     * returns.
     *
     * <p>Note that the returned array should still be examined - at the least to see if it is <code>null</code> - even
     * if the caller only wants to use the <code>Properties</code> object for retrieval of the environment variables.
     * This is because a <code>null</code> return value has special semantics, as opposed to a non- <code>null</code>
     * but empty array. See {@link #getEnvironment()} for more.</p>
     *
     * @param  properties a Properties object where the environment variables can be stored (must not be <code>
     *                    null</code>)
     *
     * @return environment variables (may be <code>null</code> or empty)
     *
     * @throws IllegalArgumentException if an environment variable string doesn't conform to the format: <code>
     *                                  name=value</code> or <code>properties</code> is <code>null</code>
     */
    public String[] getEnvironment(Properties properties) throws IllegalArgumentException {
        if (properties != null) {
            properties.clear();
        } else {
            throw new IllegalArgumentException("properties=null");
        }

        String[] envVarArray = getEnvironment();

        if (envVarArray != null) {
            for (int i = 0; i < envVarArray.length; i++) {
                String envVarString = envVarArray[i];
                int equals = envVarString.indexOf('=');
                if (equals >= 0) {
                    properties.setProperty(envVarString.substring(0, equals), envVarString.substring(equals + 1));
                } else {
                    throw new IllegalArgumentException(UtilI18NResourceKeys.MSG.getMsg(
                        UtilI18NResourceKeys.START_COMMAND_ENV_VAR_BAD_FORMAT, envVarString));
                }
            }
        }

        return envVarArray;
    }

    /**
     * Sets the environment variable name/value pairs that define the environment to be passed to the started process. A
     * <code>null</code> will allow the subprocess to inherit the parent process environment. Each string must be in the
     * format: <code>name=value</code>.
     *
     * @param value environment variables (may be <code>null</code> or empty)
     */
    public void setEnvironment(String[] value) {
        map.put(PARAM_ENV, value);
    }

    /**
     * Convienence method that takes a <code>Properties</code> object containing the environment variables, as opposed
     * to an array of strings (see {@link #setEnvironment(String[])}. Sets the environment variable name/value pairs
     * that define the environment to be passed to the started process. A <code>null</code> will allow the subprocess to
     * inherit the parent process environment.
     *
     * @param value property name/values stored in a <i>Properties</i> object
     *
     * @see   #setEnvironment(String[])
     */
    public void setEnvironment(Properties value) {
        String[] env = null;

        if (value != null) {
            env = new String[value.size()];

            Enumeration propNames = value.propertyNames();

            for (int i = 0; propNames.hasMoreElements(); i++) {
                String propName = (String) propNames.nextElement();
                String propValue = value.getProperty(propName);
                env[i] = propName + "=" + propValue;
            }
        }

        setEnvironment(env);

        return;
    }

    /**
     * Returns the working directory of the new process (known also as the current directory or the startup directory).
     * A <code>null</code> allows the subprocess to inherit the current working directory of the parent process.
     *
     * @return the working directory path (may be <code>null</code>)
     */
    public String getWorkingDirectory() {
        return (String) map.get(PARAM_WORKING_DIR);
    }

    /**
     * Sets the working directory of the new process
     *
     * @param value the working directory path (may be <code>null</code>)
     *
     * @see   #getWorkingDirectory()
     */
    public void setWorkingDirectory(String value) {
        map.put(PARAM_WORKING_DIR, value);
    }

    /**
     * Returns the directory where the program's output log file will be written. If <code>null</code>, a directory will
     * be assigned (typically the <code>java.io.tmpdir</code> directory).
     *
     * <p>If {@link #getOutputStream()} is not <code>null</code>, it overrides this (that is, the stream will get the
     * output, not the output file).</p>
     *
     * <p>Regardless of this return value, output will not be captured unless
     * {@link #setCaptureOutput(Boolean) explicitly told to do so}.</p>
     *
     * @return the directory where the output log will be written to (may be <code>null</code>)
     */
    public String getOutputDirectory() {
        return (String) map.get(PARAM_OUTPUT_DIR);
    }

    /**
     * Sets the directory where the program's output log file will be written.
     *
     * @param value the directory where the output log will be written to (may be <code>null</code>)
     *
     * @see   #getOutputDirectory()
     */
    public void setOutputDirectory(String value) {
        // output dir is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_OUTPUT_DIR, value);
        } else {
            map.remove(PARAM_OUTPUT_DIR);
        }
    }

    /**
     * The file (to be placed in the {@link #getOutputDirectory() output directory}) where the program's output will be
     * written. This should just be the filename; its full path will be specified by the
     * {@link #getOutputDirectory() output directory}.
     *
     * <p>It is in this file where you can view the program's stdout/stderr output stream data.</p>
     *
     * <p>If <code>null</code>, an auto-generated filename will be used.</p>
     *
     * <p>If {@link #getOutputStream()} is not <code>null</code>, it overrides this (that is, the stream will get the
     * output, not the output file).</p>
     *
     * <p>Regardless of this return value, output will not be captured unless
     * {@link #setCaptureOutput(Boolean) explicitly told to do so}.</p>
     *
     * @return the program's output log file (may be <code>null</code>)
     */
    public String getOutputFile() {
        return (String) map.get(PARAM_OUTPUT_FILE);
    }

    /**
     * Sets the file (to be placed in the {@link #getOutputDirectory() output directory}) where the program's output
     * will be written.
     *
     * @param value the program's output log file (may be <code>null</code>)
     *
     * @see   #getOutputFile()
     */
    public void setOutputFile(String value) {
        // output file is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_OUTPUT_FILE, value);
        } else {
            map.remove(PARAM_OUTPUT_FILE);
        }
    }

    /**
     * The output stream where the program's output will be written. If this is <code>null</code>, then
     * {@link #getOutputDirectory()}/{@link #getOutputFile()} will be examined if the output should be written to a
     * file.
     *
     * <p>Regardless of this return value, output will not be captured unless
     * {@link #setCaptureOutput(Boolean) explicitly told to do so}.</p>
     *
     * @return the program's output stream (may be <code>null</code>)
     */
    public OutputStream getOutputStream() {
        return (OutputStream) map.get(PARAM_OUTPUT_STREAM);
    }

    /**
     * The output stream where the program's output will be written.
     *
     * @param value the program's output stream (may be <code>null</code>)
     *
     * @see   #getOutputStream()
     */
    public void setOutputStream(OutputStream value) {
        // output stream is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_OUTPUT_STREAM, value);
        } else {
            map.remove(PARAM_OUTPUT_STREAM);
        }
    }

    /**
     * Returns the directory where the {@link #getInputFile() input file} is located.
     *
     * <p>If this is specified, the {@link #setInputFile(String) input file} must also be specified.</p>
     *
     * @return directory where the input file is located (may be <code>null</code>)
     */
    public String getInputDirectory() {
        return (String) map.get(PARAM_INPUT_DIR);
    }

    /**
     * Sets the directory where the {@link #getInputFile() input file} is located.
     *
     * @param value directory where the input file is located (may be <code>null</code>)
     *
     * @see   #getInputDirectory()
     */
    public void setInputDirectory(String value) {
        // input directory is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_INPUT_DIR, value);
        } else {
            map.remove(PARAM_INPUT_DIR);
        }
    }

    /**
     * Returns the name of the file that contains data to be input to the program. The data found in this file will be
     * passed into the started program via the stdin input stream.
     *
     * <p>If this is specified, the {@link #setInputDirectory(String) input directory} must also be specified.</p>
     *
     * @return name of the input file (may be <code>null</code>)
     */
    public String getInputFile() {
        return (String) map.get(PARAM_INPUT_FILE);
    }

    /**
     * Sets the name of the file that contains data to be input to the program.
     *
     * @param value name of the input file (may be <code>null</code>)
     *
     * @see   #getInputFile()
     */
    public void setInputFile(String value) {
        // input file is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_INPUT_FILE, value);
        } else {
            map.remove(PARAM_INPUT_FILE);
        }
    }

    /**
     * If 0 or less, the process executor will not wait for the process to exit before returning control. Otherwise,
     * this is the number of milliseconds the process executor will wait for the process to exit. If the time expires,
     * control will return but the process will continue to run (an attempt to kill the process will not be made).
     *
     * @return wait time (will never be <code>null</code>)
     */
    public Long getWaitForExit() {
        Long waitTimeout = (Long) map.get(PARAM_WAIT_FOR_EXIT);

        if (waitTimeout == null) {
            waitTimeout = Long.valueOf(0L);
        }

        return waitTimeout;
    }

    /**
     * If <code>null</code> or is 0 or less, the process executor will not wait for the process to exit before returning
     * control. Otherwise, this is the number of milliseconds the process executor will wait for the process to exit. If
     * the time expires, control will return but the process will continue to run (an attempt to kill the process will
     * not be made).
     *
     * @param value time
     */
    public void setWaitForExit(Long value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_WAIT_FOR_EXIT, value);
        } else {
            map.remove(PARAM_WAIT_FOR_EXIT);
        }
    }

    /**
     * If <code>true</code>, the started process' output will be captured and written to the output
     * {@link #getOutputFile() file} or {@link #getOutputStream() stream}. If <code>false</code>, no output file is
     * created and the process' output is simply consumed and ignored even if a non-<code>null</code> output file or
     * stream has been set.
     *
     * @return capture output flag (default is <code>false</code>, this will never be <code>null</code>)
     */
    public Boolean isCaptureOutput() {
        Boolean flag = (Boolean) map.get(PARAM_CAPTURE_OUTPUT);

        if (flag == null) {
            flag = Boolean.FALSE;
        }

        return flag;
    }

    /**
     * Sets the flag to indicate if the process' output should be captured in the output file.
     *
     * @param value capture output flag
     *
     * @see   #isCaptureOutput()
     */
    public void setCaptureOutput(Boolean value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_CAPTURE_OUTPUT, value);
        } else {
            map.remove(PARAM_CAPTURE_OUTPUT);
        }
    }

    /**
     * If <code>true</code>, any previously existing output file will be backed up by renaming it with a date/timestamp.
     * If <code>false</code>, any previously existing output file will be overwritten.
     *
     * @return backup output file flag
     */
    public Boolean isBackupOutputFile() {
        return (Boolean) map.get(PARAM_BACKUP_OUTPUT_FILE);
    }

    /**
     * Sets the flag to indicate if any previously existing output file should be backed up.
     *
     * @param value the backup flag
     *
     * @see   #isBackupOutputFile()
     */
    public void setBackupOutputFile(Boolean value) {
        // backup flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_BACKUP_OUTPUT_FILE, value);
        } else {
            map.remove(PARAM_BACKUP_OUTPUT_FILE);
        }
    }

    /**
     * If <code>true</code>, then the process will be forcibly killed if it doesn't exit within the
     * {@link #getWaitForExit() wait time}. If <code>false</code>, the process will be allowed to continue to run for as
     * long as it needs - {@link #getWaitForExit()} will only force the caller to "wake up" and not block waiting for
     * the process to finish.
     *
     * @return kill flag (default is <code>false</code>, will never be <code>null</code>)
     */
    public Boolean isKillOnTimeout() {
        Boolean flag = (Boolean) map.get(PARAM_KILL_ON_TIMEOUT);

        if (flag == null) {
            flag = Boolean.FALSE;
        }

        return flag;
    }

    /**
     * Sets the flag to indicate if the process should be killed after the wait timeout expires.
     *
     * @param value the kill flag
     *
     * @see   #isKillOnTimeout()
     */
    public void setKillOnTimeout(Boolean value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_KILL_ON_TIMEOUT, value);
        } else {
            map.remove(PARAM_KILL_ON_TIMEOUT);
        }
    }

    /**
     * If <code>true</code>, then the executable will first be checked for its existance.
     * If the executable does not exist, the execution should fail-fast. If <code>false</code>,
     * the process will attempt to be executed no matter what. This will allow the operating
     * system to check its executable PATH to find the executable if required.
     *
     * @return check flag (default is <code>true</code>, will never be <code>null</code>)
     */
    public Boolean isCheckExecutableExists() {
        Boolean flag = (Boolean) map.get(PARAM_CHECK_EXECUTABLE_EXISTS);

        if (flag == null) {
            flag = Boolean.TRUE;
        }

        return flag;
    }

    /**
     * Sets the flag to indicate if the executable should be checked for existence first.
     *
     * @param value the check flag
     *
     * @see   #isCheckExecutableExists()
     */
    public void setCheckExecutableExists(Boolean value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            map.put(PARAM_CHECK_EXECUTABLE_EXISTS, value);
        } else {
            map.remove(PARAM_CHECK_EXECUTABLE_EXISTS);
        }
    }
}