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
package org.jboss.on.common.jbossas;

/**
 * This is a helper class to define paths of a JBoss AS used to pass these to
 * {@link JBPMWorkflowManager}.
 * 
 * @author Lukas Krejci
 */
public class JBossASPaths {

	private String homeDir;
	private String serverDir;

	public JBossASPaths() {

	}

	public JBossASPaths(String homeDir, String serverDir) {
		super();
		this.homeDir = homeDir;
		this.serverDir = serverDir;
	}

	public String getHomeDir() {
		return homeDir;
	}

	public void setHomeDir(String homeDir) {
		this.homeDir = homeDir;
	}

	public String getServerDir() {
		return serverDir;
	}

	public void setServerDir(String serverDir) {
		this.serverDir = serverDir;
	}
}
