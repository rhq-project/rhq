/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures, updates and deletes additional JAVA_OPTS set at the
 * bottom of a standard configuration file used by AS7 standalone.
 * This has been designeed, tested and used with standard.conf (for Linux)
 * and standard.conf.bat (for Windows).
 *
 * @author Stefan Negrea
 *
 */
public abstract class AdditionalJavaOpts {

    private static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * @return content sequence
     */
    protected abstract String[] getSequence();

    /**
     * @return index of additional JAVA_OPTS content
     */
    protected abstract int getContentIndex();

    /**
     * @return end of sequence character
     */
    protected abstract char getEndOfSequenceCharacter();


    /**
     * Linux specific JAVA_OPTS configuration handler.
     *
     */
    public static class LinuxConfiguration extends AdditionalJavaOpts {
        private final String[] sequence = new String[] {
            "##  JAVA_OPTS (set via RHQ) - Start     ######################################",
            "##  PLEASE DO NOT UPDATE OUTSIDE RHQ!!! ######################################",
            "JAVA_OPTS_ADDITIONAL=\"",
            "JAVA_OPTS=\"$JAVA_OPTS $JAVA_OPTS_ADDITIONAL\"",
            "##  JAVA_OPTS (set via RHQ) - End       ######################################" };

        private final int contentIndex = 2;

        @Override
        protected String[] getSequence() {
            return sequence;
        }

        @Override
        protected int getContentIndex() {
            return contentIndex;
        }

        @Override
        protected char getEndOfSequenceCharacter() {
            return '"';
        }
    }


    /**
     * Windows specific JAVA_OPTS configuration handler.
     *
     */
    public static class WindowsConfiguration extends AdditionalJavaOpts {
        private final String[] sequence = new String[] {
            "rem ###  JAVA_OPTS (set via RHQ) - Start     ####################################",
            "rem ###  PLEASE DO NOT UPDATE OUTSIDE RHQ!!! ####################################",
            "set \"JAVA_OPTS_ADDITIONAL=",
            "set \"JAVA_OPTS=%JAVA_OPTS% %JAVA_OPTS_ADDITIONAL%\"",
            "rem ###  JAVA_OPTS (set via RHQ) - End       ####################################" };

        private final int contentIndex = 2;

        @Override
        public String[] getSequence() {
            return sequence;
        }

        @Override
        public int getContentIndex() {
            return contentIndex;
        }

        @Override
        public char getEndOfSequenceCharacter(){
            return '"';
        }
    }

    /**
     * Adds additional JAVA_OPTS setting to the config file. The code will attempt
     * to merge/update existing content if detected at the bottom of the file. If
     * no existing content found then new content is added only at the bottom of the file.
     *
     * @param configFile config file
     * @param additionalJavaOptsContent additional JAVA_OPTS content
     * @throws Exception
     */
    public void update(File configFile, String additionalJavaOptsContent) throws Exception {

        List<String> fileContent = new ArrayList<String>();

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        try {
            String line = null;

            while ((line = br.readLine()) != null) {
                fileContent.add(line);
            }
        } finally {
            br.close();
        }


        if (fileContent.size() == 0) {
            return;
        }

        //strip empty lines at the end of the file
        while(fileContent.get(fileContent.size() -1).trim().isEmpty()) {
            fileContent.remove(fileContent.size() - 1);
        }

        additionalJavaOptsContent = additionalJavaOptsContent.replace(this.getEndOfSequenceCharacter() + "", "");
        String javaOptsNewContent = this.getSequence()[this.getContentIndex()] + additionalJavaOptsContent
            + this.getEndOfSequenceCharacter();

        boolean goodSequence = true;
        boolean identicalJavaOpts = false;
        int lineToUpdate = -1;

        int potentialSequenceStart = fileContent.size() - this.getSequence().length;

        for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
            if (!fileContent.get(potentialSequenceStart + sequenceIndex).trim()
                .startsWith(this.getSequence()[sequenceIndex])) {
                goodSequence = false;
                break;
            }

            if (sequenceIndex == this.getContentIndex()) {
                if (fileContent.get(potentialSequenceStart + sequenceIndex).trim().equals(javaOptsNewContent)) {
                    identicalJavaOpts = true;
                }

                lineToUpdate = potentialSequenceStart + sequenceIndex;
            }
        }

        if (identicalJavaOpts) {
            return;
        }

        if (!goodSequence) {
            for (int index = 0; index < this.getSequence().length; index++) {
                if (this.getContentIndex() == index) {
                    fileContent.add(this.getSequence()[index] + additionalJavaOptsContent
                        + this.getEndOfSequenceCharacter());
                } else {
                    fileContent.add(this.getSequence()[index]);
                }
            }
        } else {
            fileContent.set(lineToUpdate, javaOptsNewContent);
        }

