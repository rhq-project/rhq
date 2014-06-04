package org.rhq.server.control.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

public class ExecutorAssist {

    private static Executor executor;

    static {
        executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler());
    }

    public static int execute(final File workingDir, final CommandLine commandLine, @SuppressWarnings("rawtypes") final Map environment, final boolean fireAndForget) throws IOException {
        int rValue = 0;
        try {
            // Synchronized to prevent two threads from setting different workingDirectory at the same time..
            synchronized(executor) {
                executor.setWorkingDirectory(workingDir);
                // null environment is fine in both cases, DefaultExecutor will use default environment in that case
                if(fireAndForget) {
                    // We're not interested in the return value
                    executor.execute(commandLine, environment, new DefaultExecuteResultHandler());
                } else {
                    rValue = executor.execute(commandLine, environment);
                }
            }
        } catch(ExecuteException e) {
            // DefaultExecutor has no detailed exception text, safe to ignore
            rValue = Math.max(e.getExitValue(), rValue);
        }
        return rValue;
    }

    public static int execute(final File workingDir, final CommandLine commandLine, final Map environment) throws IOException {
        return execute(workingDir, commandLine, environment, false);
    }

    public static int execute(final File workingDir, final CommandLine commandLine) throws IOException {
        return execute(workingDir, commandLine, false);
    }

    public static int execute(final File workingDir, final CommandLine commandLine, final boolean fireAndForget) throws IOException {
        return execute(workingDir, commandLine, null, false);
    }
}
