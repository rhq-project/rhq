/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.core.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.Swap;

import org.rhq.core.util.exec.ProcessExecutionOutputStream;
import org.rhq.core.util.exec.ProcessExecutor;
import org.rhq.core.util.exec.ProcessExecutorResults;
import org.rhq.core.util.exec.ProcessToStart;

/**
 * This is the "non-native" fallback implementation of a {@link SystemInfo} used in the case when there is no native
 * library available that can be used to obtain low-level system information. This implementation cannot provide all the
 * types of information that a true {@link NativeSystemInfo} implementation can - in the cases where this implementation
 * cannot provide the requested information, the {@link UnsupportedOperationException} exception will be thrown.
 *
 * @author John Mazzitelli
 */
public class JavaSystemInfo implements SystemInfo {

    private final Log log = LogFactory.getLog(JavaSystemInfo.class);

    private final ProcessExecutor javaExec;

    /**
     * Constructor for {@link JavaSystemInfo} with package scope so only the {@link SystemInfoFactory} can instantiate
     * this object.
     *
     * @param threadPool executor thread pool used for managing sub processes
     */
    JavaSystemInfo(ExecutorService threadPool) {
        javaExec = new ProcessExecutor(threadPool);
    }

    /**
     * Returns <code>false</code> - this implementation relies solely on the Java API to get all the system information
     * it can.
     *
     * @see SystemInfo#isNative()
     */
    public boolean isNative() {
        return false;
    }

