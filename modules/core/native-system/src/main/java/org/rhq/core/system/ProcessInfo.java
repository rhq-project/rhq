/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.SigarPermissionDeniedException;
import org.hyperic.sigar.SigarProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Encapsulates information about a known process.
 * </p>
 * <p>
 * A few process properties (i.e. PID, command line) will never change during the lifetime of the process and can be
 * read directly with this class accessors. Other process properties (i.e. state, cpu usage) will vary and their values
 * are grouped in {@link ProcessInfoSnapshot} instances.
 * </p>
 * <p>
 * Operations on static properties of the process must be implemented in the {@link ProcessInfo} type. Operations on
 * non static properties must be implemented in the {@link ProcessInfoSnapshot} type. The {@link ProcessInfoSnapshot}
 * subtype has been created to remind users of the class that they are working with cached data.
 * </p>
 * <p>For example, if you want to be sure a process is still alive, you should use this code:<br>
 * <code>processInfo.freshSnapshot().isRunning()</code>
 * </p>
 * <p>Rather than:<br>
 * <code>processInfo.priorSnapshot().isRunning()</code>
 * </p>
 *
 * @author John Mazzitelli
 * @author Ian Springer
 * @author Thomas Segismont
 */
public class ProcessInfo {

    /**
     * <p>
     * Exposes non static process properties and operations computed on them (like {@link #isRunning()} method).
     * Operations on non static process properties should all be implemented here.
     * </p>
     * <p>
     * New snapshots are created when {@link ProcessInfo#refresh()} or {@link ProcessInfo#freshSnapshot()} are
     * called.
     * </p>
     * <p>
     * Note the current implementation does not actually encapsulate these properties for backward compatibility
     * reasons (we have to keep the write access to {@link ProcessInfo} protected properties).
     * </p>
     */
    public final class ProcessInfoSnapshot {

        public long getParentPid() throws SystemInfoException {
            return (ProcessInfo.this.procState != null) ? ProcessInfo.this.procState.getPpid() : 0L;
        }

        public ProcState getState() throws SystemInfoException {
            return ProcessInfo.this.procState;
        }

        public ProcExe getExecutable() throws SystemInfoException {
            return ProcessInfo.this.procExe;
        }

        public ProcTime getTime() throws SystemInfoException {
            return ProcessInfo.this.procTime;
        }

        public ProcMem getMemory() throws SystemInfoException {
            return ProcessInfo.this.procMem;
        }

        public ProcCpu getCpu() throws SystemInfoException {
            return ProcessInfo.this.procCpu;
        }

        public ProcFd getFileDescriptor() throws SystemInfoException {
            return ProcessInfo.this.procFd;
        }

        public ProcCred getCredentials() throws SystemInfoException {
            return ProcessInfo.this.procCred;
        }

        public ProcCredName getCredentialsName() throws SystemInfoException {
            return ProcessInfo.this.procCredName;
        }

        /**
         * @return null if process executable or cwd is unavailable. Otherwise the Cwd as returned from the
         * process executable.
         * @throws SystemInfoException
         */
        public String getCurrentWorkingDirectory() throws SystemInfoException {
            String result = null;
            try {
                if (null != ProcessInfo.this.procExe) {
                    result = ProcessInfo.this.procExe.getCwd();
                }
            } catch (Exception e) {
                ProcessInfo.this.handleSigarCallException(e, "procExe.getCwd()");
            }
            return result;
        }

        /**
         * Checks if the process is alive.
         *
         * @return true if the process is running, sleeping or idle
         * @throws SystemInfoException
         */
        public boolean isRunning() throws SystemInfoException {
            boolean running = false;
            if (ProcessInfo.this.procState != null) {
                running = (ProcessInfo.this.procState.getState() == ProcState.RUN
                    || ProcessInfo.this.procState.getState() == ProcState.SLEEP || ProcessInfo.this.procState
                    .getState() == ProcState.IDLE);

            }
            return running;
        }

    }

    private static final Log LOG = LogFactory.getLog(ProcessInfo.class);

