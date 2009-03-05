/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.client;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import jline.*;
import mazz.i18n.Msg;
import org.rhq.enterprise.client.commands.ClientCommand;
import org.rhq.core.domain.auth.Subject;

import java.io.*;
import java.util.*;

/**
 * @author Greg Hinkle
 */
public class ClientMain {

    private static final Msg MSG = ClientI18NFactory.getMsg();

    private static Map<String, ClientCommand> commands = new HashMap<String, ClientCommand>();

    /**
     * This is the thread that is running the input loop; it accepts prompt commands from the user.
     */
    private Thread inputLoopThread;

    private BufferedReader inputReader;

    private ConsoleReader consoleReader;

    private boolean stdinInput = true;


    private PrintWriter outputWriter;

    private String host;
    private int port;
    private String user;
    private String pass;
    private RHQRemoteClient remoteClient;

    private Subject subject;

    public static void main(String[] args) throws Exception {

        /*Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                new UnixTerminal().restoreTerminal();     }
                catch (Exception e) { }
            }
        }));*/
        try {
//            new UnixTerminal().initializeTerminal();

            ClientMain main = new ClientMain();
            main.processArguments(args);
            main.inputLoop();
        } finally {
            //new UnixTerminal().restoreTerminal();
        }
    }

    public ClientMain() throws Exception {

//        this.inputReader = new BufferedReader(new InputStreamReader(System.in));
        this.outputWriter = new PrintWriter(System.out, true);

        consoleReader = new jline.ConsoleReader();
        consoleReader.addCompletor(
                new SimpleCompletor(commands.keySet().toArray(new String[commands.size()])));
        consoleReader.addCompletor(
                new ArgumentCompletor(
                        new Completor[]{
                                new SimpleCompletor("help"),
                                new SimpleCompletor(commands.keySet().toArray(new String[commands.size()]))}));

        consoleReader.setUsePagination(true);

    }

    public void start() {
        outputWriter = new PrintWriter(System.out);
//        inputReader = new BufferedReader(new InputStreamReader(System.in));

    }


    public String getUserInput(String prompt) {

        String input_string = "";
        boolean use_default_prompt = (prompt == null);

        while ((input_string != null) && (input_string.trim().length() == 0)) {
            if (prompt == null) {
                if (!loggedIn()) {
                    prompt = "unconnected> ";
                } else {

                    prompt = host + ":" + port + "> ";
                }
            }
//            outputWriter.print(prompt);

            try {
                outputWriter.flush();
                input_string = consoleReader.readLine(prompt);
                //inputReader.readLine();
            } catch (Exception e) {
                input_string = null;
            }
        }

        if (input_string != null) {
            // if we are processing a script, show the input that was just read in
            if (!stdinInput) {
                outputWriter.println(input_string);
            }
        } else if (!stdinInput) {
            // if we are processing a script, we hit the EOF, so close the input stream
            try {
                inputReader.close();
            } catch (IOException e1) {
            }

            // if we are not in daemon mode, let's now start processing prompt commands coming in via stdin
//            if (!m_daemonMode) {
//                inputReader = new BufferedReader(new InputStreamReader(System.in));
//                stdinInput = true;
//                input_string = "";
//            } else {
//                inputReader = null;
//            }
        }

        return input_string;
    }

    public boolean loggedIn() {
        return this.subject != null
                && this.getRemoteClient() != null
                && this.getRemoteClient().isConnected()
                && this.getRemoteClient().getSubjectManager().isValidSessionId(subject.getSessionId(), subject.getName());
    }

    /**
     * This enters in an infinite loop. Because this never returns, the current thread never dies and hence the agent
     * stays up and running. The user can enter agent commands at the prompt - the commands are sent to the agent as if
     * the user is a remote client.
     */
    private void inputLoop() {
        // we need to start a new thread and run our loop in it; otherwise, our shutdown hook doesn't work
        Runnable loop_runnable = new Runnable() {
            public void run() {
                while (true) {
                    // get a command from the user
                    // if in daemon mode, only get input if reading from an input file; ignore stdin
                    String cmd;
//                        if ((m_daemonMode == false) || (stdinInput == false)) {
                    cmd = getUserInput(null);
//                        } else {
//                            cmd = null;
//                        }

                    try {
                        // parse the command into separate arguments and execute it
                        String[] cmd_args = parseCommandLine(cmd);
                        boolean can_continue = executePromptCommand(cmd_args);

                        // break the input loop if the prompt command told us to exit
                        // if we are not in daemon mode, this really will end up killing the agent
                        if (!can_continue) {
                            break;
                        }
                    } catch (Throwable t) {
//                        outputWriter.println(ThrowableUtil.getAllMessages(t));
                        t.printStackTrace(outputWriter);
                        //LOG.debug(t, AgentI18NResourceKeys.COMMAND_FAILURE_STACK_TRACE);
                    }
                }

                return;
            }
        };

        // start the thread
        inputLoopThread = new Thread(loop_runnable);
        inputLoopThread.setName("RHQ Client Prompt Input Thread");
        inputLoopThread.setDaemon(false);
        inputLoopThread.start();

        return;
    }