    /**
     * Returns {@link OperatingSystemType#JAVA} unless the type can be determined via the Java "os.name" property. The
     * caller can call {@link #getOperatingSystemName()} to try to determine the real operating system type if this
     * method cannot.
     *
     * @see org.rhq.core.system.SystemInfo#getOperatingSystemType()
     */
    public OperatingSystemType getOperatingSystemType() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.US);

        if (osName.indexOf("windows") > -1) {
            return OperatingSystemType.WINDOWS;
        } else if (osName.indexOf("linux") > -1) {
            return OperatingSystemType.LINUX;
        } else if ((osName.indexOf("solaris") > -1) || (osName.indexOf("sunos") > -1)) {
            return OperatingSystemType.SOLARIS;
        } else if (osName.indexOf("bsd") > -1) {
            return OperatingSystemType.BSD;
        } else if (osName.indexOf("aix") > -1) {
            return OperatingSystemType.AIX;
        } else if (osName.indexOf("hp-ux") > -1) {
            return OperatingSystemType.HPUX;
        } else if (osName.indexOf("mac") > -1) // "Mac OS X"
        {
            return OperatingSystemType.OSX;
        }

        // TODO - find out the other os.name values for the other platforms

        log.warn("Defaulting to Java OS. Did not recogize [" + osName + "], derived from ["
            + System.getProperty("os.name") + "]");
        return OperatingSystemType.JAVA;
    }

    /**
     * Returns the value of the Java system property <code>os.name</code>.
     *
     * @see SystemInfo#getOperatingSystemName()
     */
    public String getOperatingSystemName() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the value of the Java system property <code>os.version</code>.
     *
     * @see SystemInfo#getOperatingSystemVersion()
     */
    public String getOperatingSystemVersion() {
        return System.getProperty("os.version");
    }

    /**
     * This returns the canonical hostname as it is known via the Java API <code>InetAddress.getLocalHost()</code>.
     *
     * @see SystemInfo#getHostname()
     */
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw getUnsupportedException(e);
        }
    }

    /**
     * Returns partially filled {@link NetworkAdapterInfo} objects - only the data available via the Java API
     * (particularly the <code>NetworkInterface</code> class) will be available in the returned objects.
     *
     * @see SystemInfo#getAllServices()
     */
    public List<NetworkAdapterInfo> getAllNetworkAdapters() throws SystemInfoException {
        throw getUnsupportedException("Cannot get list of network adaptors without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about installed
     * services.
     *
     * @see SystemInfo#getAllServices()
     */
    public List<ServiceInfo> getAllServices() throws UnsupportedOperationException {
        throw getUnsupportedException("Cannot get list of installed services without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about currently running
     * processes.
     *
     * @see SystemInfo#getAllProcesses()
     */
    public List<ProcessInfo> getAllProcesses() throws UnsupportedOperationException {
        throw getUnsupportedException("Cannot get the process table information without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about processes.
     *
     * @see SystemInfo#getProcesses(String)
     */
    public List<ProcessInfo> getProcesses(String processInfoQuery) {
        throw getUnsupportedException("Cannot get the process table information without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about currently running
     * processes.
     *
     * @see SystemInfo#getThisProcess()
     */
    public ProcessInfo getThisProcess() {
        throw getUnsupportedException("Cannot get information on this process without native support");
    }

    /**
     * Spawns a new process using the Java Runtime API.
     *
     * @see SystemInfo#executeProcess(ProcessExecution)
     */
    public ProcessExecutionResults executeProcess(ProcessExecution processExecution) {
        ProcessToStart process = new ProcessToStart();
        ProcessExecutionResults executionResults = new ProcessExecutionResults();

        process.setProgramExecutable(processExecution.getExecutable());
        process.setCheckExecutableExists(processExecution.isCheckExecutableExists());
        process.setArguments(processExecution.getArgumentsAsArray());
        process.setEnvironment(processExecution.getEnvironmentVariablesAsArray());
        process.setWorkingDirectory(processExecution.getWorkingDirectory());
        process.setWaitForExit(Long.valueOf(processExecution.getWaitForCompletion()));
        process.setCaptureOutput(Boolean.valueOf(processExecution.getCaptureMode().isCapture()));
        process.setKillOnTimeout(Boolean.valueOf(processExecution.isKillOnTimeout()));

        ProcessExecutionOutputStream outputStream = processExecution.getCaptureMode().createOutputStream();
        process.setOutputStream(outputStream);
        executionResults.setCapturedOutputStream(outputStream);


        ProcessExecutorResults javaExecResults = javaExec.execute(process);
        executionResults.setExitCode(javaExecResults.getExitCode());
        executionResults.setError(javaExecResults.getError());
        executionResults.setProcess(javaExecResults.getProcess());

        return executionResults;
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about the number of
     * CPUs.
     *
     * @see SystemInfo#getNumberOfCpus()
     */
    public int getNumberOfCpus() {
        throw getUnsupportedException("Cannot get the number of CPUs without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about low level memory
     * details.
     *
     * @see SystemInfo#getMemoryInfo()
     */
    public Mem getMemoryInfo() {
        throw getUnsupportedException("Cannot get any information about memory without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about low level swap
     * details.
     *
     * @see SystemInfo#getSwapInfo()
     */
    public Swap getSwapInfo() {
        throw getUnsupportedException("Cannot get any information about swap without native support");
    }

    /**
     * Reads from <code>System.in</code>.
     *
     * @see SystemInfo#readLineFromConsole(boolean)
     */
    public String readLineFromConsole(boolean noEcho) throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * Writes to <code>System.out</code>.
     *
     * @see SystemInfo#writeLineToConsole(String)
     */
    public void writeLineToConsole(String line) throws IOException {
        System.out.print(line); // note: don't use println - let the caller append newline char to 'line' if needed
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about CPUs.
     *
     * @see SystemInfo#getCpu(int)
     */
    public CpuInformation getCpu(int cpuIndex) {
        throw getUnsupportedException("Cannot get CPU information without native support");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about file systems.
     *
     * @see SystemInfo#getFileSystems()
     */
    public List<FileSystemInfo> getFileSystems() {
        throw getUnsupportedException("Cannot get file systems information");
    }

    /**
     * Throws {@link UnsupportedOperationException} since the Java API cannot obtain information about file systems.
     *
     * @see SystemInfo#getFileSystem(String)
     */
    public FileSystemInfo getFileSystem(String directory) {
        throw getUnsupportedException("Cannot get file system information");
    }

    @Override
    public DirUsage getDirectoryUsage(String path) {
        throw getUnsupportedException("Cannot get directory information yet");
    }

    public NetworkAdapterStats getNetworkAdapterStats(String interfaceName) {
        throw getUnsupportedException("Cannot get network adapter stats");
    }

    public NetworkStats getNetworkStats(String addressName, int port) {
        throw getUnsupportedException("Cannot get network stats");
    }

    public List<NetConnection> getNetworkConnections(String addressName, int port) {
        throw getUnsupportedException("Cannot get network connections");
    }

    private UnsupportedOperationException getUnsupportedException(Exception e) {
        return new UnsupportedOperationException("No native library available - " + e, e);
    }

    private UnsupportedOperationException getUnsupportedException(String err) {
        return new UnsupportedOperationException("No native library available - " + err);
    }

    public String getSystemArchitecture() {
        return System.getProperty("os.arch");
    }
}