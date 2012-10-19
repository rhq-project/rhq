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
package org.rhq.plugins.apache.util;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.system.SigarAccess;

/**
 * Utility class to obtain the executable path of a process with the help of Sigar.
 * @author Ian Springer
 */
public abstract class OsProcessUtility {

    private static final String[] NO_ARGS = new String[0];
    private static final List<String> NO_MODULES = Collections.emptyList();

    /**
     * Attempt to find the executable path of the process with the specified pid.
     * If at all possible, an absolute path will be returned. Otherwise, a relative path
     * will be returned. If the path cannot be determined, <code>null</code> is returned.
     * @param pid Process identifier
     * @param name Binary base name to match against
     * @return The process executable path.
     */
    @Nullable
    public static File getProcExe(long pid, String name) {
        SigarProxy sigar = SigarAccess.getSigar();
        File argv0;
            try {
                String exe = sigar.getProcExe(pid).getName();
                // may be "" on Solaris
                if (exe.length() > 0) {
                    return new File(exe);
                }
            } catch (SigarException e) {
                // most likely permission denied
            }

            argv0 = null;
            String[] args = getProcArgs(pid, sigar);
            if (args.length != 0) {
                // might not be an absolute path
                argv0 = new File(args[0]);
                if (argv0.exists() && argv0.isAbsolute()) {
                    return argv0;
                }
            }

            List<String> modules = getProcModules(pid, sigar);
            if (modules.size() > 0) {
                if (name == null) {
                    return new File(modules.get(0));
                }
                name = File.separator + name;
                for (String module : modules) {
                    if (module.endsWith(name)) {
                        return new File(module);
                    }
                }
            }

        return argv0;
    }

    /**
     * Wrapper for Sigar.getProcArgs which catches SigarException
     * and returns an empty array if the SigarException
     * is caught.
     * @param pid Process identifier
     * @param sigar The Sigar instance to use
     * @return Arguments that were passed to the process.
     */
    @NotNull
    private static String[] getProcArgs(long pid, SigarProxy sigar) {
        try {
            return sigar.getProcArgs(pid);
        } catch (SigarException e) {
            return NO_ARGS;
        }
    }

    /**
     * Wrapper for Sigar.getProcModules which catches SigarException
     * and returns an empty list if the SigarException
     * is caught.
     * @param pid Process identifier
     * @param sigar The Sigar instance to use
     */
    @NotNull
    private static List<String> getProcModules(long pid, SigarProxy sigar) {
        try {
            return sigar.getProcModules(pid);
        } catch (SigarException e) {
            return NO_MODULES;
        }
    }

    private OsProcessUtility() {
    }
}
