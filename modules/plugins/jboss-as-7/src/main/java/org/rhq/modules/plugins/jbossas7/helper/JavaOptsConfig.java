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
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class JavaOptsConfig {

    private static final String NEW_LINE = System.getProperty("line.separator");

    public abstract String[] getSequence();

    public abstract int getContentIndex();

    public abstract char getEndOfSequenceCharacter();

    /**
     * Linux specific JAVA_OPTS configuration handler.
     *
     */
    public static class JavaOptsConfigurationLinux extends JavaOptsConfig {
        private final String[] sequence = new String[] {
            "##  JAVA_OPTS (set via RHQ) - Start     ######################################",
            "##  PLEASE DO NOT UPDATE OUTSIDE RHQ!!! ######################################",
            "if [ \"x$JAVA_OPTS\" = \"x\" ]; then",
            "JAVA_OPTS=\"",
            "if",
            "##  JAVA_OPTS (set via RHQ) - End       ######################################" };

        private final int contentIndex = 3;

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
    public static class JavaOptsConfigurationWindows extends JavaOptsConfig {
        private final String[] sequence = new String[] {
            "rem ###  JAVA_OPTS (set via RHQ) - Start     ####################################",
            "rem ###  PLEASE DO NOT UPDATE OUTSIDE RHQ!!! ####################################",
            "if \"x%JAVA_OPTS%\" == \"x\" (",
            "set \"JAVA_OPTS=",
            "goto JAVA_OPTS_SET",
            ")",
            "rem ###  JAVA_OPTS (set via RHQ) - End       ####################################" };

        private final int contentIndex = 3;

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

        javaOptsContent = javaOptsContent.replace(this.getEndOfSequenceCharacter()+"", "");

        String line;
        int lineNumber;

        int lineToUpdate = -1;
        boolean identicalJavaOpts = false;

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        try {
            line = null;
            lineNumber = 0;

            //skip empty lines
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    break;
                }

                lineNumber++;
            }

            boolean goodSequence = false;
            identicalJavaOpts = false;

            //the sequence should be at the top of file
            //after skipping all the empty lines check if the sequence is the RHQ sequence
            goodSequence = true;
            for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
                if (line != null) {
                    if (!line.trim().startsWith(this.getSequence()[sequenceIndex])) {
                        goodSequence = false;
                        identicalJavaOpts = false;
                        break;
                    }

                    if (sequenceIndex == this.getContentIndex()) {
                        String javaOptsNewContent = this.getSequence()[this.getContentIndex()] + javaOptsContent + this.getEndOfSequenceCharacter();
                        if (line.trim().equals(javaOptsNewContent)) {
                            identicalJavaOpts = true;
                        }
                    }
                } else {
                    goodSequence = false;
                    break;
                }

                line = br.readLine();
            }

            if (goodSequence) {
                lineToUpdate = lineNumber + this.getContentIndex();
            } else {
                lineToUpdate = -1;
            }
        } finally {
            br.close();
        }

        if (identicalJavaOpts) {
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedWriter newContent = new BufferedWriter(new OutputStreamWriter(outputStream));

        //add new content at the top of the file if RHQ JAVA_OPTS never found
        if (lineToUpdate < 0) {
            for (int index = 0; index < this.getSequence().length; index++) {
                if (this.getContentIndex() == index) {
                    newContent.write(this.getSequence()[index] + javaOptsContent + this.getEndOfSequenceCharacter()
                        + NEW_LINE);
                } else {
                    newContent.write(this.getSequence()[index] + NEW_LINE);
                }
            }
        }

        //add the rest of config file content
        br = new BufferedReader(new FileReader(configFile));
        try {
            line = null;
            lineNumber = -1;
            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (lineNumber == lineToUpdate) {
                    newContent.write(this.getSequence()[this.getContentIndex()] + javaOptsContent
                        + this.getEndOfSequenceCharacter() + NEW_LINE);
                } else {
                    newContent.write(line + NEW_LINE);
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
        try {
            line = null;

            //skip empty lines
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    break;
                }
            }

            boolean goodSequence = false;

            //the sequence should be at the top of file
            //after skipping all the empty lines check if the sequence is the RHQ sequence
            goodSequence = true;
            for (int sequenceIndex = 0; sequenceIndex < this.getSequence().length; sequenceIndex++) {
                if (line != null) {
                    if (!line.trim().startsWith(this.getSequence()[sequenceIndex])) {
                        goodSequence = false;
                        break;
                    }

                    if (sequenceIndex == this.getContentIndex()) {
                        javaOptsValue = line.replace(this.getSequence()[sequenceIndex], "");
                        javaOptsValue = javaOptsValue.substring(0, javaOptsValue.lastIndexOf('"'));
                    }
                } else {
                    goodSequence = false;
                    break;
                }

                line = br.readLine();
            }

            if (!goodSequence) {
                javaOptsValue = null;
            } else {
                javaOptsValue = javaOptsValue.replace(this.getEndOfSequenceCharacter() + "", "");
            }
        } finally {
            br.close();
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
