/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.augeas.config;

import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author Filip Drabek
 *
 */
public class AugeasConfigurationSimple implements AugeasConfiguration{
        
        private String loadPath;
        private int mode;
        private String rootPath;
        private List<AugeasModuleConfig> modules;
        
        public void setLoadPath(String loadPath) {
                this.loadPath = loadPath;
        }

        public void setMode(int mode) {
                this.mode = mode;
        }

        public void setRootPath(String rootPath) {
                this.rootPath = rootPath;
        }

        public void setModules(List<AugeasModuleConfig> modules) {
                this.modules = modules;
        }

        public AugeasConfigurationSimple()
        {
                 modules = new ArrayList<AugeasModuleConfig>();
        }
        
        public String getLoadPath() {
                return loadPath;
        }

        
        public int getMode() {        
                return mode;
        }
        
        public List<AugeasModuleConfig> getModules() {
                return modules;
        }

        
        public String getRootPath() {
                return rootPath;
        }

        public void addModuleConfig(AugeasModuleConfig config)
        {
                if (modules.contains(config))
                        return;
        modules.add(config);        	
        }

		public AugeasModuleConfig getModuleByName(String name) {
			for (AugeasModuleConfig module : modules){
				if (module.getModuletName().equals(name))
					return module;
			}
			return null;
		}
}
