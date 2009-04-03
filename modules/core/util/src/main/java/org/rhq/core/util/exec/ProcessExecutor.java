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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.rhq.core.util.UtilI18NResourceKeys;

/**
 * Executes a process using 100% Java API.
 *
 * <p><b>Warning: caution should be exercised when using this class - it allows any process to be started with no
 * security restrictions.</b></p>
 *
 * @author John Mazzitelli
 */
public class ProcessExecutor {
    /**
     * This executes any operating system process as described in the given start command. When this method returns, it
     * can be assumed that the process was launched but not necessarily finished. The caller can ask this method to
     * block until process exits by setting {@link ProcessToStart#setWaitForExit(Long)} to a positive, non-zero timeout
     * in milliseconds. On error, the exception will be returned in the returned results.
     *
     * @param  processToStart the information on what process to start
     *
     * @return the results of the process execution
     */
    public ProcessExecutorResults execute(ProcessToStart processToStart) {
        ProcessExecutorResults results = new ProcessExecutorResults();

        try {
            Integer exitCode = startProgram(processToStart);
            results.setExitCode(exitCode);
        } catch (Throwable t) {
            results.setError(t);
        }

        return results;
    }

    /**
     * Starts a child process. When this method returns, it can be assumed that the process was launched. On error, an
     * exception is thrown. Note that this method will also wait for the process to exit if
     * {@link ProcessToStart#getWaitForExit()} is positive, non-zero. In that case, the returned value will be the exit
     * code of the process. If this method is told not to wait, the returned value will be <code>null</code>.
     *
     * @param  process provides the information necessary to start the child process
     *
     * @return process exit code (if the method waited for it to exit) or <code>null</code> if this method was to only
     *         start the process but not wait or was to wait and the wait time expired before the process exited
     *
     * @throws Exception if any error occurs while trying to start the child process
     */
    protected Integer startProgram(final ProcessToStart process) throws Exception {
        // prepare the process comand line and environment
        String[] cmdline = getCommandLine(process);
        File workingDir = getWorkingDirectory(process);
        String[] environment = process.getEnvironment();

        // execute the program
        final Process childProcess = Runtime.getRuntime().exec(cmdline, environment, workingDir);

        // redirect the program's streams
        // WARNING:
        // It seems there is no way to get around a possible race condition - what if the process
        // was so fast that it exited already?  We didn't get a chance to capture its output.
        // I see a unit test that periodically fails because it doesn't get any captured output when
        // it should - I think it is because of this race condition.  But there is no Java API that
        // let's me redirect a process' streams before the process is told to start.
        redirectStreams(process, childProcess);

        final Integer[] retExitCode = new Integer[1];

        // wait if told to - note that the default is not to wait
        if (process.getWaitForExit().intValue() > 0) {
            Thread waitThread = new Thread("ExecuteProcess-" + process.getProgramTitle()) {
                public void run() {
                    try {
                        int exitCode = childProcess.waitFor();
                        retExitCode[0] = new Integer(exitCode);
                    } catch (InterruptedException e) {
                    }
                }
            };

            waitThread.setDaemon(true);
            waitThread.start();
            waitThread.join(process.getWaitForExit().intValue());
            waitThread.interrupt();

            if (retExitCode[0] == null) {
                // never got the exit code so the wait time must have expired, kill the process if configured to do so
                if (process.isKillOnTimeout().booleanValue()) {
                    childProcess.destroy();
                }
            }
        }

        return retExitCode[0];
    }

