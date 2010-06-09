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
package org.rhq.enterprise.server.search;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.enterprise.server.search.RHQLParser.searchExpression_return;

/**
 * Test harness to verify correctness of RHQL grammar
 * 
 * @author Joseph Marques
 */
public class RHQLTest extends AssertJUnit {

    private enum TestResult {
        SUCCESS, FAILURE, TIMEOUT, SKIPPED;
    }

    private class AntlrTask implements Callable<TestResult> {
        private String line;

        public AntlrTask(String line) {
            this.line = line;
        }

        public TestResult call() throws Exception {
            try {
                System.out.println(line);
                ANTLRStringStream input = new ANTLRStringStream(line); // Create an input character stream from standard in
                RHQLLexer lexer = new RHQLLexer(input); // Create an echoLexer that feeds from that stream
                CommonTokenStream tokens = new CommonTokenStream(lexer); // Create a stream of tokens fed by the lexer
                RHQLParser parser = new RHQLParser(tokens); // Create a parser that feeds off the token stream
                searchExpression_return parseResults = parser.searchExpression(); // Begin parsing at 'searchExpression' rule
                System.out.println(parseResults.tree.toStringTree()); // Print result
                return TestResult.SUCCESS;
            } catch (Throwable t) {
                return TestResult.FAILURE;
            }
        }
    };

    private ExecutorService executor;

    @BeforeSuite
    public void init() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterSuite
    public void teardown() {
        executor.shutdownNow();
    }

    @Test
    public void testSingleLineRHQL() throws Exception {
        BufferedReader reader = null;
        List<String> successes = new java.util.ArrayList<String>();
        List<String> failures = new java.util.ArrayList<String>();
        int count = 0;
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("single-line-rhql.txt");
            reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            long timeout = 2000;
            while ((line = reader.readLine()) != null) {
                TestResult result = testSuccess(line, timeout);
                if (result == TestResult.SKIPPED) {
                    continue;
                }
                count++;
                if (result == TestResult.SUCCESS) {
                    successes.add(line);
                } else if (result == TestResult.FAILURE) {
                    failures.add(line);
                } else if (result == TestResult.TIMEOUT) {
                    System.out.println("Parsing took more than " + timeout
                        + "ms, does your grammar have an infinite loop?");
                }
            }
        } catch (Exception e) {
            System.out.println("Error testing single line RHQL: " + e);
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        System.out.println();
        for (String success : successes) {
            System.out.println("Parse success: " + success);
        }
        for (String failure : failures) {
            System.out.println("Parse failure: " + failure);
        }

        System.out.println();
        System.out.printf("RHQL expressions parsed: %1$s, Failures: %2$s", count, failures.size());
        System.out.println();

        assert failures.size() == 0;
    }

    private TestResult testSuccess(final String line, long timeout) {
        if (shouldSkip(line)) {
            return TestResult.SKIPPED; // skip empty lines used for visual separation in test file
        }

        AntlrTask task = new AntlrTask(line);
        Future<TestResult> futureTask = executor.submit(task);
        TestResult result = null;
        try {
            result = futureTask.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException ee) {
        } catch (TimeoutException te) {
            futureTask.cancel(true);
            result = TestResult.TIMEOUT;
        } catch (InterruptedException ie) {
            futureTask.cancel(true);
        }

        return result;
    }

    private boolean shouldSkip(String line) {
        line = line.trim();
        if (line.equals("")) {
            return true; // ignore empty lines
        }
        if (line.startsWith("#")) {
            return true; // ignore comments
        }
        return false;
    }
}
