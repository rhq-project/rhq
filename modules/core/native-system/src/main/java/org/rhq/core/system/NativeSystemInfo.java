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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.Swap;

import org.rhq.core.system.pquery.ProcessInfoQuery;

/**
 * The superclass for all the native {@link SystemInfo} implementations. You are free to subclass this implementation if
 * there are additional platform-specific methods that need to be exposed. Most functionality, however, can be exposed
 * via this native superclass implementation.
 *
 * <p>This implementation uses SIGAR. To enable debug logging in SIGAR, set the system property
 * <code>sigar.nativeLogging</code> or call {@link Sigar#enableLogging(boolean)}.</p>
 *
 * @author John Mazzitelli
 */
public class NativeSystemInfo implements SystemInfo {

    private final Log log = LogFactory.getLog(NativeSystemInfo.class);

    private SigarProxy sigar;

    /**
     * Always returns <code>true</code> to indicate that the native library is available.
     *
     * @see SystemInfo#isNative()
     */
    public boolean isNative() {
        return true;
    }

    public OperatingSystemType getOperatingSystemType() {
        OperatingSystem os = OperatingSystem.getInstance();
        if (OperatingSystem.NAME_LINUX.equals(os.getName())) {
            return OperatingSystemType.LINUX;
        }

        if (OperatingSystem.NAME_SOLARIS.equals(os.getName())) {
            return OperatingSystemType.SOLARIS;
        }

        if (OperatingSystem.NAME_WIN32.equals(os.getName())) {
            return OperatingSystemType.WINDOWS;
        }

        if (OperatingSystem.NAME_HPUX.equals(os.getName())) {
            return OperatingSystemType.HPUX;
        }

        if (OperatingSystem.NAME_AIX.equals(os.getName())) {
            return OperatingSystemType.AIX;
        }

        if (OperatingSystem.NAME_MACOSX.equals(os.getName())) {
            return OperatingSystemType.OSX;
        }

        if (OperatingSystem.NAME_FREEBSD.equals(os.getName())) {
            return OperatingSystemType.BSD;
        }

        log.warn("Could not parse operating system name from " + os.getName() + ", returning Java platform");

        return OperatingSystemType.JAVA;
    }

    public String getOperatingSystemName() {
        OperatingSystem os = OperatingSystem.getInstance();
        // SIGAR returns "Win32" as the OS name for all Windows systems, even 64-bit ones. Work around this by instead
        // returning "Windows" for all Windows systems, which is more consistent with the UNIX operating systems anyway.
        // (https://jira.hyperic.com/browse/SIGAR-238)
        return (OperatingSystem.NAME_WIN32.equals(os.getName()) ? "Windows" : os.getName());
    }

    public String getOperatingSystemVersion() {
        return OperatingSystem.getInstance().getVersion();
    }

    public String getHostname() throws SystemInfoException {
        try {
            return sigar.getNetInfo().getHostName();
        } catch (Exception e) {
            // For some reason, the native layer failed to get the hostname.
            // Let's fallback and ask Java for help. But if that fails too,
            // let's wrap the native layer's exception, since we'll want to
            // see its cause, which will probably have a more descriptive error message.
            try {
                return InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException uhe) {
                throw new SystemInfoException(e);
            }
        }
    }

    public List<NetworkAdapterInfo> getAllNetworkAdapters() throws SystemInfoException {
        List<NetworkAdapterInfo> adapters = new ArrayList<NetworkAdapterInfo>();

        try {
            String[] interfaceNames = sigar.getNetInterfaceList();

            if (interfaceNames != null) {
                NetworkAdapterInfo.DisplayName displayName = NetworkAdapterInfo.DisplayName.FROM_NAME;
                // If Sigar can't get the adapter name it just starts naming the network adapters as eth0, eth1, etc.
                // This can be confusing on Windows, so we switch to their description
                // All references should be the same, as we are are only changing the displayName
                if (this.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
                    displayName = NetworkAdapterInfo.DisplayName.FROM_DESCRIPTION;
                }
                for (String interfaceName : interfaceNames) {
                    if (interfaceName.indexOf(':') != -1) {
                        continue; //filter out virtual IPs
                    }

                    adapters.add(new NetworkAdapterInfo(sigar.getNetInterfaceConfig(interfaceName), displayName));
                }
            }
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }

        return adapters;
    }

    public NetworkAdapterStats getNetworkAdapterStats(String interfaceName) {
        try {
            NetInterfaceStat interfaceStat = sigar.getNetInterfaceStat(interfaceName);
            return new NetworkAdapterStats(interfaceStat);
        } catch (SigarException e) {
            throw new SystemInfoException(e);
        }
    }

    public NetworkStats getNetworkStats(String addressName, int port) {
        List<NetConnection> matches = getNetworkConnections(addressName, port);
        NetworkStats stats = new NetworkStats(matches.toArray(new NetConnection[matches.size()]));
        return stats;
    }

    public List<NetConnection> getNetworkConnections(String addressName, int port) {
        try {
            int flags = NetFlags.CONN_SERVER | NetFlags.CONN_CLIENT | NetFlags.CONN_TCP;
            NetConnection[] conns = sigar.getNetConnectionList(flags);

            InetAddress matchAddress = (addressName != null) ? InetAddress.getByName(addressName) : null;

            List<NetConnection> list = new ArrayList<NetConnection>();
            for (NetConnection conn : conns) {
                if (port > 0 && (conn.getLocalPort() != port)) {
                    continue; // does not match the port we are looking for
                }
                if (matchAddress != null && !matchAddress.equals(InetAddress.getByName(conn.getLocalAddress()))) {
                    continue; // does not match the address we are looking for
                }
                list.add(conn); // matches our criteria, add it to the list to be returned to the caller
            }
            return list;
        } catch (SigarException e) {
            throw new SystemInfoException(e);
        } catch (UnknownHostException e) {
            throw new SystemInfoException(e);
        }
    }