    /**
     * This method redirects the stdout/stderr streams of the child process to the output log file and pipes the
     * contents of the input file (if one was specified) to the stdin stream of the child process.
     *
     * <p>This is done asynchronously so as to avoid deadlocking and to allow the main thread to continue its work.</p>
     *
     * <p>Once the child process dies, so do the piping threads.</p>
     *
     * @param  process      used to configure the process
     * @param  childProcess the newly spawned child process
     *
     * @throws IOException if failed to pipe data to/from stdin/stdout
     */
    protected void redirectStreams(ProcessToStart process, Process childProcess) throws IOException {
        // Process.getInputStream is actually the process's stdout output
        // Process.getOutputStream is actually the process's stdin intput
        // Process.getErrorStream is the process's stderr output
        InputStream stdout = childProcess.getInputStream();
        InputStream stderr = childProcess.getErrorStream();
        OutputStream stdin = childProcess.getOutputStream();

        // pipe both stderr and stdout to the output file asynchronously so we don't hang collecting output data infinitely
        String threadNamePrefix = process.getProgramTitle();
        OutputStream fileOutputStream = null;

        if (process.isCaptureOutput().booleanValue()) {
            fileOutputStream = process.getOutputStream(); // override the file if given a stream already

            if (fileOutputStream == null) {
                File outputFile = createOutputFile(process);
                if (outputFile != null) {
                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                }
            }
        }

        if (threadNamePrefix == null) {
            threadNamePrefix = process.getProgramExecutable();
        }

        StreamRedirector stdoutThread = new StreamRedirector(threadNamePrefix + "-stdout", stdout, fileOutputStream);

        StreamRedirector stderrThread = new StreamRedirector(threadNamePrefix + "-stderr", stderr, fileOutputStream);

        stdoutThread.start();
        stderrThread.start();

        // if an input file was specified, take the file's data and write it to the process' stdin
        File inputFile = getInputFile(process);

        if (inputFile != null) {
            BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(inputFile));
            byte[] fileBytes = new byte[4096];
            int fileBytesRead = 1; // prime the pump

            while (fileBytesRead > 0) {
                fileBytesRead = fileInputStream.read(fileBytes);
                if (fileBytesRead > 0) {
                    stdin.write(fileBytes, 0, fileBytesRead);
                }
            }

            fileInputStream.close();
        }

        stdin.close();

