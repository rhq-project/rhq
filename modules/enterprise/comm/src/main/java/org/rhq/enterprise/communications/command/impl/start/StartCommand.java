/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.communications.command.impl.start;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import org.rhq.enterprise.communications.command.AbstractCommand;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.InvalidParameterValueException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Command used to start operating system processes.
 *
 * <p><b>Warning: This command has security implications - it defines any process to start. A remote service that
 * executes this command should have some type of security restrictions in place, to avoid running rogue or unauthorized
 * commands.</b></p>
 *
 * @author John Mazzitelli
 */
public class StartCommand extends AbstractCommand {
    /**
     * command type constant identifying this command
     */
    public static final CommandType COMMAND_TYPE = new CommandType("start", 1);

    /**
     * identifies the parameter whose value is a simple title for this start command configuration (may be used for
     * output filename)
     */
    public static final ParameterDefinition PARAM_PROGRAM_TITLE = new ParameterDefinition("programTitle", String.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_PROGRAM_TITLE));

    /**
     * identifies the parameter whose value is the name of the program to execute
     */
    public static final ParameterDefinition PARAM_PROGRAM_EXE = new ParameterDefinition("programExecutable",
        String.class.getName(), ParameterDefinition.REQUIRED, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.START_COMMAND_PROGRAM_EXECUTABLE));

    /**
     * identifies the parameter whose value is the full path to the program's executable
     */
    public static final ParameterDefinition PARAM_PROGRAM_DIR = new ParameterDefinition("programDirectory",
        String.class.getName(), ParameterDefinition.REQUIRED, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.START_COMMAND_PROGRAM_DIRECTORY));

    /**
     * identifies the parameter whose value is an array of arguments to pass to the executable
     */
    public static final ParameterDefinition PARAM_ARGS = new ParameterDefinition("arguments", String[].class.getName(),
        ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN, CommI18NFactory
            .getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_ARGUMENTS));

    /**
     * identifies the parameter whose value is an Properties object containing environment variable name/value pairs to
     * give to the process
     */
    public static final ParameterDefinition PARAM_ENV = new ParameterDefinition("environmentVariables", String[].class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_ENVIRONMENT_VARIABLES));

    /**
     * identifies the parameter whose value is directory location to start the process in (the working directory).
     */
    public static final ParameterDefinition PARAM_WORKING_DIR = new ParameterDefinition("workingDirectory",
        String.class.getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.START_COMMAND_WORKING_DIRECTORY));

    /**
     * identifies the parameter whose value is the directory where the process's output log will be written
     */
    public static final ParameterDefinition PARAM_OUTPUT_DIR = new ParameterDefinition("outputDirectory", String.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_OUTPUT_DIRECTORY));

    /**
     * identifies the parameter whose value is the filename (in the output directory) where the process's stdout/stderr
     * output log will be written
     */
    public static final ParameterDefinition PARAM_OUTPUT_FILE = new ParameterDefinition("outputFile", String.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_OUTPUT_FILE));

    /**
     * identifies the parameter whose value is the directory location of the file that contains data to be fed into the
     * programs' stdin input stream
     */
    public static final ParameterDefinition PARAM_INPUT_DIR = new ParameterDefinition("inputDirectory", String.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_INPUT_DIRECTORY));

    /**
     * identifies the parameter whose value is the file name that contains data to be fed into the programs' stdin input
     * stream
     */
    public static final ParameterDefinition PARAM_INPUT_FILE = new ParameterDefinition("inputFile", String.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_INPUT_FILE));

    /**
     * identifies the parameter whose value is the time to wait until the process has exited
     */
    public static final ParameterDefinition PARAM_WAIT_FOR_EXIT = new ParameterDefinition("waitForExit", Long.class
        .getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE, ParameterDefinition.NOT_HIDDEN,
        CommI18NFactory.getMsg().getMsg(CommI18NResourceKeys.START_COMMAND_WAIT_FOR_EXIT));

    /**
     * identifies the parameter whose value is the flag to determine if the started process's output should be dumped to
     * the output file
     */
    public static final ParameterDefinition PARAM_CAPTURE_OUTPUT = new ParameterDefinition("captureOutput",
        Boolean.class.getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.START_COMMAND_CAPTURE_OUTPUT));

    /**
     * identifies the parameter whose value is the flag to determine whether to backup any previously existing output
     * file; if false, it will be overwritten
     */
    public static final ParameterDefinition PARAM_BACKUP_OUTPUT_FILE = new ParameterDefinition("backupOutputFile",
        Boolean.class.getName(), ParameterDefinition.OPTIONAL, ParameterDefinition.NOT_NULLABLE,
        ParameterDefinition.NOT_HIDDEN, CommI18NFactory.getMsg().getMsg(
            CommI18NResourceKeys.START_COMMAND_BACKUP_OUTPUT_FILE));

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link StartCommand}.
     *
     * @see AbstractCommand#AbstractCommand()
     */
    public StartCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        super();
    }

    /**
     * Constructor for {@link StartCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Map)
     */
    public StartCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        super(commandParameters);
    }

    /**
     * Constructor for {@link StartCommand}.
     *
     * @see AbstractCommand#AbstractCommand(Command)
     */
    public StartCommand(Command commandToTransform) {
        super(commandToTransform);
    }

    /**
     * Returns the title for this program. This is a simple name to help identify this start command configuration or to
     * just help identify the program being launched.
     *
     * <p>This is useful when, for example, a Java JVM needs to be started and you want to set the title to describe the
     * main class being launched (since all Java executables that are launched typically have the same executable name,
     * that of "java.exe").</p>
     *
     * @return the title of the program
     */
    public String getProgramTitle() {
        return (String) getParameterValue(PARAM_PROGRAM_TITLE.getName());
    }

    /**
     * Sets the title for this program. This is a simple name to help identify this start command configuration or to
     * just help identify the program being launched.
     *
     * <p>This is useful when, for example, a Java JVM needs to be started and you want to set the title to describe the
     * main class being launched (since all Java executables that are launched typically have the same executable name,
     * that of "java.exe").</p>
     *
     * @param value the title of the program (may be <code>null</code>)
     */
    public void setProgramTitle(String value) {
        // the title is optional but non-nullable; remove the param value if null
        if (value != null) {
            setParameterValue(PARAM_PROGRAM_TITLE.getName(), value);
        } else {
            removeParameterValue(PARAM_PROGRAM_TITLE.getName());
        }
    }

    /**
     * Returns the name of the program to execute. This is just the executable file name without any path information
     * (for that information, see {@link #getProgramDirectory()}).
     *
     * @return program executable name
     */
    public String getProgramExecutable() {
        return (String) getParameterValue(PARAM_PROGRAM_EXE.getName());
    }

    /**
     * Sets the name of the program to execute. This is just the executable file name without any path information (for
     * setting that information, see {@link #setProgramDirectory(String)}).
     *
     * @param value program executable name
     */
    public void setProgramExecutable(String value) {
        setParameterValue(PARAM_PROGRAM_EXE.getName(), value);
    }

    /**
     * Returns the full path to the program executable. This is just the directory where the executable file is located
     * without the name of the executable itself (for that information, see {@link #getProgramExecutable()}).
     *
     * @return program executable directory location
     */
    public String getProgramDirectory() {
        return (String) getParameterValue(PARAM_PROGRAM_DIR.getName());
    }

    /**
     * Sets the full path to the program executable. This is just the directory where the executable file is located
     * without the name of the executable itself (for setting that information, see {@link #getProgramExecutable()}).
     *
     * @param value program executable directory location
     */
    public void setProgramDirectory(String value) {
        setParameterValue(PARAM_PROGRAM_DIR.getName(), value);
    }

    /**
     * Returns an array of strings that are the argument values passed on the command line to the program executable. If
     * <code>null</code> or empty, no arguments will be passed to the program.
     *
     * @return array of program arguments
     */
    public String[] getArguments() {
        return (String[]) getParameterValue(PARAM_ARGS.getName());
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
            setParameterValue(PARAM_ARGS.getName(), value);
        } else {
            removeParameterValue(PARAM_ARGS.getName());
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
        return (String[]) getParameterValue(PARAM_ENV.getName());
    }

    /**
     * Convienence method that not only returns the environment variables as a String array, but the environment
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
     * @throws InvalidParameterValueException if an environment variable string doesn't conform to the format: <code>
     *                                        name=value</code>
     * @throws IllegalArgumentException       if <code>properties</code> is <code>null</code>
     */
    public String[] getEnvironment(Properties properties) throws InvalidParameterValueException,
        IllegalArgumentException {
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
                    throw new InvalidParameterValueException(CommI18NFactory.getMsgWithLoggerLocale().getMsg(
                        CommI18NResourceKeys.START_COMMAND_ENV_VAR_BAD_FORMAT, envVarString));
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
        setParameterValue(PARAM_ENV.getName(), value);
    }

    /**
     * Convienence method that takes a <code>Properties</code> object containing the environment variables, as opposed
     * to an array of strings (see {@link #setEnvironment(String[])}. Sets the environment variable name/value pairs
     * that define the environment to be passed to the started process. A*<code>null</code> will allow the subprocess to
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
        return (String) getParameterValue(PARAM_WORKING_DIR.getName());
    }

    /**
     * Sets the working directory of the new process (known also as the current directory or the startup directory). A
     * <code>null</code> allows the subprocess to inherit the current working directory of the parent process.
     *
     * @param value the working directory path (may be <code>null</code>)
     */
    public void setWorkingDirectory(String value) {
        setParameterValue(PARAM_WORKING_DIR.getName(), value);
    }

    /**
     * Returns the directory where the program's output log file will be written. If <code>null</code>, a directory will
     * be assigned (typically the <code>java.io.tmpdir</code> directory).
     *
     * @return the directory where the output log will be written to (may be <code>null</code>)
     */
    public String getOutputDirectory() {
        return (String) getParameterValue(PARAM_OUTPUT_DIR.getName());
    }

    /**
     * Sets the directory where the program's output log file will be written. If <code>null</code>, a directory will be
     * assigned (typically the <code>java.io.tmpdir</code> directory).
     *
     * @param value the directory where the output log will be written to (may be <code>null</code>)
     */
    public void setOutputDirectory(String value) {
        // output dir is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_OUTPUT_DIR.getName(), value);
        } else {
            removeParameterValue(PARAM_OUTPUT_DIR.getName());
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
     * @return the program's output log file (may be <code>null</code>)
     */
    public String getOutputFile() {
        return (String) getParameterValue(PARAM_OUTPUT_FILE.getName());
    }

    /**
     * Sets the file (to be placed in the {@link #getOutputDirectory() output directory}) where the program's output
     * will be written. This should just be the filename; its full path will be specified by the
     * {@link #getOutputDirectory() output directory}.
     *
     * <p>It is in this file where you can view the program's stdout/stderr output stream data.</p>
     *
     * <p>If <code>null</code>, an auto-generated filename will be used.</p>
     *
     * @param value the program's output log file (may be <code>null</code>)
     */
    public void setOutputFile(String value) {
        // output file is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_OUTPUT_FILE.getName(), value);
        } else {
            removeParameterValue(PARAM_OUTPUT_FILE.getName());
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
        return (String) getParameterValue(PARAM_INPUT_DIR.getName());
    }

    /**
     * Sets the directory where the {@link #getInputFile() input file} is located.
     *
     * <p>If this is specified, the {@link #setInputFile(String) input file} must also be specified.</p>
     *
     * @param value directory where the input file is located (may be <code>null</code>)
     */
    public void setInputDirectory(String value) {
        // input directory is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_INPUT_DIR.getName(), value);
        } else {
            removeParameterValue(PARAM_INPUT_DIR.getName());
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
        return (String) getParameterValue(PARAM_INPUT_FILE.getName());
    }

    /**
     * Sets the name of the file that contains data to be input to the program. The data found in this file will be
     * passed into the started program via the stdin input stream.
     *
     * <p>If this is specified, the {@link #setInputDirectory(String) input directory} must also be specified.</p>
     *
     * @param value name of the input file (may be <code>null</code>)
     */
    public void setInputFile(String value) {
        // input file is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_INPUT_FILE.getName(), value);
        } else {
            removeParameterValue(PARAM_INPUT_FILE.getName());
        }
    }

    /**
     * If <code>null</code> or is 0 or less, the process executor will not wait for the process to exit before returning
     * the response. Otherwise, this is the number of milliseconds the process executor will wait for the process to
     * exit. If the time expires, the response will return but the process will continue to run (an attempt to kill the
     * process will not be made).
     *
     * @return wait time in milliseconds
     */
    public Long getWaitForExit() {
        return (Long) getParameterValue(PARAM_WAIT_FOR_EXIT.getName());
    }

    /**
     * Sets the time to wait for the process to exit.
     *
     * @param value wait time in milliseconds
     *
     * @see   #getWaitForExit()
     */
    public void setWaitForExit(Long value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_WAIT_FOR_EXIT.getName(), value);
        } else {
            removeParameterValue(PARAM_WAIT_FOR_EXIT.getName());
        }
    }

    /**
     * If <code>true</code>, the started process' output will be captured and written to the output file. If <code>
     * false</code>, no output file is created and the process' output is simply consumed and ignored.
     *
     * @return capture output flag
     */
    public Boolean isCaptureOutput() {
        return (Boolean) getParameterValue(PARAM_CAPTURE_OUTPUT.getName());
    }

    /**
     * Sets the flag to indicate if the process' output should be captured in the output file. If <code>true</code>, the
     * started process' output will be captured and written to the output file. If <code>false</code>, no output file is
     * created and the process' output is simply consumed and ignored.
     *
     * @param value capture output flag
     */
    public void setCaptureOutput(Boolean value) {
        // flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_CAPTURE_OUTPUT.getName(), value);
        } else {
            removeParameterValue(PARAM_CAPTURE_OUTPUT.getName());
        }
    }

    /**
     * If <code>true</code>, any previously existing output file will be backed up by renaming it with a date/timestamp.
     * If <code>false</code>, any previously existing output file will be overwritten.
     *
     * @return backup output file flag
     */
    public Boolean isBackupOutputFile() {
        return (Boolean) getParameterValue(PARAM_BACKUP_OUTPUT_FILE.getName());
    }

    /**
     * Sets the flag to indicate if any previously existing output file should be backed up. If <code>true</code>, any
     * previously existing output file will be backed up by renaming it with a date/timestamp. If <code>false</code>,
     * any previously existing output file will be overwritten.
     *
     * @param value the backup flag
     */
    public void setBackupOutputFile(Boolean value) {
        // backup flag is optional, but non-nullable; remove it if null is passed in
        if (value != null) {
            setParameterValue(PARAM_BACKUP_OUTPUT_FILE.getName(), value);
        } else {
            removeParameterValue(PARAM_BACKUP_OUTPUT_FILE.getName());
        }
    }

    /**
     * Ensures that if either {@link #PARAM_INPUT_DIR} or {@link #PARAM_INPUT_FILE} are specified, that <b>both</b> are
     * specified. In other words, you can't specify one without specifying the other.
     *
     * @see Command#checkParameterValidity(boolean)
     */
    public void checkParameterValidity(boolean convertIfNecessary) throws InvalidParameterValueException {
        boolean inputDirSpecified = (getParameterValue(PARAM_INPUT_DIR.getName()) != null);
        boolean inputFileSpecified = (getParameterValue(PARAM_INPUT_FILE.getName()) != null);

        // XOR - if both are specified or neither are specified, that is OK; if one but not the other is specified, that's invalid
        if (inputDirSpecified ^ inputFileSpecified) {
            throw new InvalidParameterValueException(CommI18NFactory.getMsg().getMsg(
                CommI18NResourceKeys.START_COMMAND_BAD_INPUT_PARAMS, toString()));
        }

        super.checkParameterValidity(convertIfNecessary);

        return;
    }

    /**
     * @see AbstractCommand#buildCommandType()
     */
    protected CommandType buildCommandType() {
        return COMMAND_TYPE;
    }

    /**
     * @see AbstractCommand#buildParameterDefinitions()
     */
    protected ParameterDefinition[] buildParameterDefinitions() {
        return new ParameterDefinition[] { PARAM_PROGRAM_TITLE, PARAM_PROGRAM_EXE, PARAM_PROGRAM_DIR, PARAM_ARGS,
            PARAM_ENV, PARAM_WORKING_DIR, PARAM_OUTPUT_DIR, PARAM_OUTPUT_FILE, PARAM_INPUT_DIR, PARAM_INPUT_FILE,
            PARAM_WAIT_FOR_EXIT, PARAM_CAPTURE_OUTPUT, PARAM_BACKUP_OUTPUT_FILE };
    }
}