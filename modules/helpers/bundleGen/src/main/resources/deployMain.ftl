<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
-->
<#-- @ftlvariable name="props" type="org.rhq.helpers.bundleGen.Props" -->
<?xml version="1.0"?>

<project name="${props.project}" default="main"
         xmlns:rhq="antlib:org.rhq.bundle">

    <rhq:bundle name="${props.bundleName}"
                version="${props.bundleVersion}"
                description="${props.bundleDescription}">

        <rhq:deployment-unit name="fixedForNow"
                             preinstallTarget="preinstall"
                             postinstallTarget="postinstall">

            <rhq:archive name="${props.bundleFile}">
               <#if props.replacePattern??>
                   <rhq:replace>
                      <rhq:fileset>
                         <include name="${props.replacePattern}"/>
                      </rhq:fileset>
                   </rhq:replace>
               </#if>
            </rhq:archive>

        </rhq:deployment-unit>
    </rhq:bundle>

    <!-- needed by ant, do not remove -->
    <target name="main"/>

    <target name="preinstall">
        <echo>Installing to ${r"${rhq.deploy.dir}"}...</echo>
    </target>

    <target name="postinstall">
        <echo>Done installing to ${r"${rhq.deploy.dir}"}.</echo>
    </target>


</project>