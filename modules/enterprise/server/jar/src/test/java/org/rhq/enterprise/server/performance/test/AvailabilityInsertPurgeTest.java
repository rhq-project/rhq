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
package org.rhq.enterprise.server.performance.test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.test.AbstractEJB3PerformanceTest;

import org.testng.annotations.Test;

/**
 * Performance test the availabilities subsystem
 *
 * @author Heiko W. Rupp
 */
@Test(groups = "PERF")
public class AvailabilityInsertPurgeTest extends AbstractEJB3PerformanceTest {

    private final Log log = LogFactory.getLog(AvailabilityInsertPurgeTest.class);

    /*
     * we need to replace the ids in the csv files with the ids that we get back from the
     * databse in relations. So store them as pair <csv-id,new entity-id>
     */
    private Map<Integer,Integer> agentsTranslationTable = new HashMap<Integer,Integer>();
    private Map<Integer,Integer> pluginsTranslationTable = new HashMap<Integer,Integer>();
    private Map<Integer,Integer> resourceTypeTranslationTable = new HashMap<Integer,Integer>();

    private Map<Integer,Integer> childParentTypeMap = new HashMap<Integer, Integer>();

    public void testOne() throws Exception {
        setup();
        startTiming();

        Thread.sleep(1234);

        endTiming();

        commitTimings();

    }

    private void setup() {
        setupAgents();
        setupPlugins();
        setupResourceTypes();
        // TODO set up resources

    }

    private void setupAgents() {
        String descriptorFile = "perftest/agents.csv";
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);
        FileReader fr = null;
        try {
            String fileName = descriptorUrl.getFile();
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return;
        }
        try {
            CSVReader reader = new CSVReader(fr,',','"',1); // Skip 1st line, use " as quote char and , as separator

            List<String[]> lines = reader.readAll();
            System.out.println("# of lines: " + lines.size());
            for (String[] line : lines) {
                if (line[0].startsWith("#"))
                    continue; // comment

                int originalId = Integer.parseInt(line[0]);
                Agent agent = new Agent(line[1],line[2],Integer.parseInt(line[3]),line[5],line[4]);// TODO more information?
                getEntityManager().persist(agent);
                int id = agent.getId();

                agentsTranslationTable.put(originalId,id);


            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

    }

    private void setupPlugins() {
        String descriptorFile = "perftest/plugins.csv";
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);
        FileReader fr = null;
        try {
            String fileName = descriptorUrl.getFile();
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return;
        }
        try {
            CSVReader reader = new CSVReader(fr,',','"',1);

            List<String[]> lines = reader.readAll();
            System.out.println("# of lines: " + lines.size());
            for (String[] line : lines) {
                if (line[0].startsWith("#"))
                    continue; // comment

                int originalId = Integer.parseInt(line[0]);
                Plugin plugin = new Plugin(line[1],line[5],line[6]);
                plugin.setDisplayName(line[2]);
                plugin.setVersion(line[3]);
                plugin.setAmpsVersion(line[4]);
                getEntityManager().persist(plugin);

                int id = plugin.getId();
                pluginsTranslationTable.put(originalId,id);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

    }
    private void setupResourceTypes() {


        // first pull in the parentResourceTypes.csv file to get a mapping for them.
        String descriptorFile = "perftest/parentResourceTypes.csv";
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);
        FileReader fr = null;
        try {
            String fileName = descriptorUrl.getFile();
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return;
        }
        try {
            CSVReader reader = new CSVReader(fr,',','"',1);
            List<String[]> lines = reader.readAll();
            System.out.println("# of lines: " + lines.size());
            for (String[] line: lines) {
                Integer typeId = Integer.parseInt(line[0]);
                Integer parentTypeId = Integer.parseInt(line[1]);
                childParentTypeMap.put(typeId,parentTypeId);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

        // now the ResourceTypes themselves

        descriptorFile = "perftest/resourceTypes.csv";
        descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);
        fr = null;
        try {
            String fileName = descriptorUrl.getFile();
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return;
        }
        try {
            CSVReader reader = new CSVReader(fr,',','"',1);

            List<String[]> lines = reader.readAll();
            System.out.println("# of lines: " + lines.size());
            for (String[] line : lines) {
                if (line[0].startsWith("#"))
                    continue; // comment

                int originalId = Integer.parseInt(line[0]);
                ResourceType parentType = findResourceType(originalId);
                ResourceCategory category = ResourceCategory.valueOf(line[2]);
                ResourceType rt = new ResourceType(line[1],line[3],category,parentType);
                getEntityManager().persist(rt);

                int id = rt.getId();
                resourceTypeTranslationTable.put(originalId,id);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }

    }

    private ResourceType findResourceType(int originalId) {

        if (childParentTypeMap.containsKey(originalId)) {
            int id = childParentTypeMap.get(originalId);
            int translatedId = resourceTypeTranslationTable.get(id);
            ResourceType parentType = getEntityManager().find(ResourceType.class,translatedId);
        }
        return null;
    }
}
