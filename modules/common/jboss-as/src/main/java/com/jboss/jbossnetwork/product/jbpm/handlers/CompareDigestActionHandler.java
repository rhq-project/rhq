 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author Jason Dobies
 */
public class CompareDigestActionHandler extends BaseHandler {
    /**
     * The algorithm to use when calculating the digest (i.e. MD5). See Appendix A in the Java Cryptography Architecture
     * API Specification & Reference for information about standard algorithm names.
     */
    private String algorithm;

    /**
     * Location of the file being tested.
     */
    private String fileToBeCheckedLocation;

    /**
     * This value is compared, ignoring case, with the calculated digest of the indicated file.
     */
    private String expectedDigest;

    public void run(ExecutionContext executionContext) {
        try {
            HandlerUtils.checkFilenameExists(fileToBeCheckedLocation);
            HandlerUtils.checkFilenameIsAFile(fileToBeCheckedLocation);
            HandlerUtils.checkFilenameIsReadable(fileToBeCheckedLocation);

            verifyDigest();

            complete(executionContext, "Successfully checked digest of ["
                + HandlerUtils.formatPath(fileToBeCheckedLocation) + "]. Confirmed to be [" + expectedDigest + "].");
        } catch (Throwable e) {
            error(executionContext, e, MESSAGE_NO_CHANGES, TRANSITION_ERROR);
        }
    }

    public String getDescription() {
        return "Calculate the digest of [" + HandlerUtils.formatPath(getFileToBeCheckedLocation()) + "] using the ["
            + getAlgorithm() + "] algorithm and check it matches [" + getExpectedDigest() + "].";
    }

    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        setFileToBeCheckedLocation(substituteVariable(fileToBeCheckedLocation, executionContext));
        setAlgorithm(substituteVariable(algorithm, executionContext));
        setExpectedDigest(substituteVariable(expectedDigest, executionContext));
    }

    public void setPropertyDefaults() {
        if (algorithm == null) {
            setAlgorithm("MD5");
        }

        if (expectedDigest == null) {
            setExpectedDigest("#{software.MD5}");
        }
    }

    protected void checkProperties() throws ActionHandlerException {
        HandlerUtils.checkIsSet("algorithm", algorithm);
        HandlerUtils.checkIsSet("fileToBeCheckedLocation", fileToBeCheckedLocation);
        HandlerUtils.checkIsSet("expectedDigest", expectedDigest);
    }

    private void verifyDigest() throws ActionHandlerException {
        String actualDigest;

        try {
            actualDigest = calculateDigest();
        } catch (Exception e) {
            throw new ActionHandlerException("Failed trying to calculate digest of ["
                + HandlerUtils.formatPath(fileToBeCheckedLocation) + "]", e);
        }

        if (!expectedDigest.equalsIgnoreCase(actualDigest)) {
            throw new ActionHandlerException("Digest of [" + HandlerUtils.formatPath(fileToBeCheckedLocation)
                + "] is [" + actualDigest + "] and does not match expected value [" + expectedDigest + "]");
        }
    }

    private String calculateDigest() throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        DigestInputStream in = null;
        
        try {
            in = new DigestInputStream(new FileInputStream(fileToBeCheckedLocation), messageDigest);
            byte[] buffer = new byte[4096];
            while (in.read(buffer) != -1) {
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
        String digest = HandlerUtils.encode(messageDigest.digest());
        return digest;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getFileToBeCheckedLocation() {
        return fileToBeCheckedLocation;
    }

    public void setFileToBeCheckedLocation(String fileToBeCheckedLocation) {
        this.fileToBeCheckedLocation = fileToBeCheckedLocation;
    }

    public String getExpectedDigest() {
        return expectedDigest;
    }

    public void setExpectedDigest(String expectedDigest) {
        this.expectedDigest = expectedDigest;
    }
}