    private static final int REFRESH_LOCK_ACQUIRE_TIMEOUT_SECONDS = 5;

    private static final String UNKNOWN_PROCESS_NAME = "?";

    private static final Set<String> MS_WINDOWS_TERMINATE_SIGNAL_NAMES = new HashSet<String>();
    static {
        MS_WINDOWS_TERMINATE_SIGNAL_NAMES.add("INT");
        MS_WINDOWS_TERMINATE_SIGNAL_NAMES.add("KILL");
        MS_WINDOWS_TERMINATE_SIGNAL_NAMES.add("QUIT");
        MS_WINDOWS_TERMINATE_SIGNAL_NAMES.add("TERM");
    }

    protected boolean initialized;

    protected SigarProxy sigar;

    // these are static - values remain for the life of this object
    protected long pid;
    protected String name;
    protected String[] commandLine;
    protected Map<String, String> procEnv;

    // these are computed once with static data (purposely lazy in order to speed up discovery process)
    protected Map<String, String> environmentVariables;
    protected String baseName;

    // this one is computed once with non static data
    protected ProcessInfo parentProcess;

    // these are refreshed and may change during the life of the process

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcState procState;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcExe procExe;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcTime procTime;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcMem procMem;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcCpu procCpu;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcFd procFd;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcCred procCred;