        return;
    }

    /**
     * Creates the output file and returns its <code>File</code> representation. This is the location where the child
     * process' stdout/stderr output streams get redirected to. Note that if the file does not exist, it will be
     * created; if it does exist, the original will be renamed with a timestamp before a new file is created only if the
     * {@link ProcessToStart#isBackupOutputFile() backup output file flag} is <code>Boolean.TRUE</code>; otherwise it
     * will be overwritten.
     *
     * <p>If the {@link ProcessToStart#getOutputDirectory() output directory} was not specified, a temporary location
     * will be used (i.e. the System property <code>java.io.tmpdir</code>). If the
     * {@link ProcessToStart#getOutputFile() output file name} was not specified, one will be generated automatically -
     * using the {@link ProcessToStart#getProgramTitle() title} if one was specified.</p>
     *
     * <p>If we are not to {@link ProcessToStart#isCaptureOutput() capture the output}, this method returns <code>
     * null</code>.</p>
     *
     * @param  process the process to start
     *
     * @return output log file (may be <code>null</code>)
     *
     * @throws IOException           if the output file could not be created
     * @throws FileNotFoundException the output directory does not exist or is not a valid directory
     */
    protected File createOutputFile(ProcessToStart process) throws IOException {
        // return immediately if the client does want us to capture the output
        if (!process.isCaptureOutput().booleanValue()) {
            return null;
        }

        String directoryStr = process.getOutputDirectory();
        String filenameStr = process.getOutputFile();

        // determine the valid output directory to use
        if ((directoryStr == null) || (directoryStr.length() == 0)) {
            directoryStr = System.getProperty("java.io.tmpdir");
        }

        File directoryFile = new File(directoryStr);

        if (!directoryFile.exists()) {
            throw new FileNotFoundException(UtilI18NResourceKeys.MSG.getMsg(
                UtilI18NResourceKeys.PROCESS_EXEC_OUTPUT_DIR_DOES_NOT_EXIST, directoryFile));
        }

        if (!directoryFile.isDirectory()) {
            throw new IOException(UtilI18NResourceKeys.MSG.getMsg(UtilI18NResourceKeys.PROCESS_EXEC_OUTPUT_DIR_INVALID,
                directoryFile));
        }

        // determine the valid filename to use and create the file if necessary
        // IF the filename was not specified
        //    Create a file with a name that has the title or executable as part of the filename
        // ELSE
        //    Use the filename given, renaming any existing file if we were told to back it up
        // END IF
        File retOutputFile;

        if ((filenameStr == null) || (filenameStr.length() == 0)) {
            String prefix = process.getProgramTitle();

            if (prefix == null) {
                prefix = process.getProgramExecutable();
            }

            prefix += "__"; // ensures that we follow createTempFile requirement that it be at least 3 characters

            retOutputFile = File.createTempFile(prefix, ".out", directoryFile);
        } else {
            retOutputFile = new File(directoryFile, filenameStr);

            if (retOutputFile.isDirectory()) {
                throw new IOException(UtilI18NResourceKeys.MSG.getMsg(
                    UtilI18NResourceKeys.PROCESS_EXEC_OUTPUT_FILE_IS_DIR, retOutputFile));
            }

            if (retOutputFile.exists()) {
                if ((process.isBackupOutputFile() != null) && process.isBackupOutputFile().booleanValue()) {
                    renameFile(retOutputFile);
                } else {
                    retOutputFile.delete();
                }
            }

            if (!retOutputFile.createNewFile()) {
                throw new IOException(UtilI18NResourceKeys.MSG.getMsg(
                    UtilI18NResourceKeys.PROCESS_EXEC_OUTPUT_FILE_CREATION_FAILURE, retOutputFile));
            }
        }

        return retOutputFile;
    }

    /**
     * Gets the input file and returns its <code>File</code> representation. This is the file whose data will be sent to
     * the child process' stdin input stream. Note that if the input file that was specified in the command does not
     * exist, an exception is thrown. If the command does not specify an input file, <code>null</code> is returned.
     *
     * @param  process the start command
     *
     * @return input file (may be <code>null</code>)
     *
     * @throws IOException           if the input file could not be found
     * @throws FileNotFoundException the input directory does not exist or is not a valid directory
     */
    protected File getInputFile(ProcessToStart process) throws IOException {
        String directoryStr = process.getInputDirectory();
        String filenameStr = process.getInputFile();

        boolean filenameSpecified = (filenameStr != null) && (filenameStr.length() > 0);
        boolean directorySpecified = (directoryStr != null) && (directoryStr.length() > 0);

        if (directorySpecified ^ filenameSpecified) {
            throw new IOException(UtilI18NResourceKeys.MSG.getMsg(
                UtilI18NResourceKeys.PROCESS_EXEC_INPUT_PARAMS_INVALID, process));
        }

        if (!directorySpecified) {
            return null;
        }

        // determine the valid input directory to use
        File directoryFile = new File(directoryStr);

        if (!directoryFile.exists()) {
            throw new FileNotFoundException(UtilI18NResourceKeys.MSG.getMsg(
                UtilI18NResourceKeys.PROCESS_EXEC_INPUT_DIR_DOES_NOT_EXIST, directoryFile));
        }

        if (!directoryFile.isDirectory()) {
            throw new IOException(UtilI18NResourceKeys.MSG.getMsg(UtilI18NResourceKeys.PROCESS_EXEC_INPUT_DIR_INVALID,
                directoryFile));
        }

        // determine the valid input filename to use
        File retInputFile = new File(directoryFile, filenameStr);

        if (!retInputFile.exists()) {
            throw new FileNotFoundException(UtilI18NResourceKeys.MSG.getMsg(
                UtilI18NResourceKeys.PROCESS_EXEC_INPUT_FILE_DOES_NOT_EXIST, retInputFile));
        }

        if (!retInputFile.canRead()) {
            throw new IOException(UtilI18NResourceKeys.MSG.getMsg(
                UtilI18NResourceKeys.PROCESS_EXEC_INPUT_FILE_UNREADABLE, retInputFile));
        }

        if (retInputFile.isDirectory()) {
            throw new IOException(UtilI18NResourceKeys.MSG.getMsg(UtilI18NResourceKeys.PROCESS_EXEC_INPUT_FILE_IS_DIR,
                retInputFile));
        }

        return retInputFile;
    }

    /**
     * Returns the full pathname to the program executable. If the program executable does not exist, an exception is
     * thrown.
     *
     * @param  process the process to start
     *
     * @return full path name to the program executable file
     *
     * @throws FileNotFoundException if the program executable does not exist
     */
    protected String getFullProgramExecutablePath(ProcessToStart process) throws FileNotFoundException {
        File progFile = new File(process.getProgramDirectory(), process.getProgramExecutable());
        String result = progFile.getPath();

        // If executable verification has been turned off then assume the caller wants his executable "as-is".
        // Otherwise, validate and ensure a full path. 
        if (Boolean.TRUE.equals(process.isCheckExecutableExists())) {
            if (Boolean.TRUE.equals(process.isCheckExecutableExists()) && !progFile.exists()) {
                throw new FileNotFoundException(UtilI18NResourceKeys.MSG.getMsg(
                    UtilI18NResourceKeys.PROCESS_EXEC_PROGRAM_DOES_NOT_EXIST, progFile));
            }

            result = progFile.getAbsolutePath();
        }

        return result;
    }

    /**
     * Returns the full pathname to the working directory. An exception is thrown if the directory does not exist. If
     * the working directory is <code>null</code>, child process inherits the parent process's current working
     * directory.
     *
     * @param  process the process to start
     *
     * @return the working directory where the program should "start in" - its starting or current directory in other
     *         words
     *
     * @throws FileNotFoundException if the working directory does not exist
     */
    protected File getWorkingDirectory(ProcessToStart process) throws FileNotFoundException {
        File retWorkingDir = null;
        String workingDirString = process.getWorkingDirectory();

        if (workingDirString != null) {
            retWorkingDir = new File(workingDirString);
            if (!retWorkingDir.exists()) {
                throw new FileNotFoundException(UtilI18NResourceKeys.MSG.getMsg(
                    UtilI18NResourceKeys.PROCESS_EXEC_WORKING_DIR_DOES_NOT_EXIST, retWorkingDir));
            }
        }

        return retWorkingDir;
    }

    /**
     * Builds the command line containing the full path to the program executable and any arguments that are to be
     * passed to the program.
     *
     * @param  process the process to start
     *
     * @return array of command line arguments (the first of which is the full path to the program executable file)
     *
     * @throws FileNotFoundException if the program executable file does not exist
     */
    protected String[] getCommandLine(ProcessToStart process) throws FileNotFoundException {
        // determine where the executable is
        String fullProgramPath = getFullProgramExecutablePath(process);

        // build the command line
        String[] args = process.getArguments();
        int numArgs = (args != null) ? args.length : 0;
        String[] retCmdline = new String[numArgs + 1]; // +1 for the program executable path

        retCmdline[0] = fullProgramPath;

        if (numArgs > 0) {
            System.arraycopy(args, 0, retCmdline, 1, numArgs);
        }

        return retCmdline;
    }

    /**
     * Renames the given file by appending to its name a date/time stamp.
     *
     * @param  file the file to be renamed
     *
     * @throws IOException if failed to rename the file
     */
    private void renameFile(File file) throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("-yyyy-MM-dd--HH-mm-ss");
        String timestamp = formatter.format(new Date());
        String newFileName = file.getCanonicalPath() + timestamp;
        file.renameTo(new File(newFileName));
    }
}