    private List<InetAddress> getInetAddressInList(String address) throws UnknownHostException {
        List<InetAddress> inetAddresses = new ArrayList<InetAddress>();

        if (address != null) {
            inetAddresses.add(InetAddress.getByName(address));
        }

        return inetAddresses;
    }

    public List<ServiceInfo> getAllServices() throws SystemInfoException {
        throw new UnsupportedOperationException("Cannot get services for this platform");
    }

    public List<ProcessInfo> getAllProcesses() {
        ArrayList<ProcessInfo> processes = new ArrayList<ProcessInfo>();
        long[] pids = null;

        log.debug("Retrieving PIDs of all running processes...");
        long startTime = System.currentTimeMillis();
        try {
            pids = sigar.getProcList();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("Retrieval of " + pids.length + " PIDs took " + elapsedTime + " ms.");
            // NOTE: Do not close sigarImpl on success, as the ProcessInfos created below will reuse it.
        } catch (Exception e) {
            log.warn("Failed to retrieve PIDs of all running processes.", e);
        }

        if (pids != null) {
            for (long pid : pids) {
                if (log.isTraceEnabled()) {
                    log.trace("Loading process info for pid " + pid + "...");
                }
                ProcessInfo info = new ProcessInfo(pid, sigar);
                processes.add(info);
            }
        }

        return processes;
    }

    public List<ProcessInfo> getProcesses(String piq) {
        ProcessInfoQuery piql = new ProcessInfoQuery(getAllProcesses());
        return piql.query(piq);
    }

    public ProcessInfo getThisProcess() {
        long self = sigar.getPid();
        ProcessInfo info = new ProcessInfo(self);
        return info;
    }

    public ProcessExecutionResults executeProcess(ProcessExecution processExecution) {
        // TODO: doesn't look like SIGAR has an API to fork/execute processes? fallback to using the Java way
        return SystemInfoFactory.createJavaSystemInfo().executeProcess(processExecution);
    }

    public int getNumberOfCpus() {
        try {
            // NOTE: This will return the number of cores, not the number of sockets.
            return sigar.getCpuPercList().length;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get number of CPUs from native layer", e);
        }
    }

    public Mem getMemoryInfo() {
        try {
            return sigar.getMem();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get memory info from native layer", e);
        }
    }

    public Swap getSwapInfo() {
        try {
            // Removed this check since http://jira.hyperic.com/browse/SIGAR-112 is fixed.
            /*
            int enabledCpuCount = sigar.getCpuPercList().length;
            int totalCpuCount = sigar.getCpuInfoList().length;
            if (enabledCpuCount < totalCpuCount) {
                log.info("Aborting swap info collection because one or more CPUs is disabled - " + enabledCpuCount
                    + " out of " + totalCpuCount + " CPUs are enabled.");
                return null;
            }
            */
            return sigar.getSwap();
        } catch (Exception e) {
            throw new UnsupportedOperationException("Cannot get swap info from native layer", e);
        }
    }

    public String readLineFromConsole(boolean noEcho) throws IOException {
        String input;

        if (noEcho) {
            input = Sigar.getPassword("");
        } else {
            input = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }

        return input;
    }

    public void writeLineToConsole(String line) throws IOException {
        System.out.print(line); // note: don't use println - let the caller append newline char to 'line' if needed
    }

    public CpuInformation getCpu(int cpuIndex) {
        return new CpuInformation(cpuIndex, sigar);
    }

    private List<FileSystemInfo> getFileSystems(boolean deferredUsageInfo) {
        List<String> mountPoints = new ArrayList<String>();

        try {
            FileSystemMap map = sigar.getFileSystemMap();
            mountPoints.addAll(map.keySet());
        } catch (Exception e) {
            log.warn("Cannot obtain native file system information", e); // ignore native error otherwise
        }

        List<FileSystemInfo> infos = new ArrayList<FileSystemInfo>();
        for (String mountPoint : mountPoints) {
            infos.add(new FileSystemInfo(mountPoint, deferredUsageInfo));
        }

        return infos;
    }

    public List<FileSystemInfo> getFileSystems() {
        return getFileSystems(false);
    }

    public List<FileSystemInfo> getFileSystemsDeferredUsageInfo() {
        return getFileSystems(true);
    }

    public FileSystemInfo getFileSystem(String path) {
        String mountPoint = null;

        try {
            FileSystem mountPointForPath = sigar.getFileSystemMap().getMountPoint(path);
            if (mountPointForPath != null)
                mountPoint = mountPointForPath.getDirName();
        } catch (Throwable e) {
            log.warn("Cannot obtain native file system information for [" + path + "]", e); // ignore native error otherwise
        }

        FileSystemInfo fileSystem = new FileSystemInfo(mountPoint);
        return fileSystem;
    }

    @Override
    public DirUsage getDirectoryUsage(String path) {
        DirUsage dirUsage = null;
        try {
            dirUsage = sigar.getDirUsage(path);
        } catch (SigarException e) {
            log.warn("Can not get directory usage for [" + path + "] cause: " + e.getMessage());
            return null;
        }
        return dirUsage;
    }

    public String getSystemArchitecture() {
        OperatingSystem op = OperatingSystem.getInstance();
        return op.getArch();
    }

    /**
     * Constructor for {@link NativeSystemInfo} with package scope so only the {@link SystemInfoFactory} can instantiate
     * this object.
     */
    public NativeSystemInfo() {
        this.sigar = SigarAccess.getSigar();
    }

}