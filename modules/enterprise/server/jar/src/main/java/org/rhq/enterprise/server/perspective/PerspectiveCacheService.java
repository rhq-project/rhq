/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.perspective.Perspective;
import org.rhq.core.clientapi.descriptor.perspective.Task;

/**
 * PerspectiveCacheService parses and caches the Perspective definitions in memory. For now if a new perspective is
 * defined this class must be destroyed and reloaded.
 */
public class PerspectiveCacheService {
    private static PerspectiveCacheService INSTANCE;

    private Log log = LogFactory.getLog(PerspectiveCacheService.class.getName());
    private Map<String, Perspective> perspectivesByName = new HashMap<String, Perspective>();
    private Map<String, List<Task>> tasksByContext = new HashMap<String, List<Task>>();

    private PerspectiveCacheService() {
        parseAllPerspectives();
    }

    /**
     * Get the instance of the PerspectiveServiceCache
     *
     * @return PerspectiveServiceCache
     */
    public static PerspectiveCacheService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PerspectiveCacheService();
        }

        return INSTANCE;
    }

    public Map<String, Perspective> getPerspectivesByName() {
        return perspectivesByName;
    }

    public Map<String, List<Task>> getTasksByContext() {
        return tasksByContext;
    }

    /**
     * Parse all the perspectives off disk and cache them in a local Map object for quick lookup.
     */
    private synchronized void parseAllPerspectives() {
        try {
            // The source of these files is:
            // trunk/modules/enterprise/server/jar/src/main/resources/perspectives/
            URL defDirUri = getClass().getClassLoader().getResource("perspectives/");
            log.debug("parseAllPerspectives.URI: " + defDirUri);
            File defDir = new File(defDirUri.toURI());
            File[] files = defDir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File currFile = files[i];
                if (currFile.getAbsolutePath().endsWith(".xml")) {
                    log.debug("parseAllPerspectives.parseAllPerspectives.File: " + currFile);
                    Perspective p = parsePerspective(currFile.toURL());
                    perspectivesByName.put(p.getName(), p);
                    if (p.getTasks() != null) {
                        for (Task t : p.getTasks()) {
                            System.out.println("Working with Task: " + t.getName());
                            if (t.getContext() != null) {
                                System.out.println("t.getContext: " + t.getContext());

                                // Update existing List of relevent items
                                if (tasksByContext.containsKey(t.getContext())) {
                                    System.out.println("t already has context, adding");
                                    List<Task> tasks = tasksByContext.get(t.getContext());
                                    tasks.add(t);
                                } else {
                                    System.out.println("need to create first entry");
                                    List<Task> tasks = new LinkedList<Task>();
                                    tasks.add(t);
                                    tasksByContext.put(t.getContext(), tasks);
                                }
                            }
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            log.error("Error building url to perspective directory.", e);
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            log.error("Error building url to perspective directory.", e);
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            log.error("Error parsing perspective xml", e);
            throw new RuntimeException(e);
        }
    }

    private Perspective parsePerspective(URL path) throws JAXBException {
        log.debug("parsePerspective start.");

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.PERSPECTIVE);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        /*
         * URL perspectiveSchemaUrl = getClass().getClassLoader().getResource( "perpective.xsd");
         *
         * Schema pluginSchema; try { pluginSchema = SchemaFactory.newInstance(
         * XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema( perspectiveSchemaUrl); } catch (SAXException e) { throw new
         * JAXBException("Schema is invalid: " + e.getMessage()); } unmarshaller.setSchema(pluginSchema);
         */

        ValidationEventCollector vec = new ValidationEventCollector();
        unmarshaller.setEventHandler(vec);

        Perspective perspective = (Perspective) unmarshaller.unmarshal(path);

        for (ValidationEvent event : vec.getEvents()) {
            log.error(event.getSeverity() + ":" + event.getMessage() + "    " + event.getLinkedException());
        }

        return perspective;
    }
}