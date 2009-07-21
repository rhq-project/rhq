/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.client;

public class PerfTest {

    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            Exception e = new Exception();
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().endsWith("AOPRemotingInvocationHandler")) {
                    System.out.println("IS REMOTE? " + el.getClassName());
                }
            }
        }

        System.out.println("Time: " + (System.currentTimeMillis() - start));
    }

}
