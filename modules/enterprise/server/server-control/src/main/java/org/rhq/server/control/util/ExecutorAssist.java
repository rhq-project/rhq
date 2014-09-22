package org.rhq.server.control.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
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

    public static int execute(final File workingDir, final CommandLine commandLine, @SuppressWarnings("rawtypes") final Map environment) throws IOException {
        int rValue = 0;
        try {
            // Synchronized to prevent two threads from setting different workingDirectory at the same time..
            synchronized(executor) {
                executor.setWorkingDirectory(workingDir);
                // null environment is fine in both cases, DefaultExecutor will use default environment in that case
                rValue = executor.execute(commandLine, environment);
            }
        } catch(ExecuteException e) {
            // DefaultExecutor has no detailed exception text, safe to ignore
            rValue = Math.max(e.getExitValue(), rValue);
        }
        return rValue;
    }

    public static Future<Integer> executeAsync(final File workingDir, final CommandLine commandLine, final Map environment) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return execute(workingDir, commandLine, environment);
            }
        });
    }

    public static int execute(final File workingDir, final CommandLine commandLine) throws IOException {
        return execute(workingDir, commandLine, null);
    }
}
