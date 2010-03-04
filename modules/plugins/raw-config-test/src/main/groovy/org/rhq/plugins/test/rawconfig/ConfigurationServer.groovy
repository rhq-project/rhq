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

package org.rhq.plugins.test.rawconfig

import org.rhq.core.domain.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration
import org.rhq.core.domain.configuration.PropertySimple
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.configuration.RawConfiguration

class ConfigurationServer {

  ResourceContext resourceContext

  def ant = new AntBuilder()

  /**
   * Returns a list of the simple properties having the specified names that are found in configFile. If no
   * properties are found, then an empty list is returned. This method assumes that the properties are strings and as
   * such does not handle other simple types.
   */
  def loadSimpleProperties(File configFile, List propertyNames) {
    def properties = []
    def propertiesConfiguration = new PropertiesConfiguration(configFile)

    propertyNames.each { properties << new PropertySimple(it, propertiesConfiguration.getString(it)) }

    return properties
  }


  /**
   * Makes a copy of the current configuration file found on disk. The backup has the original name with the extension,
   * '.orig' tacked onto the end. Then the configuration file is overwritten with the contents of rawConfiguration.
   */
  def backupAndSave(RawConfiguration rawConfiguration) {
    ant.copy(file: rawConfiguration.path, tofile: "${rawConfiguration.path}.orig")
    ant.delete(file: rawConfiguration.path)

    def file = new File(rawConfiguration.path)
    file.createNewFile()
    file << rawConfiguration.contents
  }

  def pauseForRawUpdateIfDelaySet() {
    def delay = resourceContext.pluginConfiguration.getSimple("rawUpdateDelay")
    if (delay.longValue) {
      Thread.sleep(delay.longValue)
    }
  }

  def pauseForStructuredUpdateIfDelaySet() {
    def delay = resourceContext.pluginConfiguration.getSimple("structuredUpdateDelay")
    if (delay.longValue) {
      Thread.sleep(delay.longValue)
    }
  }

}
