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

public abstract class AdditionalJavaOptsConfig {

    private static final String NEW_LINE = System.getProperty("line.separator");

    public abstract String[] getSequence();

    public abstract int getContentIndex();

    public abstract char getEndOfSequenceCharacter();

    /**
     * Linux specific JAVA_OPTS configuration handler.
     *
     */
    public static class LinuxConfiguration extends AdditionalJavaOptsConfig {
        private final String[] sequence = new String[] {
            "##  JAVA_OPTS (set via RHQ) - Start     ######################################",
            "##  PLEASE DO NOT UPDATE OUTSIDE RHQ!!! ######################################",
            "JAVA_OPTS_ADDITIONAL=\"",
            "JAVA_OPTS=\"$JAVA_OPTS $JAVA_OPTS_ADDITIONAL\"",
            "##  JAVA_OPTS (set via RHQ) - End       ######################################" };

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
     * Windows specific JAVA_OPTS configuration handler.
     *
     */
    public static class WindowsConfiguration extends AdditionalJavaOptsConfig {
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
     * Adds JAVA_OPTS setting to the config file. The code will attempt
     * to merge/update existing content if detected at the top of the file.
     * RHQ content is added only at the top of the file.
     *
     * @param configFile
     * @param javaOptsContent
     * @throws Exception
     */
    public void updateJavaOptsConfig(File configFile, String javaOptsContent) throws Exception {

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

        javaOptsContent = javaOptsContent.replace(this.getEndOfSequenceCharacter() + "", "");
        String javaOptsNewContent = this.getSequence()[this.getContentIndex()] + javaOptsContent
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
                    fileContent.add(this.getSequence()[index] + javaOptsContent + this.getEndOfSequenceCharacter());
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
     * Discover JAVA_OPTS setting from to the config file. The code will attempt
     * to detect only RHQ set JAVA_OPTS content that is top of the file.
     *
     * @param configFile
     * @throws Exception
     */
    public String discoverJavaOptsConfig(File configFile)
        throws Exception {

        String line;
        String javaOptsValue = null;

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        List<String> fileContent = new ArrayList<String>();
        try {
            line = null;

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

        for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
            if (!fileContent.get(potentialSequenceStart + sequenceIndex).trim()
                .startsWith(this.getSequence()[sequenceIndex])) {
                goodSequence = false;
                break;
            }

            if (sequenceIndex == this.getContentIndex()) {
                javaOptsValue = fileContent.get(potentialSequenceStart + sequenceIndex)
                    .replace(this.getSequence()[sequenceIndex], "");
                javaOptsValue = javaOptsValue.substring(0, javaOptsValue.lastIndexOf(this.getEndOfSequenceCharacter()));
            }
        }

        if (!goodSequence) {
            javaOptsValue = null;
        } else {
            javaOptsValue = javaOptsValue.replace(this.getEndOfSequenceCharacter() + "", "");
        }

        return javaOptsValue;
    }

    /**
     * Clean the config file of any traces of JAVA_OPTS set via RHQ. If
     * the content is set inadvertently multiple times it will delete it all.
     *
     * @param configFile
     * @param javaOptsConfig
     * @throws Exception
     */
    public void cleanJavaOptsConfig(File configFile) throws Exception {
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

        //remove those spaced too close
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
                    linesToClear.add(potentialLinesToClear.get(index));
                }
            }
        }

        if (linesToClear.size() == 0) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedWriter newContent = new BufferedWriter(new OutputStreamWriter(outputStream));

        String newLineSeparator = System.getProperty("line.separator");

        //Add the rest of config file content
        br = new BufferedReader(new FileReader(configFile));
        try {
            line = null;
            lineNumber = -1;
            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (!linesToClear.isEmpty() && lineNumber == linesToClear.get(0)) {
                    //discard lines, they are guaranteed to exist and match the JAVA_OPTS sequence
                    for (int index = 1; index < this.getSequence().length; index++) {
                        br.readLine();
                        lineNumber++;
                    }

                    linesToClear.remove(0);
                } else {
                    newContent.write(line + newLineSeparator);
                }
            }
        } finally {
            br.close();
            newContent.close();
        }

        FileOutputStream updatedConfigFile = new FileOutputStream(configFile);
        try {
            outputStream.writeTo(updatedConfigFile);
        } finally {
            updatedConfigFile.close();
        }
    }
}
