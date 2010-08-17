/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.helpers.bundleGen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  TODO: Document this
 * @author Heiko W. Rupp
 */
public class Props {

   private final Log log = LogFactory.getLog(Props.class);


   private String project;
   private String bundleName;
   private String bundleVersion;
   private String bundleDescription;
   private String bundleFile;
   private String replacePattern;
   private String contentDir;

   public String getProject() {
      return project;
   }

   public void setProject(String project) {
      this.project = project;
   }

   public String getBundleName() {
      return bundleName;
   }

   public void setBundleName(String bundleName) {
      this.bundleName = bundleName;
   }

   public String getBundleVersion() {
      return bundleVersion;
   }

   public void setBundleVersion(String bundleVersion) {
      this.bundleVersion = bundleVersion;
   }

   public String getBundleDescription() {
      return bundleDescription;
   }

   public void setBundleDescription(String bundleDescription) {
      this.bundleDescription = bundleDescription;
   }

   public String getBundleFile() {
      return bundleFile;
   }

   public void setBundleFile(String bundleFile) {
      this.bundleFile = bundleFile;
   }

   public String getReplacePattern() {
      return replacePattern;
   }

   public void setReplacePattern(String replacePattern) {
      this.replacePattern = replacePattern;
   }

   public String getContentDir() {
      return contentDir;
   }

   public void setContentDir(String contentDir) {
      this.contentDir = contentDir;
   }
}

