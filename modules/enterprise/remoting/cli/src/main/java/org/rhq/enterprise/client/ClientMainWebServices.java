///*
// * JBoss, a division of Red Hat.
// * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
// */
//
//package org.rhq.enterprise.client;
//
//import gnu.getopt.LongOpt;
//import gnu.getopt.Getopt;
//
//import java.io.File;
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.PrintWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.ByteArrayInputStream;
//import java.io.StreamTokenizer;
//import java.net.URL;
//import java.net.MalformedURLException;
//import java.util.List;
//import java.util.ArrayList;
//
//import mazz.i18n.Msg;
//
//import javax.xml.namespace.QName;
//import javax.xml.ws.Service;
//import javax.security.auth.login.LoginException;
//
//import org.rhq.enterprise.server.auth.SubjectManagerRemote;
//import org.rhq.core.domain.auth.Subject;
//
///**
// * @author Greg Hinkle
// */
//public class ClientMainWebServices {
//
//    private static final Msg MSG = ClientI18NFactory.getMsg();
//    public static final String SERVICE_NAME_BASE = ".server.enterprise.rhq.org/";
//    public static final String WSDL_BASE = "http://localhost:7080/rhq-rhq-enterprise-server-ejb3/";
//
//    /**
//     * This is the thread that is running the input loop; it accepts prompt commands from the user.
//     */
//    private Thread inputLoopThread;
//
//    private BufferedReader inputReader;
//
//    private boolean stdinInput = true;
//
//
//    private PrintWriter outputWriter;
//
//    private String host;
//    private String port;
//    private String user;
//    private String pass;
//
//
//    public static void main(String[] args) {
////        ClientMain main = new ClientMain();
////        main.processArguments(args);
////        main.start();
//    }
//
//    public void start() {
//        outputWriter = new PrintWriter(System.out);
//        inputReader = new BufferedReader(new InputStreamReader(System.in));
//
//    }
//
//
//    public void login() throws MalformedURLException, LoginException {
//
//
//        URL url = new URL(WSDL_BASE + "SubjectManagerBean?wsdl");
//        QName qname = new QName("http://auth" + SERVICE_NAME_BASE, "SubjectManagerBeanService");
//
//        Service s = Service.create(url, qname);
//        //s.setHandlerResolver(new ClientConsole.MyHandlerResolver(s.getHandlerResolver()));
//
//
//        SubjectManagerRemote sm = s.getPort(SubjectManagerRemote.class);
//        Subject subject = sm.login("jonadmin", "jonadmin");
//
//
//    }
//
//
//    public String getUserInput(String prompt) {
//        String input_string = "";
//        boolean use_default_prompt = (prompt == null);
//
//        while ((input_string != null) && (input_string.trim().length() == 0)) {
//            prompt = host + ":" + port + " ";
//
//            outputWriter.print(prompt);
//
//            try {
//                outputWriter.flush();
//                input_string = inputReader.readLine();
//            } catch (Exception e) {
//                input_string = null;
//            }
//        }
//
//        if (input_string != null) {
//            // if we are processing a script, show the input that was just read in
//            if (!stdinInput) {
//                outputWriter.println(input_string);
//            }
//        } else if (!stdinInput) {
//            // if we are processing a script, we hit the EOF, so close the input stream
//            try {
//                inputReader.close();
//            } catch (IOException e1) {
//            }
//
//            // if we are not in daemon mode, let's now start processing prompt commands coming in via stdin
////            if (!m_daemonMode) {
////                inputReader = new BufferedReader(new InputStreamReader(System.in));
////                stdinInput = true;
////                input_string = "";
////            } else {
////                inputReader = null;
////            }
//        }
//
//        return input_string;
//    }
//
//    /**
//     * This enters in an infinite loop. Because this never returns, the current thread never dies and hence the agent
//     * stays up and running. The user can enter agent commands at the prompt - the commands are sent to the agent as if
//     * the user is a remote client.
//     */
//    private void inputLoop() {
//        // we need to start a new thread and run our loop in it; otherwise, our shutdown hook doesn't work
//        Runnable loop_runnable = new Runnable() {
//            public void run() {
//                while (true) {
//                    // get a command from the user
//                    // if in daemon mode, only get input if reading from an input file; ignore stdin
//                    String cmd;
////                        if ((m_daemonMode == false) || (stdinInput == false)) {
//                    cmd = getUserInput(null);
////                        } else {
////                            cmd = null;
////                        }
//
//                    try {
//                        // parse the command into separate arguments and execute it
//                        String[] cmd_args = parseCommandLine(cmd);
//                        boolean can_continue = executePromptCommand(cmd_args);
//
//                        // break the input loop if the prompt command told us to exit
//                        // if we are not in daemon mode, this really will end up killing the agent
//                        if (!can_continue) {
//                            break;
//                        }
//                    } catch (Throwable t) {
//                        //outputWriter.println(MSG.getMsg(ClientI18NResourceKeys.COMMAND_FAILURE, cmd, ThrowableUtil.getAllMessages(t)));
//                        //LOG.debug(t, AgentI18NResourceKeys.COMMAND_FAILURE_STACK_TRACE);
//                    }
//                }
//
//                return;
//            }
//        };
//
//        // start the thread
//        inputLoopThread = new Thread(loop_runnable);
//        inputLoopThread.setName("RHQ Agent Prompt Input Thread");
//        inputLoopThread.setDaemon(false);
//        inputLoopThread.start();
//
//        return;
//    }
//
//
//    private boolean executePromptCommand(String[] args) {
//        String cmd = args[0];
//        if (cmd.equals("findResource")) {
//
//        } else if (cmd.equals("quit")) {
//            return false;
//        }
//        return true;
//    }
//
//
//    /**
//     * Given a command line, this will parse each argument and return the argument array.
//     *
//     * @param cmdLine the command line
//     * @return the array of command line arguments
//     */
//    private String[] parseCommandLine(String cmdLine) {
//        ByteArrayInputStream in = new ByteArrayInputStream(cmdLine.getBytes());
//        StreamTokenizer strtok = new StreamTokenizer(new InputStreamReader(in));
//        List<String> args = new ArrayList<String>();
//        boolean keep_going = true;
//
//        // we don't want to parse numbers and we want ' to be a normal word character
//        strtok.ordinaryChars('0', '9');
//        strtok.ordinaryChar('.');
//        strtok.ordinaryChar('-');
//        strtok.ordinaryChar('\'');
//        strtok.wordChars(33, 127);
//        strtok.quoteChar('\"');
//
//        // parse the command line
//        while (keep_going) {
//            int nextToken;
//
//            try {
//                nextToken = strtok.nextToken();
//            } catch (IOException e) {
//                nextToken = StreamTokenizer.TT_EOF;
//            }
//
//            if (nextToken == StreamTokenizer.TT_WORD) {
//                args.add(strtok.sval);
//            } else if (nextToken == '\"') {
//                args.add(strtok.sval);
//            } else if ((nextToken == StreamTokenizer.TT_EOF) || (nextToken == StreamTokenizer.TT_EOL)) {
//                keep_going = false;
//            }
//        }
//
//        return args.toArray(new String[args.size()]);
//    }
//
//
//    private void displayUsage() {
//        outputWriter.println(MSG.getMsg(ClientI18NResourceKeys.USAGE, this.getClass().getName()));
//    }
//
//
//    private void processArguments(String[] args) throws IllegalArgumentException {
//
//        String sopts = "-:hu:p:s:t:f:";
//        LongOpt[] lopts = {
//                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
//                new LongOpt("user", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
//                new LongOpt("password", LongOpt.REQUIRED_ARGUMENT, null, 'p'),
//                new LongOpt("host", LongOpt.REQUIRED_ARGUMENT, null, 's'),
//                new LongOpt("port", LongOpt.REQUIRED_ARGUMENT, null, 't'),
//                new LongOpt("file", LongOpt.NO_ARGUMENT, null, 'f')
//        };
//
//        String config_file_name = null;
//        boolean clean_config = false;
//        boolean purge_data = false;
//
//        Getopt getopt = new Getopt("agent", args, sopts, lopts);
//        int code;
//
//        while ((code = getopt.getopt()) != -1) {
//            switch (code) {
//                case ':':
//                case '?': {
//                    // for now both of these should exit
//                    displayUsage();
//                    throw new IllegalArgumentException("mm");//MSG.getMsg(ClientI18NResourceKeys.BAD_ARGS,null));
//                }
//
//                case 1: {
//                    // this will catch non-option arguments (which we don't currently care about)
//                    System.err.println(MSG.getMsg(ClientI18NResourceKeys.USAGE, getopt.getOptarg()));
//                    break;
//                }
//
//                case 'h': {
//                    displayUsage();
////                throw new HelpException(MSG.getMsg(ClientI18NResourceKeys.HELP_SHOWN));
//                }
//
//                case 'D': {
//                    // set a system property
//                    String sysprop = getopt.getOptarg();
//                    int i = sysprop.indexOf("=");
//                    String name;
//                    String value;
//
//                    if (i == -1) {
//                        name = sysprop;
//                        value = "true";
//                    } else {
//                        name = sysprop.substring(0, i);
//                        value = sysprop.substring(i + 1, sysprop.length());
//                    }
//
//                    System.setProperty(name, value);
//                    //   LOG.debug(ClientI18NResourceKeys.SYSPROP_SET, name, value);
//
//                    break;
//                }
//
//                case 'c': {
//                    config_file_name = getopt.getOptarg();
//                    break;
//                }
//
//                case 'l': {
//                    clean_config = true;
//                    purge_data = true;
//                    break;
//                }
//
//                case 'u': {
//                    purge_data = true;
//                    break;
//                }
//
//                case 'a': {
//                    ///m_advancedSetup = true;
//                    break;
//                }
//
//                case 's': {
//                    //m_forcedSetup = true;
//                    break;
//                }
//
//                case 'n': {
//                    //m_startAtBoot = false;
//                    break;
//                }
//
//                case 'p': {
//                    //se//tConfigurationPreferencesNode(getopt.getOptarg());
//                    break;
//                }
//
//                case 'd': {
//                    //m_daemonMode = true;
//                    break;
//                }
//
//                case 'i': {
//                    File script = new File(getopt.getOptarg());
//
//                    try {
//                        inputReader = new BufferedReader(new FileReader(script));
//                        stdinInput = false;
//                    } catch (Exception e) {
//                        //throw new IllegalArgumentException(MSG.getMsg(ClientI18NResourceKeys.BAD_INPUT_FILE, script, e));
//                    }
//
//                    break;
//                }
//
//                case 'o': {
//                    File output = new File(getopt.getOptarg());
//
//                    try {
//                        File parentDir = output.getParentFile();
//                        if ((parentDir != null) && (!parentDir.exists())) {
//                            parentDir.mkdirs();
//                        }
//
//                        outputWriter = new PrintWriter(new FileWriter(output), true);
//                    } catch (Exception e) {
//                        //throw new IllegalArgumentException(MSG.getMsg(ClientI18NResourceKeys.BAD_OUTPUT_FILE, output, e));
//                    }
//
//                    break;
//                }
//
//                case 't': {
//                    // SystemInfoFactory.disableNativeSystemInfo();
//                    // LOG.info(ClientI18NResourceKeys.NATIVE_SYSTEM_DISABLED);
//                }
//            }
//        }
//    }
//}