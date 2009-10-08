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
package org.rhq.core.util;

import mazz.i18n.Msg;
import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NMessages;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * I18N resource bundle keys that identify the messages needed by the util module.
 *
 * @author John Mazzitelli
 */
@I18NResourceBundle(baseName = "util-messages", defaultLocale = "en")
public interface UtilI18NResourceKeys {
    Msg.BundleBaseName BUNDLE_BASE_NAME = new Msg.BundleBaseName("util-messages");
    Msg MSG = new Msg(BUNDLE_BASE_NAME);

    @I18NMessages( { @I18NMessage("The output directory [{0}] does not exist"),
        @I18NMessage(value = "Das Ausgabeverzeichnis [{0}] existiert nicht", locale = "de") })
    String PROCESS_EXEC_OUTPUT_DIR_DOES_NOT_EXIST = "ProcessExec.output-dir-does-not-exist";

    @I18NMessages( { @I18NMessage("The output directory [{0}] is not a valid directory"),
        @I18NMessage(value = "Das Ausgabeverzeichnis [{0}] ist kein gültiges Verzeichnis", locale = "de") })
    String PROCESS_EXEC_OUTPUT_DIR_INVALID = "ProcessExec.output-dir-invalid";

    @I18NMessages( { @I18NMessage("The output file [{0}] is really a directory"),
        @I18NMessage(value = "[{0}] ist keine Datei sondern ein Verzeichnis", locale = "de") })
    String PROCESS_EXEC_OUTPUT_FILE_IS_DIR = "ProcessExec.output-file-is-dir";

    @I18NMessages( { @I18NMessage("Failed to create the output file [{0}]"),
        @I18NMessage(value = "Kann die Ausgabedatei [{0}] nicht erzeugen", locale = "de") })
    String PROCESS_EXEC_OUTPUT_FILE_CREATION_FAILURE = "ProcessExec.output-file-creation-failure";

    @I18NMessages( { @I18NMessage("Must specify both or neither of input directory/input file: [{0}]") })
    String PROCESS_EXEC_INPUT_PARAMS_INVALID = "ProcessExec.input-params-invalid";

    @I18NMessages( { @I18NMessage("The input directory [{0}] does not exist"),
        @I18NMessage(value = "Das Eingabeverzeichnis [{0}] existiert nicht", locale = "de") })
    String PROCESS_EXEC_INPUT_DIR_DOES_NOT_EXIST = "ProcessExec.input-dir-does-not-exist";

    @I18NMessages( { @I18NMessage("The input directory [{0}] is not a valid directory"),
        @I18NMessage(value = "Das Eingabeverzeichnis [{0}] ist kein gültiges Verzeichnis", locale = "de") })
    String PROCESS_EXEC_INPUT_DIR_INVALID = "ProcessExec.input-dir-invalid";

    @I18NMessages( { @I18NMessage("The input file [{0}] does not exist"),
        @I18NMessage(value = "Die Eingabedatei [{0}] ist nicht vorhanden", locale = "de") })
    String PROCESS_EXEC_INPUT_FILE_DOES_NOT_EXIST = "ProcessExec.input-file-does-not-exist";

    @I18NMessages( { @I18NMessage("The input file [{0}] is not readable"),
        @I18NMessage(value = "Die Eingabedatei [{0}] ist nicht lesbar", locale = "de") })
    String PROCESS_EXEC_INPUT_FILE_UNREADABLE = "ProcessExec.input-file-unreadable";

    @I18NMessages( { @I18NMessage("The input file [{0}] is really a directory"),
        @I18NMessage(value = "[{0}] ist keine Datei sondern ein Verzeichnis ", locale = "de") })
    String PROCESS_EXEC_INPUT_FILE_IS_DIR = "ProcessExec.input-file-is-dir";

    @I18NMessages( { @I18NMessage("The program to execute [{0}] does not exist"),
        @I18NMessage(value = "Das auszuführende Programm [{0}] wurde nicht gefunden", locale = "de") })
    String PROCESS_EXEC_PROGRAM_DOES_NOT_EXIST = "ProcessExec.program-does-not-exist";

    @I18NMessages( { @I18NMessage("The working directory [{0}] does not exist"),
        @I18NMessage(value = "Das Arbeitsverzeichnis [{0}] ist nicht vorhanden", locale = "de") })
    String PROCESS_EXEC_WORKING_DIR_DOES_NOT_EXIST = "ProcessExec.working-dir-does-not-exist";

    @I18NMessages( { @I18NMessage("Environment variable is not in the form name=value: {0}"),
        @I18NMessage(value = "Die Umgebungsvariable ist nicht in der Form Name=Wert: {0}", locale = "de") })
    String START_COMMAND_ENV_VAR_BAD_FORMAT = "ProcessToStart.env-var-bad-format";
}