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
package org.rhq.enterprise.agent.promptcmd;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

import mazz.i18n.Msg;

import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;

/**
 * Allows you to peek into the agent's data directory. This also provides
 * a way to delete files in the data directory and its subdirectories, allowing
 * you to clean up temporary or unused data files that may be bloating the agent.
 *
 * @author John Mazzitelli
 */
public class ListDataPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.LISTDATA);
    }

    public boolean execute(AgentMain agent, String[] args) {
        // strip the first argument, which is the name of our prompt command
        String[] realArgs = new String[args.length - 1];
        System.arraycopy(args, 1, realArgs, 0, args.length - 1);

        processArguments(agent, realArgs);
        return true;
    }

    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.LISTDATA_SYNTAX);
    }

    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.LISTDATA_HELP);
    }

    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DETAILED_HELP);
    }

    private void processArguments(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();

        String sopts = "-dvr";
        LongOpt[] lopts = { new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
            new LongOpt("recurse", LongOpt.NO_ARGUMENT, null, 'r'),
            new LongOpt("delete", LongOpt.NO_ARGUMENT, null, 'd') };

        Getopt getopt = new Getopt(getPromptCommandString(), args, sopts, lopts);
        int code;
        boolean verbose = false;
        boolean recurse = false;
        boolean delete = false;
        String pathname = null;

        while ((pathname == null) && (code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                break;
            }

            case 1: {
                // we found the path name - stop processing arguments
                pathname = getopt.getOptarg();
                break;
            }

            case 'v': {
                verbose = true;
                break;
            }

            case 'r': {
                recurse = true;
                break;
            }

            case 'd': {
                delete = true;
                break;
            }
            }
        }

        if (getopt.getOptind() < args.length || pathname == null) {
            // we got too many arguments on the command line
            out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
            return;
        }

        // sneaky sneaky - do not allow the user to attempt to go up to parent directories
        if (pathname.indexOf("..") != -1) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DOTDOT_NOT_ALLOWED));
            return;
        }

        // sneaky sneaky - do not allow the user to attempt to go to any absolute path
        if (new File(pathname).isAbsolute()) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_ABSOLUTE_NOT_ALLOWED));
            return;
        }

        String dataDir = agent.getConfiguration().getDataDirectory().getAbsolutePath();

        if (verbose) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DATA_DIR, dataDir));
        }

        if ("bundles".equals(pathname)) {
            pathname = "tmp/bundle-versions";
        }

        list(out, dataDir, new File(dataDir, pathname), verbose, recurse, delete);

        return;
    }

    private void list(PrintWriter out, String dataDir, File file, boolean verbose, boolean recurse, boolean delete) {
        String relativePath = getRelativePath(dataDir, file);

        if (file.isFile()) {
            printFileInfo(out, dataDir, file, verbose);
        } else if (file.isDirectory()) {
            printDirInfo(out, dataDir, file, verbose);
            File[] dirFiles = file.listFiles();
            if (recurse) {
                for (File dirFile : dirFiles) {
                    list(out, dataDir, dirFile, verbose, recurse, delete);
                }
            }
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_FILE_NOT_FOUND, relativePath));
            return;

        }

        if (delete) {
            boolean ok = file.delete();
            if (ok) {
                out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DELETED, relativePath));
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DELETED_FAILED, relativePath));
            }
        }
    }

    private void printFileInfo(PrintWriter out, String dataDir, File file, boolean verbose) {
        String name = getRelativePath(dataDir, file);
        if (verbose) {
            long filesize = file.length();
            Date lastModified = new Date(file.lastModified());
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_FILE_INFO_VERBOSE, name, lastModified, filesize));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_FILE_INFO, name));
        }
    }

    private void printDirInfo(PrintWriter out, String dataDir, File dir, boolean verbose) {
        String name = getRelativePath(dataDir, dir);
        if (verbose) {
            int filecount = dir.listFiles().length;
            Date lastModified = new Date(dir.lastModified());
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DIR_INFO_VERBOSE, name, lastModified, filecount));
        } else {
            out.println(MSG.getMsg(AgentI18NResourceKeys.LISTDATA_DIR_INFO, name));
        }
    }

    private String getRelativePath(String dataDir, File file) {
        try {
            return file.getAbsolutePath().substring(dataDir.length());
        } catch (Exception e) {
            // something weird happened, just show the path
            return file.getPath();
        }
    }
}
