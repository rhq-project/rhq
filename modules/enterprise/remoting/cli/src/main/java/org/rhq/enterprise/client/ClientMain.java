/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.MultiCompletor;
import jline.SimpleCompletor;
import mazz.i18n.Msg;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.commands.ClientCommand;
import org.rhq.enterprise.client.commands.ScriptCommand;
import org.rhq.enterprise.client.script.CommandLineParseException;
import org.rhq.enterprise.client.utility.CLIMetadataProvider;
import org.rhq.enterprise.client.utility.CodeCompletionCompletorWrapper;
import org.rhq.enterprise.client.utility.DummyCodeCompletion;
import org.rhq.enterprise.clientapi.RemoteClient;
import org.rhq.scripting.CodeCompletion;
import org.rhq.scripting.ScriptEngineInitializer;

/**
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class ClientMain {

    // I18N messaging
    private static final Msg MSG = ClientI18NFactory.getMsg();

    // Stored command map. Key to instance that handles that command.
    private static Map<String, ClientCommand> commands = new HashMap<String, ClientCommand>();

    public static final int DEFAULT_CONSOLE_WIDTH = 80;
    
    /**
     * This is the thread that is running the input loop; it accepts prompt commands from the user.
     */
    private Thread inputLoopThread;

    // JLine console reader
    private ConsoleReader consoleReader;

    private boolean stdinInput = true;

    // for feedback to user.
    private PrintWriter outputWriter;

    // Local storage of credentials for this session/client
    private String transport = null;
    private String host = null;
    private int port = 7080;
    private String user;
    private String pass;
    private String language;
    private ArrayList<String> notes = new ArrayList<String>();
    
    private RemoteClient remoteClient;

    // The subject that will be used to carry out all requested actions
    private Subject subject;
    
    private CodeCompletion codeCompletion;

    private boolean interactiveMode = true;

    private Recorder recorder = new NoOpRecorder();

    private ScriptEngine engine;
    private ScriptEngineInitializer scriptEngineInitializer;
    
    private class StartupConfiguration {
        public boolean askForPassword;
        public boolean displayUsage;
        public List<String> commandsToExec;
        public boolean invalidArgs;
        public boolean showVersionAndExit;
        public boolean showDetailedVersion;
        
        public void process() throws Exception {
            if (invalidArgs) {
                displayUsage();
                throw new IllegalArgumentException(MSG.getMsg(ClientI18NResourceKeys.BAD_ARGS));
            }
            
            if (displayUsage) {
                displayUsage();
            }
            
            if (askForPassword) {
                setPass(getConsoleReader().readLine("password: ", (char) 0));
            }
            
            if (isInteractiveMode()) {
                String version = showDetailedVersion ? Version.getProductNameAndVersionBuildInfo() : Version.getProductNameAndVersion();
                outputWriter.println(version);
                if (showVersionAndExit) {
                    // If -v was the only option specified, exit after printing the version.
                    System.exit(0);
                }
            }
            
            if (getUser() != null && getPass() != null) {
                ClientCommand loginCmd = getCommands().get("login");
                if (getHost() != null) {
                    loginCmd.execute(ClientMain.this, new String[] { "login", getUser(), getPass(), getHost(), String.valueOf(getPort()), getTransport() });
                } else {
                    loginCmd.execute(ClientMain.this, new String[] { "login", getUser(), getPass() });
                }
                if (!loggedIn()) {
                    if (isInteractiveMode()) {
                        return;
                    } else {
                        System.exit(1);
                    }
                }
            }
            
            if (commandsToExec != null && !commandsToExec.isEmpty()) {
                getCommands().get("exec").execute(ClientMain.this, commandsToExec.toArray(new String[commandsToExec.size()]));                
            }            
        }
    }

    public static void main(String[] args) {
        initCommands();
        try {
            new ClientMain().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run(String[] args) throws Exception {
        // capture startup arguments and setup the properties
        //from them
        StartupConfiguration config = processArguments(args);

        //initialize the CLI
        initialize();

        //process the arguments now that we are initialized
        config.process();

        if (isInteractiveMode()) {
            // begin client access loop
            inputLoop();
        }
    }

    private static void initCommands() {
        for (Class<ClientCommand> commandClass : ClientCommand.COMMANDS) {
            ClientCommand command;
            try {
                command = commandClass.newInstance();
                commands.put(command.getPromptCommandString(), command);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void initScriptCommand() {
        ScriptCommand sc = (ScriptCommand) commands.get("exec");
        sc.initClient(this);        
    }
    
    private void initCodeCompletion() {
        this.codeCompletion.setScriptContext(getScriptEngine().getContext());
        this.codeCompletion.setMetadataProvider(new CLIMetadataProvider());
    }

    public ClientMain() {
        // initialize the printwriter to system.out for console conversations
        outputWriter = new PrintWriter(System.out, true);
    }
    
    private void initialize() throws IOException {
        // this.inputReader = new BufferedReader(new
        // InputStreamReader(System.in));

        // initialize the printwriter to system.out for console conversations
        this.outputWriter = new PrintWriter(System.out, true);

        //ScriptCommand is super special because it handles executing all the code for us
        initScriptCommand();
        
        if (isInteractiveMode()) {
            // Initialize JLine console elements.
            consoleReader = new jline.ConsoleReader();
    
            // Setup the command line completers for listed actions for the user before login
            // completes initial commands available
            Completor commandCompletor = new SimpleCompletor(commands.keySet().toArray(new String[commands.size()]));
            // completes help arguments (basically, help <command>)
            Completor helpCompletor = new ArgumentCompletor(new Completor[] { new SimpleCompletor("help"),
                new SimpleCompletor(commands.keySet().toArray(new String[commands.size()])) });
    
            this.codeCompletion = ScriptEngineFactory.getCodeCompletion(getLanguage());
            if (codeCompletion == null) {
                //the language module for this language doesn't support code completion
                //let's provide a dummy one.
                codeCompletion = new DummyCodeCompletion();
            }

            initCodeCompletion();

            consoleReader.addCompletor(new MultiCompletor(new Completor[] {
                new CodeCompletionCompletorWrapper(codeCompletion, outputWriter, consoleReader), helpCompletor,
                commandCompletor }));
                
            // enable pagination
            consoleReader.setUsePagination(true);
        }
    }
    
    public String getUserInput(String prompt) {

        String input_string = "";

        while ((input_string != null) && (input_string.trim().length() == 0)) {
            if (prompt == null) {
                if (!loggedIn()) {
                    prompt = "unconnected$ ";
                } else {
                    // Modify the prompt to display host:port(logged-in-user)
                    String loggedInUser = "";
                    if ((getSubject() != null) && (getSubject().getName() != null)) {
                        loggedInUser = getSubject().getName();
                    }
                    if (loggedInUser.trim().length() > 0) {
                        prompt = loggedInUser + "@" + host + ":" + port + "$ ";
                    } else {
                        prompt = host + ":" + port + "$ ";
                    }
                }
            }

            try {
                outputWriter.flush();
                input_string = consoleReader.readLine(prompt);
            } catch (Exception e) {
                input_string = null;
            }
        }

        if (input_string != null) {
            // if we are processing a script, show the input that was just read
            if (!stdinInput) {
                outputWriter.println(input_string);
            }
        }

        return input_string;
    }

    public ConsoleReader getConsoleReader() {
        return consoleReader;
    }

    /**
     * Indicates whether the 'Subject', used for all authenticated actions, is currently logged in.
     *
     * @return flag indicating status of realtime check.
     */
    public boolean loggedIn() {
        return subject != null && remoteClient != null && remoteClient.isLoggedIn();
    }

    /**
     * This enters in an infinite loop. Because this never returns, the current thread never dies and hence the agent
     * stays up and running. The user can enter agent commands at the prompt - the commands are sent to the agent as if
     * the user is a remote client.
     */
    private void inputLoop() {
        // we need to start a new thread and run our loop in it; otherwise, our
        // shutdown hook doesn't work
        Runnable loop_runnable = new Runnable() {
            public void run() {
                while (true) {
                    String cmd;
                    cmd = getUserInput(null);

                    try {
                        recorder.record(cmd);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        // parse the command into separate arguments and execute
                        String[] cmd_args = parseCommandLine(cmd);
                        boolean can_continue = executePromptCommand(cmd_args);

                        // break the input loop if the prompt command told us to exit
                        // if we are not in daemon mode, this really will end up killing the agent
                        if (!can_continue) {
                            break;
                        }
                    } catch (Throwable t) {
                        // outputWriter.println(ThrowableUtil.getAllMessages(t));
                        t.printStackTrace(outputWriter);
                        // LOG.debug(t,
                        // AgentI18NResourceKeys.COMMAND_FAILURE_STACK_TRACE);
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

    public boolean executePromptCommand(String[] args) throws Exception {
        String cmd = args[0];
        if (commands.containsKey(cmd)) {
            ClientCommand command = commands.get(cmd);

            if (shouldDisplayHelp(args)) {
                outputWriter.println("Usage: " + command.getSyntax());
                outputWriter.println(command.getDetailedHelp());
                return true;
            }

            try {
                boolean response = command.execute(this, args);
                processNotes(outputWriter);
                outputWriter.println("");
                return response;
            } catch (CommandLineParseException e) {
                outputWriter.println(command.getPromptCommandString() + ": " + e.getMessage());
                outputWriter.println("Usage: " + command.getSyntax());
            } catch (ArrayIndexOutOfBoundsException e) {
                outputWriter.println(command.getPromptCommandString()
                    + ": An incorrect number of arguments was specified.");
                outputWriter.println("Usage: " + command.getSyntax());
            }
        } else {
            boolean result = commands.get("exec").execute(this, args);
            if (loggedIn()) {
                this.codeCompletion.setScriptContext(getScriptEngine().getContext());
            }

            return result;
        }
        return true;
    }

    private boolean shouldDisplayHelp(String[] args) {
        if (args.length < 2) {
            return false;
        }

        return args[1].equals("-h") || args[1].equals("--help");
    }

    /**
     * Meant to display small note/helpful ui messages to the user as feedback from the previous command.
     *
     * @param outputWriter2
     *            reference to printWriter.
     */
    private void processNotes(PrintWriter outputWriter2) {
        if ((outputWriter2 != null) && (notes.size() > 0)) {
            for (String line : notes) {
                outputWriter2.println("-> " + line);
            }
            notes.clear();
        }
    }

    /**
     * Given a command line, this will parse each argument and return the argument array.
     *
     * @param cmdLine
     *            the command line
     * @return the array of command line arguments
     */
    public String[] parseCommandLine(String cmdLine) {
        if (cmdLine == null) {
            return new String[] { "" };
        }

        ByteArrayInputStream in = new ByteArrayInputStream(cmdLine.getBytes());
        StreamTokenizer strtok = new StreamTokenizer(new InputStreamReader(in));
        List<String> args = new ArrayList<String>();
        boolean keep_going = true;

        boolean isScriptFileCommand = false;
        boolean isNamedArgs = false;

        // we don't want to parse numbers and we want ' to be a normal word
        // character
        strtok.ordinaryChars('0', '9');
        strtok.ordinaryChar('.');
        strtok.ordinaryChar('-');
        strtok.ordinaryChar('\'');
        strtok.wordChars(33, 127);

        // parse the command line
        while (keep_going) {
            int nextToken;

            try {
                // if we are executing a script file and have reached the arguments, we
                // want to reset the tokenizer's syntax so that handle single and double
                // quotes correctly.
                if (isScriptFileCommand && args.size() > 2 && args.get(args.size() - 2).equals("-f")) {
                    strtok.resetSyntax();
                    strtok.ordinaryChars('0', '9');
                    strtok.ordinaryChar('.');
                    strtok.ordinaryChar('-');
                    strtok.quoteChar('\'');
                    strtok.quoteChar('"');
                    strtok.wordChars(33, 33);
                    strtok.wordChars(35, 38);
                    strtok.wordChars(40, 127);
                }

                nextToken = strtok.nextToken();

            } catch (IOException e) {
                nextToken = StreamTokenizer.TT_EOF;
            }

            if (nextToken == java.io.StreamTokenizer.TT_WORD) {
                if (args.size() > 0 && strtok.sval.equals("-f")) {
                    isScriptFileCommand = true;
                }
                args.add(strtok.sval);
                if (strtok.sval.equals("--args-style=named")) {
                    isNamedArgs = true;
                }
            } else if (nextToken == '\"' || nextToken == '\'') {
                args.add(strtok.sval);
            } else if ((nextToken == java.io.StreamTokenizer.TT_EOF) || (nextToken == java.io.StreamTokenizer.TT_EOL)) {
                keep_going = false;
            }
        }

        if (isNamedArgs) {
            List<String> newArgs = new ArrayList<String>();
            int namedArgsIndex = args.indexOf("--args-style=named");

            for (int i = 0; i <= namedArgsIndex; ++i) {
                newArgs.add(args.get(i));
            }

            String namedArg = null;
            for (int i = namedArgsIndex + 1; i < args.size(); ++i) {
                if (namedArg == null && args.get(i).endsWith("=")) {
                    namedArg = args.get(i);
                } else if (namedArg != null) {
                    newArgs.add(args.get(i - 1) + args.get(i));
                    namedArg = null;
                } else {
                    newArgs.add(args.get(i));
                }
            }

            return newArgs.toArray(new String[newArgs.size()]);
        }

        return args.toArray(new String[args.size()]);
    }

    private void displayUsage() {
        outputWriter
            .println("rhq-cli.sh [-h] [-u user] [-p pass] [-P] [-s host] [-t port] [-v] [-f file]|[-c command]");
    }

    StartupConfiguration processArguments(String[] args) throws IllegalArgumentException, IOException {
        StartupConfiguration config = new StartupConfiguration();
        
        String sopts = "-:hu:p:Ps:t:r:c:f:v";
        LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
            new LongOpt("prompt", LongOpt.OPTIONAL_ARGUMENT, null, 'P'),
            new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 's'),
            new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 't'),
            new LongOpt("transport", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
            new LongOpt("command", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
            new LongOpt("file", LongOpt.NO_ARGUMENT, null, 'f'),
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
            new LongOpt("language", LongOpt.REQUIRED_ARGUMENT, null, 'l'),
            new LongOpt("args-style", LongOpt.REQUIRED_ARGUMENT, null, -2) };

        Getopt getopt = new Getopt("Cli", args, sopts, lopts, false);
        int code;

        List<String> execCmdLine = new ArrayList<String>();
        execCmdLine.add("exec");

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                config.invalidArgs = true;
                break;
            }

            case 1: {
                // this catches non-option arguments which can be passed when running a script in non-interactive mode
                // with -f or running a single command in non-interactive mode with -c.
                execCmdLine.add(getopt.getOptarg());
                break;
            }

            case 'h': {
                config.displayUsage = true;
                break;
            }

            case 'u': {
                this.user = getopt.getOptarg();
                break;
            }
            case 'p': {
                this.pass = getopt.getOptarg();
                break;
            }
            case 'P': {
                config.askForPassword = true;
                break;
            }
            case 'c': {
                interactiveMode = false;
                execCmdLine.add(getopt.getOptarg());
                break;
            }
            case 'f': {
                interactiveMode = false;
                execCmdLine.add("-f");
                execCmdLine.add(getopt.getOptarg());
                break;
            }
            case -2: {
                execCmdLine.add("--args-style=" + getopt.getOptarg());
                break;
            }
            case 's': {
                setHost(getopt.getOptarg());
                break;
            }
            case 'r': {
                setTransport(getopt.getOptarg());
                break;
            }
            case 't': {
                String portArg = getopt.getOptarg();
                try {
                    setPort(Integer.parseInt(portArg));
                } catch (Exception e) {
                    outputWriter.println("Invalid port [" + portArg + "]");
                    System.exit(1);
                }
                break;
            }
            case 'v': {        
                config.showDetailedVersion  = true;
                if (args.length == 1) {
                    config.showVersionAndExit = true;
                }
                break;
            }
            case 'l':
                this.language = getopt.getOptarg();
                break;
            }
        }

        if (!interactiveMode) {
            config.commandsToExec = execCmdLine;
        }
        
        return config;
    }

    public RemoteClient getRemoteClient() {
        return remoteClient;
    }

    public void setRemoteClient(RemoteClient remoteClient) {
        this.remoteClient = remoteClient;

        initScriptCommand();
        if (isInteractiveMode()) {
            initCodeCompletion();
        }
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
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

    public void setPrintWriter(PrintWriter writer) {
        this.outputWriter = writer;
    }

    public String getLanguage() {
        return this.language == null ? "javascript" : this.language;
    }
    
    public int getConsoleWidth() {
        //the console reader might be null when this method is asked for the output
        //width in non-interactive mode where we don't attach to stdin.
        return this.consoleReader == null ? DEFAULT_CONSOLE_WIDTH : this.consoleReader.getTermwidth();
    }

    public ScriptEngine getScriptEngine() {
        if (engine == null) {
            try {
                engine = ScriptEngineFactory.getScriptEngine(getLanguage(),
                    new PackageFinder(Arrays.asList(getLibDir())), null);

                if (engine == null) {
                    throw new IllegalStateException("The scripting language '" + getLanguage()
                        + "' could not be loaded.");
                }
                scriptEngineInitializer = ScriptEngineFactory.getInitializer(getLanguage());
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return engine;
    }
    
    public String getUsefulErrorMessage(ScriptException e) {
        return scriptEngineInitializer.extractUserFriendlyErrorMessage(e);
    }

    public Map<String, ClientCommand> getCommands() {
        return commands;
    }

    /**
     * This method allows ClientCommands to insert a small note to be displayed after the command has been executed. A
     * note can be an indication of a problem that was handled or a note about some option that should be changed.
     *
     * These notes are meant to be terse, and pasted/purged at the end of every command execution.
     *
     * @param note the note to be displayed, e.g. "There were errors retrieving some data from the server objects. See
     *             System Admin."
     */
    public void addMenuNote(String note) {
        if ((note != null) && (note.trim().length() > 0)) {
            notes.add(note);
        }
    }

    public boolean isInteractiveMode() {
        return interactiveMode;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    public void setRecorder(Recorder recorder) {
        this.recorder = recorder;
    }
    
    private static File getLibDir() {
        String cwd = System.getProperty("user.dir");
        return new File(cwd, "lib");
    }
}
