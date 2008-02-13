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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import mazz.i18n.Msg;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.agent.AgentMain;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.util.StreamUtil;

/**
 * Downloads a file from a remote server (either the RHQ Server or a remote URL).
 *
 * @author John Mazzitelli
 */
public class DownloadPromptCommand implements AgentPromptCommand {
    private static final Msg MSG = AgentI18NFactory.getMsg();

    /**
     * @see AgentPromptCommand#getPromptCommandString()
     */
    public String getPromptCommandString() {
        return MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD);
    }

    /**
     * @see AgentPromptCommand#execute(AgentMain, String[])
     */
    public boolean execute(AgentMain agent, String[] args) {
        PrintWriter out = agent.getOut();
        String file_to_download = null;

        try {
            File storage_dir;

            if (args.length == 2) {
                storage_dir = agent.getConfiguration().getDataDirectory();
            } else if (args.length == 3) {
                storage_dir = new File(args[2]);
            } else {
                out.println(MSG.getMsg(AgentI18NResourceKeys.HELP_SYNTAX_LABEL, getSyntax()));
                return true;
            }

            file_to_download = args[1];

            if (file_to_download.indexOf(':') == -1) {
                // there is no protocol:, assume the user wants to get the file from the server
                downloadFromServer(agent, out, file_to_download, storage_dir);
            } else {
                downloadFromURL(out, new URL(file_to_download), storage_dir);
            }
        } catch (Exception e) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_ERROR, file_to_download, ThrowableUtil
                .getAllMessages(e)));
        }

        return true;
    }

    /**
     * Downloads a file from a remote URL endpoint. This is very basic and does not attempt to do anything special with
     * authentication or security. This is just to support simple downloading of content as an anonymous client.
     *
     * @param  out
     * @param  url
     * @param  storage_dir
     *
     * @throws IOException
     */
    private void downloadFromURL(PrintWriter out, URL url, File storage_dir) throws IOException {
        String file_to_download = url.getFile();
        InputStream in = url.openStream();
        downloadStream(out, storage_dir, file_to_download, in);
    }

    /**
     * Downloads a file from the RHQ Server.
     *
     * @param  agent
     * @param  out
     * @param  file_to_download
     * @param  storage_dir
     *
     * @throws FileNotFoundException
     */
    private void downloadFromServer(AgentMain agent, PrintWriter out, String file_to_download, File storage_dir)
        throws FileNotFoundException {
        // make sure our agent is currently in communications with the server
        ClientCommandSender sender = agent.getClientCommandSender();
        if (sender == null) {
            out.println(MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_ERROR_NOT_SENDING, file_to_download));
        } else {
            // now let's ask the server for the file's stream
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            CoreServerService server = factory.getRemotePojo(CoreServerService.class);
            InputStream in = server.getFileContents(file_to_download);
            downloadStream(out, storage_dir, file_to_download, in);
        }
    }

    /**
     * @param  out
     * @param  storage_dir      parent directory where to store the file being downloaded
     * @param  file_to_download the relative path of the file that will be written with the downloaded contents
     * @param  in               stream containing the content to download
     *
     * @throws FileNotFoundException
     */
    private void downloadStream(PrintWriter out, File storage_dir, String file_to_download, InputStream in)
        throws FileNotFoundException {
        FileOutputStream storage_file_stream = null;

        try {
            // prepare our local storage area and make sure we can write to it
            if ((file_to_download == null) || (file_to_download.length() == 0)) {
                file_to_download = "agent-download.txt";
            }

            File storage_file = new File(storage_dir, file_to_download);
            storage_file.getParentFile().mkdirs();
            storage_file_stream = new FileOutputStream(storage_file, false);

            out.println(MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_INPROGRESS, file_to_download));

            // now let's ask the URL for the file's contents and store it locally
            StreamUtil.copy(in, storage_file_stream, true);

            // we are OK now and everything is closed, null these out so our finally doesn't try to close them again
            in = null;
            storage_file_stream = null;

            out.println(MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_SUCCESS, storage_file));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }

            if (storage_file_stream != null) {
                try {
                    storage_file_stream.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * @see AgentPromptCommand#getSyntax()
     */
    public String getSyntax() {
        return MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_SYNTAX);
    }

    /**
     * @see AgentPromptCommand#getHelp()
     */
    public String getHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_HELP);
    }

    /**
     * @see AgentPromptCommand#getDetailedHelp()
     */
    public String getDetailedHelp() {
        return MSG.getMsg(AgentI18NResourceKeys.DOWNLOAD_DETAILED_HELP);
    }
}