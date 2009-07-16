package org.rhq.enterprise.client.script;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import static org.rhq.enterprise.client.script.ScriptCmdLine.ArgType.INDEXED;
import static org.rhq.enterprise.client.script.ScriptCmdLine.ArgType.NAMED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdLineParser {

    public ScriptCmdLine parse(String[] cmdLine) throws ParseException {
        String[] args = Arrays.copyOfRange(cmdLine, 1, cmdLine.length);
        
        String shortOpts = "-:f:s:";
        LongOpt[] longOpts = {
                new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
                new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, 's')
        };
        Getopt getopt = new Getopt("exec", args, shortOpts, longOpts);

        List<String> scriptArgs = new ArrayList<String>();
        String argStyle = "indexed";
        String scriptName = null;

        int code = getopt.getopt();
        while (code != -1) {
            switch (code) {
                case ':':
                case '?':
                   throw new IllegalArgumentException("Invalid options");
                case 1:
                    scriptArgs.add(getopt.getOptarg());
                    break;
                case 'f':
                    scriptName = getopt.getOptarg();
                    break;
                case 's':
                    argStyle = getopt.getOptarg();
                    if (isInvalidArgStyle(argStyle)) {
                        throw new ParseException(argStyle + " - invalid value for style option");
                    }
                    break;
            }
            code = getopt.getopt();
        }

        return createScriptCmdLine(scriptName, argStyle, scriptArgs);
    }

    private boolean isInvalidArgStyle(String argStyle) {
        return !(INDEXED.value().equals(argStyle) || NAMED.value().equals(argStyle));
    }

    private ScriptCmdLine createScriptCmdLine(String scriptName, String argStyle,
            List<String> args) {
        ScriptCmdLine cmdLine = new ScriptCmdLine();
        cmdLine.setScriptFileName(scriptName);

        if (INDEXED.value().equals(argStyle)) {
            cmdLine.setArgType(INDEXED);
            for (String arg : args) {
                cmdLine.addArg(new ScriptArg(arg));
            }
        }
        else {
            cmdLine.setArgType(NAMED);
            for (String arg : args) {
                cmdLine.addArg(parseNamedArg(arg));
            }
        }

        return cmdLine;
    }

    private NamedScriptArg parseNamedArg(String arg) {
        String[] tokens = arg.split("=");
        String name = tokens[0];
        String value = null;

        if (tokens.length > 1) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 1; i < tokens.length; ++i) {
                buffer.append(tokens[i]);
            }
            value = buffer.toString();
        }

        return new NamedScriptArg(name, value);
    }

}