    private boolean executePromptCommand(String[] args) throws Exception {
        String cmd = args[0];
        if (commands.containsKey(cmd)) {
            ClientCommand command = commands.get(cmd);
            try {
                return command.execute(this, args);
            } catch (ArrayIndexOutOfBoundsException e) {
                outputWriter.println("Unexpected syntax: " + args);
                outputWriter.println("   [" + args[0] + " syntax: " + command.getPromptCommandString());
            }
        } else {
            return commands.get("exec").execute(this, args);
            //outputWriter.println("Unknown command [" + cmd + "]");
        }
        return true;
    }


    /**
     * Given a command line, this will parse each argument and return the argument array.
     *
     * @param cmdLine the command line
     * @return the array of command line arguments
     */
    private String[] parseCommandLine(String cmdLine) {
        ByteArrayInputStream in = new ByteArrayInputStream(cmdLine.getBytes());
        StreamTokenizer strtok = new StreamTokenizer(new InputStreamReader(in));
        List<String> args = new ArrayList<String>();
        boolean keep_going = true;

        // we don't want to parse numbers and we want ' to be a normal word character
        strtok.ordinaryChars('0', '9');
        strtok.ordinaryChar('.');
        strtok.ordinaryChar('-');
        strtok.ordinaryChar('\'');
        strtok.wordChars(33, 127);
        strtok.quoteChar('\"');

        // parse the command line
        while (keep_going) {
            int nextToken;

            try {
                nextToken = strtok.nextToken();
            } catch (IOException e) {
                nextToken = StreamTokenizer.TT_EOF;
            }

            if (nextToken == java.io.StreamTokenizer.TT_WORD) {
                args.add(strtok.sval);
            } else if (nextToken == '\"') {
                args.add(strtok.sval);
            } else if ((nextToken == java.io.StreamTokenizer.TT_EOF) || (nextToken == java.io.StreamTokenizer.TT_EOL)) {
                keep_going = false;
            }
        }

        return args.toArray(new String[args.size()]);
    }


    private void displayUsage() {
        outputWriter.println("rhq-client.sh [-h] [-u user] [-p pass] [-s host] [-t port] [-f file]");
    }


    void processArguments(String[] args) throws IllegalArgumentException {

        String sopts = "-:hu:p:s:t:f:";
        LongOpt[] lopts = {
                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
                new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
                new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
                new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 's'),
                new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 't'),
                new LongOpt("file", LongOpt.NO_ARGUMENT, null, 'f')
        };

        String config_file_name = null;
        boolean clean_config = false;
        boolean purge_data = false;

        Getopt getopt = new Getopt("agent", args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
                case ':':
                case '?': {
                    // for now both of these should exit
                    displayUsage();
                    throw new IllegalArgumentException("mm");//MSG.getMsg(ClientI18NResourceKeys.BAD_ARGS,null));
                }

                case 1: {
                    // this will catch non-option arguments (which we don't currently care about)
                    System.err.println(MSG.getMsg(ClientI18NResourceKeys.USAGE, getopt.getOptarg()));
                    break;
                }

                case 'h': {
                    displayUsage();
                }

                case 'u': {
                    this.user = getopt.getOptarg();
                }
                case 'p': {
                    this.pass = getopt.getOptarg();
                }


                case 'i': {
                    File script = new File(getopt.getOptarg());

                    try {
                        inputReader = new BufferedReader(new FileReader(script));
                        stdinInput = false;
                    } catch (Exception e) {
                        //throw new IllegalArgumentException(MSG.getMsg(ClientI18NResourceKeys.BAD_INPUT_FILE, script, e));
                    }

                    break;
                }
            }
        }
        if (user != null && pass != null) {
            commands.get("login").execute(this, new String[]{"login", user, pass});
        }
    }

    public RHQRemoteClient getRemoteClient() {
        return remoteClient;
    }

    public void setRemoteClient(RHQRemoteClient remoteClient) {
        this.remoteClient = remoteClient;
        if (remoteClient != null) {
            consoleReader.addCompletor(
                    new ArgumentCompletor(
                            new Completor[]{
                                    new SimpleCompletor("help"),
                                    new SimpleCompletor("api"),
                                    new SimpleCompletor(this.getRemoteClient().getAllServices().keySet().toArray(new String[this.getRemoteClient().getAllServices().size()]))}));

            consoleReader.addCompletor(new ServiceCompletor(this.getRemoteClient().getAllServices()));
        }
    }


    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public PrintWriter getPrintWriter() {
        return outputWriter;
    }

    public int getConsoleWidth() {
        return this.consoleReader.getTermwidth();
    }


    static {
        for (Class commandClass : ClientCommand.COMMANDS) {
            ClientCommand command = null;
            try {
                command = (ClientCommand) commandClass.newInstance();
                commands.put(command.getPromptCommandString(), command);

            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public Map<String, ClientCommand> getCommands() {
        return commands;
    }
}
