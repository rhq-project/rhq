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

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.SigarPermissionDeniedException;
import org.hyperic.sigar.SigarProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encapsulates information about a known process.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class ProcessInfo {
    private final Log log = LogFactory.getLog(ProcessInfo.class.getName());

    protected boolean initialized;

    // these are static - values remain for the life of this object
    protected long pid;
    protected String name;
    protected String baseName;
    protected String[] commandLine;
    protected Map<String, String> environmentVariables;
    protected SigarProxy sigar;

    // these are refreshed and may change during the life of the process
    protected ProcState procState;
    protected ProcExe procExe;
    protected ProcTime procTime;
    protected ProcMem procMem;
    protected ProcCpu procCpu;
    protected ProcFd procFd;
    protected ProcCred procCred;
    protected ProcCredName procCredName;
    protected Map<String, String> procEnv;

    private boolean loggedPermissionsError = false;
    private static final String UNKNOWN = "?";

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

    public void refresh() throws SystemInfoException {
        update(this.pid);
    }

    private void update(long pid) throws SystemInfoException {
        long startTime = System.currentTimeMillis();
        try {
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

                this.name = (procExe != null) ? procExe.getName() : UNKNOWN;
                this.baseName = determineBaseName(procExe, procState);
                this.commandLine = (procArgs != null) ? procArgs : new String[0];

                this.procEnv = null;
                try {
                    this.procEnv = sigar.getProcEnv(pid);
                    if (this.procEnv == null) {
                        log.debug("SIGAR returned a null environment for [" + this.baseName + "] process with pid ["
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
        if (log.isDebugEnabled()) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("Retrieval of process info for pid " + pid + " took " + elapsedTime + " ms." );
        }
    }

    public void destroy() throws SystemInfoException {
        if (this.sigar instanceof Sigar) {
            try
            {
                ((Sigar)this.sigar).close();
            }
            catch (RuntimeException e)
            {
                throw new SystemInfoException(e);
            }
        }
    }

    private void handleSigarCallException(Exception e, String methodName) {
        if (isWindows() && (this.pid == 0 || this.pid == 4))
            // On Windows, Pid 0 and Pid 4 are special Kernel processes (Pid 0 is the "System Idle Process" and Pid 4 is
            // the "System" process). For these processes, it's normal for many of the Sigar.getProc calls to fail, so
            // there's no need to log anything.
            return;
        String procName = (this.baseName != null) ? this.baseName : "<unknown>";
        if (e instanceof SigarPermissionDeniedException) {
            if (!this.loggedPermissionsError) {
                // Only log permissions errors once per process.
                String currentUserName = System.getProperty("user.name");
                log
                    .trace("Unable to obtain all info for [" + procName + "] process with pid [" + this.pid
                            + "] - call to " + methodName + "failed. "
                            + "The process is most likely owned by a user other than the user that owns the RHQ plugin container's process ("
                            + currentUserName + ").");
                this.loggedPermissionsError = true;
            }
        } else if (e instanceof SigarNotImplementedException) {
            log.trace("Unable to obtain all info for [" + procName + "] process with pid [" + this.pid + "] - call to "
                    + methodName + "failed. Cause: " + e);
        } else {
            log.debug("Unexpected error occurred while looking up info for [" + procName + "] process with pid ["
                    + this.pid + "] - call to " + methodName + " failed. Did the process die? Cause: " + e);
        }
    }

    private static boolean isWindows()
    {
        return File.separatorChar == '\\';
    }

    private String determineBaseName(ProcExe exe, ProcState state) {
        String base = null;

        if (exe != null) {
            base = exe.getName();

            if (base != null) {
                int slash = Math.max(base.lastIndexOf('\\'), base.lastIndexOf('/'));
                if ((slash > -1) && ((slash + 1) < base.length())) {
                    base = base.substring(slash + 1);
                }
            }
        }

        if (base == null) {
            if ((state != null) && (state.getName() != null)) {
                base = state.getName();
            }
        }

        return (base != null) ? base : UNKNOWN;
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
            this.environmentVariables = new HashMap(this.procEnv.size());
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

    public long getParentPid() throws SystemInfoException {
        return (this.procState != null) ? this.procState.getPpid() : 0L;
    }

    public ProcState getState() throws SystemInfoException {
        return this.procState;
    }

    public ProcExe getExecutable() throws SystemInfoException {
        return this.procExe;
    }

    public ProcTime getTime() throws SystemInfoException {
        return this.procTime;
    }

    public ProcMem getMemory() throws SystemInfoException {
        return this.procMem;
    }

    public ProcCpu getCpu() throws SystemInfoException {
        return this.procCpu;
    }

    public ProcFd getFileDescriptor() throws SystemInfoException {
        return this.procFd;
    }

    public ProcCred getCredentials() throws SystemInfoException {
        return this.procCred;
    }

    public ProcCredName getCredentialsName() throws SystemInfoException {
        return this.procCredName;
    }

    public String getCurrentWorkingDirectory() throws SystemInfoException {
        return this.procExe.getCwd();
    }

    public boolean isRunning() throws SystemInfoException {
        boolean running = false;

        if (this.procState != null) {
            running = (
                    this.procState.getState() == ProcState.RUN
                    || this.procState.getState() == ProcState.SLEEP
                    || this.procState.getState() == ProcState.IDLE);

        }

        return running;
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
        s.append((!getName().equals(UNKNOWN)) ? getName() : getBaseName());
        s.append("], ppid=[");
        try {
            s.append(getParentPid());
        } catch (Exception e) {
            s.append(e);
        }

        s.append("]");

        return s.toString();
    }
}