    /**
     * @deprecated as of 4.6. To read this property call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    protected ProcCredName procCredName;

    // Set to true in handleSigarCallException
    protected boolean processDied;

    // Set to true if a SIGAR permission error has already been logged
    private boolean loggedPermissionsError = false;

    // The last snasphot of non static process properties
    // In a future implementation a new snapshot will be created on each call to refresh
    private ProcessInfoSnapshot snapshot = new ProcessInfoSnapshot();

    // A lock to serialize calls to refresh method
    private ReentrantLock refreshLock = new ReentrantLock();

    // useful for mocking this object, this is purposely not public
    protected ProcessInfo() {
    }

    public ProcessInfo(long pid) throws SystemInfoException {
        this(pid, SigarAccess.getSigar());
    }

    public ProcessInfo(long pid, SigarProxy sigar) throws SystemInfoException {
        this.pid = pid;
        this.sigar = sigar;
        update(pid);
    }

    /**
     * Takes a fresh snapshot of non static properties of the underlying process. This method internally serializes 
     * calls so that it maintains a consistent view of the various Sigar call results.
     *
     * @throws SystemInfoException
     */
    public void refresh() throws SystemInfoException {
        // Serializing is also important as in somes cases, the process could be reported up while being down.
        // See this thread on VMWare forum: http://communities.vmware.com/message/2187972#2187972
        boolean acquiredLock = false;
        try {
            acquiredLock = refreshLock.tryLock(REFRESH_LOCK_ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Thread interrupted while trying to acquire ProcessInfo[" + this.pid + "] refresh lock", e);
        }
        if (!acquiredLock) {
            throw new RuntimeException("Could not acquire ProcessInfo[" + this.pid + "] refresh lock");
        }
        try {
            // No need to update if the process has already been reported down, Sigar will only throw exceptions...
            if (priorSnaphot().isRunning()) {
                update(this.pid);
            }
        } finally {
            refreshLock.unlock();
        }
    }

    // Refresh and update methods cannot be merged because subclasses may override refresh behavior
    // and we can't be sure that instances will already be properly initialized.
    private void update(long pid) throws SystemInfoException {
        long startTime = System.currentTimeMillis();
        try {

            this.processDied = false;

            // Get ProcState and ProcExe before static data as they can help to determine the name field in some cases.

            ProcState procState = null;
            try {
                procState = sigar.getProcState(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcState");
            }

            ProcExe procExe = null;
            try {
                procExe = sigar.getProcExe(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcExe");
            }

            // If this is the first time we are refreshing this object, initialize the static data.
            // We only have to do this once, since this data never changes during the life of the process.
            if (!this.initialized) {
                String[] procArgs = null;
                try {
                    procArgs = sigar.getProcArgs(pid);
                } catch (Exception e) {
                    handleSigarCallException(e, "getProcArgs");
                }

                this.name = determineName(procArgs, procExe, procState);
                // NOTE: for the sake of efficiency, this.baseName is lazily initialized by its getter.
                this.commandLine = (procArgs != null) ? procArgs : new String[0];

                this.procEnv = null;
                try {
                    this.procEnv = sigar.getProcEnv(pid);
                    if (this.procEnv == null) {
                        LOG.debug("SIGAR returned a null environment for [" + getBaseName() + "] process with pid ["
                            + this.pid + "].");
                    }
                } catch (Exception e) {
                    handleSigarCallException(e, "getProcEnv");
                }

                this.initialized = true;
            }

            // now refresh the process data
            this.procState = procState;
            this.procExe = procExe;

            try {
                this.procTime = sigar.getProcTime(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcTime");
            }

            try {
                this.procMem = sigar.getProcMem(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcMem");
            }

            try {
                this.procCpu = sigar.getProcCpu(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcCpu");
            }

            try {
                this.procFd = sigar.getProcFd(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcFd");
            }

            try {
                this.procCred = sigar.getProcCred(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcCred");
            }

            try {
                this.procCredName = sigar.getProcCredName(pid);
            } catch (Exception e) {
                handleSigarCallException(e, "getProcCredName");
            }
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }
        if (LOG.isTraceEnabled()) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            LOG.trace("Retrieval of process info for pid " + pid + " took " + elapsedTime + " ms.");
        }

        this.processDied = false;
    }

    /**
     * <p>
     * Returns the last snasphot of the non static process properties.
     * </p>
     * <p>
     * Caveat the returned may hold stale data has it was taken with previous SIGAR calls.
     * Calling {@link #freshSnapshot()} instead is almost always a better idea.
     * </p>
     *
     * @return a {@link ProcessInfoSnapshot} possibly holding stale data
     */
    public ProcessInfoSnapshot priorSnaphot() {
        return snapshot;
    }

    /**
     * Takes a fresh snapshot of the non static process properties.
     *
     * @return a fresh {@link ProcessInfoSnapshot}
     */
    public ProcessInfoSnapshot freshSnapshot() {
        refresh();
        return snapshot;
    }

    public void destroy() throws SystemInfoException {
        if (this.sigar instanceof Sigar) {
            try {
                ((Sigar) this.sigar).close();
            } catch (RuntimeException e) {
                throw new SystemInfoException(e);
            }
        }
    }

    private void handleSigarCallException(Exception e, String methodName) {
        if (this.processDied) {
            // We already figured out the process died on a previous call to this method, so just return rather than
            // flooding the log with debug messages.
            return;
        }

        if (OperatingSystem.IS_WIN32 && (this.pid == 0 || this.pid == 4)) {
            // On Windows, Pid 0 and Pid 4 are special Kernel processes (Pid 0 is the "System Idle Process" and Pid 4 is
            // the "System" process). For these processes, it's normal for many of the Sigar.getProc calls to fail, so
            // there's no need to log anything.
            return;
        }

        String procName = (this.baseName != null) ? this.baseName : "<unknown>";
        if (e instanceof SigarPermissionDeniedException) {
            if (!this.loggedPermissionsError) {
                // Only log permissions errors once per process.
                String currentUserName = System.getProperty("user.name");
                LOG.trace("Unable to obtain all info for ["
                    + procName
                    + "] process with pid ["
                    + this.pid
                    + "] - call to "
                    + methodName
                    + "failed. "
                    + "The process is most likely owned by a user other than the user that owns the RHQ plugin container's process ("
                    + currentUserName + ").");
                this.loggedPermissionsError = true;
            }
        } else if (e instanceof SigarNotImplementedException) {
            LOG.trace("Unable to obtain all info for [" + procName + "] process with pid [" + this.pid + "] - call to "
                + methodName + "failed. Cause: " + e);
        } else {
            if (!exists()) {
                LOG.debug("Attempt to refresh info for process with pid [" + this.pid
                    + "] failed, because the process is no longer running.");
                this.processDied = true;
            }

            LOG.debug("Unexpected error occurred while looking up info for [" + procName + "] process with pid ["
                + this.pid + "] - call to " + methodName + " failed. Did the process die? Cause: " + e);
        }
    }

    private boolean exists() {
        long[] pids;
        try {
            pids = sigar.getProcList();
        } catch (SigarException e1) {
            // TODO (ips, 04/30/12): It probably makes more sense to let this exception bubble up.
            LOG.error("Failed to obtain process list.", e1);
            return true;
        }

        boolean foundProcess = false;
        for (long pid : pids) {
            if (pid == this.pid) {
                foundProcess = true;
                break;
            }
        }
        return foundProcess;
    }

    private String determineName(String[] procArgs, ProcExe procExe, ProcState procState) {
        String name;
        if ((procArgs != null) && (procArgs.length != 0)) {
            name = procArgs[0];
        } else if ((procExe != null) && (procExe.getName() != null)) {
            name = procExe.getName();
        } else if ((procState != null) && (procState.getName() != null)) {
            name = procState.getName();
        } else {
            name = UNKNOWN_PROCESS_NAME;
        }
        return name;
    }

    public long getPid() {
        return pid;
    }

    /**
     * Convenience method that returns the first command line argument, which is the name of the program that the
     * process is executing.
     *
     * @return full name of program that is executing
     *
     * @see    #getBaseName()
     * @see    #getCommandLine()
     */
    public String getName() {
        return name;
    }

    /**
     * Similar to {@link #getName()}, this is a convenience method that returns the first command line argument, which
     * is the name of the program that the process is executing. However, this is only the relative filename of the
     * program, which does not include the full path to the program (e.g. this would return "sh" if the name of the
     * process is "/usr/bin/sh").
     *
     * @return filename of program that is executing
     *
     * @see    #getName()
     * @see    #getCommandLine()
     */
    public String getBaseName() {
        if (baseName == null) {
            baseName = (getName() != null) ? new File(getName()).getName() : UNKNOWN_PROCESS_NAME;
        }
        return baseName;
    }

    public String[] getCommandLine() {
        return commandLine;
    }

    public Map<String, String> getEnvironmentVariables() {
        if (this.procEnv == null) {
            return Collections.emptyMap();
        }
        if (this.environmentVariables == null) {
            this.environmentVariables = new HashMap<String, String>(this.procEnv.size());
            SystemInfo systemInfo = SystemInfoFactory.createJavaSystemInfo();
            boolean isWindows = systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS;
            if (isWindows) {
                // Windows environment is case-insensitive so convert variable names to all-caps,
                // this way we will be able to do case-insensitive lookups from the map later
                for (Map.Entry<String, String> env : this.procEnv.entrySet()) {
                    this.environmentVariables.put(env.getKey().toUpperCase(), env.getValue());
                }
            } else {
                this.environmentVariables.putAll(procEnv);
            }
        }
        return this.environmentVariables;
    }

    /**
     * Retrieves a specific environment property if it exists, <code>null</code> otherwise.
     *
     * @param  name the name of the property to find
     *
     * @return the environment value
     */
    @Nullable
    public String getEnvironmentVariable(@NotNull
    String name) {
        if (this.procEnv == null) {
            return null;
        }
        SystemInfo systemInfo = SystemInfoFactory.createJavaSystemInfo();
        boolean isWindows = systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS;
        // Windows env names are case insensitive, so convert the specified name to all-caps before doing the lookup.
        return getEnvironmentVariables().get((isWindows) ? name.toUpperCase() : name);
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public long getParentPid() throws SystemInfoException {
        return priorSnaphot().getParentPid();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcState getState() throws SystemInfoException {
        return priorSnaphot().getState();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcExe getExecutable() throws SystemInfoException {
        return priorSnaphot().getExecutable();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcTime getTime() throws SystemInfoException {
        return priorSnaphot().getTime();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcMem getMemory() throws SystemInfoException {
        return priorSnaphot().getMemory();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcCpu getCpu() throws SystemInfoException {
        return priorSnaphot().getCpu();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcFd getFileDescriptor() throws SystemInfoException {
        return priorSnaphot().getFileDescriptor();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcCred getCredentials() throws SystemInfoException {
        return priorSnaphot().getCredentials();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public ProcCredName getCredentialsName() throws SystemInfoException {
        return priorSnaphot().getCredentialsName();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public String getCurrentWorkingDirectory() throws SystemInfoException {
        return priorSnaphot().getCurrentWorkingDirectory();
    }

    /**
     * @deprecated as of 4.6. For similar purpose, call {@link #priorSnaphot()} and then corresponding method
     * from the returned {@link ProcessInfoSnapshot}.
     */
    @Deprecated
    public boolean isRunning() throws SystemInfoException {
        return priorSnaphot().isRunning();
    }

    /**
     * Returns an object that provides aggregate information on this process and all its children.
     *
     * @return aggregate information on this process and its children
     */
    public AggregateProcessInfo getAggregateProcessTree() {
        AggregateProcessInfo root = new AggregateProcessInfo(this.pid);
        return root;
    }

    /**
     * Returns a {@link ProcessInfo} instance for the parent of this process.
     *
     * This method uses the parent process id which is not static (it can change if the parent process dies before its
     * child). So in theory it should be moved to the {@link ProcessInfoSnapshot} type.
     *
     * In practice, it stays here because the parent {@link ProcessInfo} instance is cached after creation.
     *
     * @since 4.4
     */
    public ProcessInfo getParentProcess() throws SystemInfoException {
        if (this.parentProcess == null) {
            this.parentProcess = new ProcessInfo(priorSnaphot().getParentPid(), sigar);
        } else {
            this.parentProcess.refresh();
        }
        return this.parentProcess;
    }

    /**
     * Send the signal with the specified name to this process.
     *
     * @param signalName the name of the signal to send
     *
     * @throws IllegalArgumentException if the signal name is not valid
     * @throws SigarException if the native kill() call fails
     *
     * @since 4.4
     */
    public void kill(String signalName) throws SigarException {
        int signalNumber = getSignalNumber(signalName);
        // TODO: Should we check if the process is even running and throw a special exception if it's not?
        Sigar fullSigar = new Sigar();
        try {
            fullSigar.kill(pid, signalNumber);
        } finally {
            fullSigar.close();
        }
    }

    /**
     * A process' pid makes it unique - this returns the {@link #getPid()} itself.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Long.valueOf(this.pid).intValue();
    }

    /**
     * Two {@link ProcessInfo} objects are equal if their {@link #getPid() pids} are the same.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof ProcessInfo)) {
            return false;
        }

        return this.pid == ((ProcessInfo) obj).pid;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("process: ");
        s.append("pid=[");
        s.append(getPid());
        s.append("], name=[");
        s.append((!getName().equals(UNKNOWN_PROCESS_NAME)) ? getName() : getBaseName());
        s.append("], ppid=[");
        try {
            s.append(priorSnaphot().getParentPid());
        } catch (Exception e) {
            s.append(e);
        }
        s.append("]");
        return s.toString();
    }

    private static int getSignalNumber(String signalName) {
        if (signalName == null) {
            throw new IllegalArgumentException("Signal name is null.");
        }
        int signalNumber;
        if (OperatingSystem.IS_WIN32) {
            if (MS_WINDOWS_TERMINATE_SIGNAL_NAMES.contains(signalName)) {
                signalNumber = 1;
            } else {
                throw new IllegalArgumentException("Unsupported signal name: " + signalName
                    + " - on Windows, the only supported signal names are " + MS_WINDOWS_TERMINATE_SIGNAL_NAMES
                    + ", all of which return 1.");
            }
        } else {
            signalNumber = Sigar.getSigNum(signalName);
            if (signalNumber == -1) {
                throw new IllegalArgumentException("Unknown signal name: " + signalName);
            }
        }
        return signalNumber;
    }

}