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
package org.rhq.core.system;

import java.io.IOException;
import java.util.List;

import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.Swap;

/**
 * Interface of all native system info objects plus the "non-native" generic {@link JavaSystemInfo} object.
 * Implementations provide native access to low-level operations such as operating system process table access.
 *
 * @author John Mazzitelli
 */
public interface SystemInfo {
    /**
     * Returns <code>true</code> if the actual implementation is backed by a native library. An implementation that is
     * backed by a native library has access to very low-level information from the operating system on which the JVM is
     * currently running. If <code>false</code>, only the Java API can provide system level information. When this
     * returns <code>false</code>, some methods from the {@link SystemInfo} API will throw an
     * {@link UnsupportedOperationException} to indicate there is no native library available to perform the requested
     * operation and the Java API does not provide similar functionality that is able to satisfy the request.
     *
     * @return <code>true</code> if there is a low-level native API available for the JVM's platform
     */
    boolean isNative();

    /**
     * Returns the operating system type, if known; otherwise, {@link OperatingSystemType#JAVA} is returned (in which
     * case, the caller can examine the Java system property "os.name" to try to detect the type).
     *
     * @return the type of operating system, as detected by the native layer
     */
    OperatingSystemType getOperatingSystemType();

    /**
     * Returns the name of the operating system.
     *
     * @return OS name
     */
    String getOperatingSystemName();

    /**
     * Returns the version string of the operating system, as reported by the operating system.
     *
     * @return what the operating system says is its version
     */
    String getOperatingSystemVersion();

    /**
     * Returns the architecture of the underlying hardware
     * @return
     */
    String getSystemArchitecture();

    /**
     * Returns the hostname that the system is known as.
     *
     * @return machine host name
     *
     * @throws SystemInfoException
     */
    String getHostname() throws SystemInfoException;

    /**
     * Returns the information on all installed network adapters.
     *
     * @return list containing information on all network adapters
     *
     * @throws SystemInfoException
     */
    List<NetworkAdapterInfo> getAllNetworkAdapters() throws SystemInfoException;

    /**
     * Returns the information on all installed services found in the services manager.
     *
     * @return list container information on all services currently installed
     *
     * @throws SystemInfoException
     */
    List<ServiceInfo> getAllServices() throws SystemInfoException;

    /**
     * Returns the information on all processes found in the process table. This means that all processes currently
     * running and are visible to the user running the VM are returned.
     *
     * @return list containing information on all processes currently running at the time the method was called
     */
    List<ProcessInfo> getAllProcesses();

    /**
     * Returns ProcessInfo objects for all processes that match the provided PIQL query string
     *
     * @param  processInfoQuery the PIQL query string
     *
     * @return list containing process information
     */
    List<ProcessInfo> getProcesses(String processInfoQuery);

    /**
     * Returns the process information for the Java VM process this object is running in.
     *
     * @return this VM process's information
     */
    ProcessInfo getThisProcess();

    /**
     * Executes a process. The caller specifies the actual executable to run, the optional arguments to pass to the
     * executable, the optional name=value set of environment variables the new process will have, the optional new
     * working directory for the new process, the time (in milliseconds) to wait for the process to exit and the flag to
     * indicate if the process's output should be captured and passed back in the returned results.
     *
     * @param  processExecution the settings on how to execute the process
     *
     * @return the results of the execution
     */
    ProcessExecutionResults executeProcess(ProcessExecution processExecution);

    /**
     * Returns the number of CPUs on the hardware platform.
     *
     * @return CPU count
     */
    int getNumberOfCpus();

    /**
     * Returns information about memory installed on the platform.
     *
     * @return memory information
     */
    Mem getMemoryInfo();

    /**
     * Returns information about the virtual, swap memory installed on the platform.
     *
     * @return swap information
     */
    Swap getSwapInfo();

    /**
     * Reads a line of input from the console and returns that line as a string. You can ask the implementation to not
     * echo what the user typed into the console by passing <code>true</code> as the value to <code>noEcho</code>.
     * <code>noEcho</code> is a hint and not guaranteed to work since some implementations (like the Java-only fallback
     * implementation) will not be able to stop the console from echoing input.
     *
     * @param  noEcho if <code>true</code>, the implementation should try to not echo the input to the console
     *
     * @return the line read from the console input
     *
     * @throws IOException if failed to read console input
     */
    String readLineFromConsole(boolean noEcho) throws IOException;

    /**
     * Writes a line of output to the console. This method does <i>not</i> output any newline characters - if you wish
     * to end the output with a newline character, you must append it to the end of <code>line</code>.
     *
     * @param  line
     *
     * @throws IOException
     */
    void writeLineToConsole(String line) throws IOException;

    /**
     * Returns information about a specified CPU installed on the hardware where the JVM is running.
     *
     * @param  cpuIndex identifies the CPU whose information is to be returned; on a single-CPU system, the index must
     *                  be 0.
     *
     * @return information on the CPU
     */
    CpuInformation getCpu(int cpuIndex);

    /**
     * Returns information on all mounted file systems.
     *
     * @return list of all file systems on the platform
     */
    List<FileSystemInfo> getFileSystems();

    /**
     * Returns information on all mounted file systems,
     *
     * The usage information will be obtained when the method {@link FileSystemInfo#getFileSystemUsage()} is called.
     *
     * @return list of all file systems on the platform
     */

    List<FileSystemInfo> getFileSystemsDeferredUsageInfo();

    /**
     * Returns information on the mounted file system that hosts the given file or directory.
     *
     * @param  path the file or directory whose mounted file system should be returned
     *
     * @return the file system where the given file or directory is hosted
     */
    FileSystemInfo getFileSystem(String path);

    DirUsage getDirectoryUsage(String path);

    /**
     * Returns network adapter measurements for the named network adapter interface.
     * @param interfaceName
     * @return statistics for the named adapter interface
     */
    NetworkAdapterStats getNetworkAdapterStats(String interfaceName);

    /**
     * Returns network stats for connections that match the given address and port.
     * See {@link #getNetworkConnections(String, int)} for the semantics of the parameters.
     *
     * @param addressName
     * @param port
     * @return stats for the connections that are found
     *
     * @see #getNetworkConnections(String, int)
     */
    NetworkStats getNetworkStats(String addressName, int port);

    /**
     * Returns information on all known network connections from the given address/port.
     * If address is <code>null</code>, connections from all local addresses will be returned.
     * If port is <code>0</code>, then connections on all local ports will be returned.
     *
     * @param addressName if not <code>null</code>, the returned connections are from this local address only
     * @param port if not <code>0</code>, the returned connections are from this local port only
     * @return the matched connections
     */
    List<NetConnection> getNetworkConnections(String addressName, int port);
}