        BufferedWriter updatedConfigFile = new BufferedWriter(new FileWriter(configFile));
        try {
            for (String line : fileContent) {
                updatedConfigFile.write(line);
                updatedConfigFile.write(NEW_LINE);
            }
        } finally {
            updatedConfigFile.close();
        }
    }

    /**
     * Discover additional JAVA_OPTS setting from to the config file. The code will attempt
     * to detect only RHQ set JAVA_OPTS content that is bottom of the file.
     *
     * @param configFile config file
     * @throws Exception
     */
    public String discover(File configFile)
        throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        List<String> fileContent = new ArrayList<String>();
        try {
            String line = null;

            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    fileContent.add(line);
                }
            }

        } finally {
            br.close();
        }

        if (fileContent.size() == 0) {
            return null;
        }

        boolean goodSequence = true;
        int potentialSequenceStart = fileContent.size() - this.getSequence().length;
        String additionalJavaOptsValue = null;

        for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
            if (!fileContent.get(potentialSequenceStart + sequenceIndex).trim()
                .startsWith(this.getSequence()[sequenceIndex])) {
                goodSequence = false;
                break;
            }

            if (sequenceIndex == this.getContentIndex()) {
                additionalJavaOptsValue = fileContent.get(potentialSequenceStart + sequenceIndex)
                    .replace(this.getSequence()[sequenceIndex], "");
                additionalJavaOptsValue = additionalJavaOptsValue.substring(0,
                    additionalJavaOptsValue.lastIndexOf(this.getEndOfSequenceCharacter()));
            }
        }

        if (!goodSequence) {
            additionalJavaOptsValue = null;
        } else {
            additionalJavaOptsValue = additionalJavaOptsValue.replace(this.getEndOfSequenceCharacter() + "", "");
        }

        return additionalJavaOptsValue;
    }

    /**
     * Clean the config file of any traces of JAVA_OPTS set via RHQ. If
     * the content is set inadvertently multiple times it will delete all
     * instances.
     *
     * @param configFile
     * @param javaOptsConfig
     * @throws Exception
     */
    public void clean(File configFile) throws Exception {
        String line;
        int lineNumber;

        List<Integer> potentialLinesToClear = new ArrayList<Integer>();
        List<List<String>> potentialSequencesToClear = new ArrayList<List<String>>();

        //find all the lines that have the start sequence
        BufferedReader br = new BufferedReader(new FileReader(configFile));
        try {
            line = null;
            lineNumber = -1;

            int lastPotentialLineToClear = -1;
            List<String> lastPotentialSequenceToClear = null;
            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (line.trim().startsWith(this.getSequence()[0])) {
                    lastPotentialLineToClear = lineNumber;
                    lastPotentialSequenceToClear = new ArrayList<String>();

                    potentialLinesToClear.add(lastPotentialLineToClear);
                    potentialSequencesToClear.add(lastPotentialSequenceToClear);
                }

                if (lastPotentialLineToClear != -1 && lineNumber < lastPotentialLineToClear + this.getSequence().length) {
                    lastPotentialSequenceToClear.add(line);
                }
            }
        } finally {
            br.close();
        }

        if (potentialLinesToClear.size() == 0) {
            return;
        }

        //remove those spaced too close or not matching from the lines to clear list
        List<Integer> linesToClear = new ArrayList<Integer>(potentialLinesToClear.size());
        for (int index = 0; index < potentialLinesToClear.size(); index++) {
            List<String> sequenceToCheck = potentialSequencesToClear.get(index);

            if (sequenceToCheck.size() == this.getSequence().length) {
                boolean goodSequence = true;

                for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
                    if (!sequenceToCheck.get(sequenceIndex).trim().startsWith(this.getSequence()[sequenceIndex])) {
                        goodSequence = false;
                        break;
                    }
                }

                if (goodSequence) {
                    //add all the lines to clear to the list of lines to clear
                    int startLineToClear = potentialLinesToClear.get(index);
                    for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
                        linesToClear.add(startLineToClear + sequenceIndex);
                    }
                }
            }
        }

        if (linesToClear.size() == 0) {
            return;
        }

        ByteArrayOutputStream cachedUpdatedFileContent = new ByteArrayOutputStream();
        BufferedWriter updatedFileContent = new BufferedWriter(new OutputStreamWriter(cachedUpdatedFileContent));

        String newLineSeparator = System.getProperty("line.separator");

        br = new BufferedReader(new FileReader(configFile));
        try {
            line = null;
            lineNumber = -1;
            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (!linesToClear.isEmpty() && lineNumber == linesToClear.get(0)) {
                    linesToClear.remove(0);
                } else {
                    updatedFileContent.write(line + newLineSeparator);
                }
            }
        } finally {
            br.close();
            updatedFileContent.close();
        }

        FileOutputStream updatedConfigFile = new FileOutputStream(configFile);
        try {
            cachedUpdatedFileContent.writeTo(updatedConfigFile);
        } finally {
            updatedConfigFile.close();
        }
    }
}
