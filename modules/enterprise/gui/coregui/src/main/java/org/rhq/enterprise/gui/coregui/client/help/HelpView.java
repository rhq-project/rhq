/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.help;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.enterprise.gui.coregui.client.components.AboutModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.view.AbstractSectionedLeftNavigationView;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationItem;
import org.rhq.enterprise.gui.coregui.client.components.view.NavigationSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewFactory;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * The Help top-level view.
 *
 * @author Jay Shaughnessy
 */
public class HelpView extends AbstractSectionedLeftNavigationView {
    public static final ViewName VIEW_ID = new ViewName("Help", MSG.common_title_help());

    private static final ViewName SECTION_DOC_VIEW_ID = new ViewName("Documentation", MSG
        .view_helpSection_documentation());
    private static final ViewName SECTION_TUTORIAL_VIEW_ID = new ViewName("Tutorial", MSG.view_helpSection_tutorial());

    public HelpView() {
        // This is a top level view, so our locator id can simply be our view id.
        super(VIEW_ID.getName());
    }

    @Override
    protected List<NavigationSection> getNavigationSections() {
        List<NavigationSection> sections = new ArrayList<NavigationSection>();

        NavigationSection docSection = buildDocSection();
        sections.add(docSection);

        NavigationSection tutorialSection = buildTutorialSection();
        sections.add(tutorialSection);

        return sections;
    }

    @Override
    protected HTMLFlow defaultView() {
        String contents = "<h1>" + MSG.common_title_help() + "</h1>\n" + MSG.view_helpTop_description();
        HTMLFlow flow = new HTMLFlow(contents);
        flow.setPadding(20);
        return flow;
    }

    private NavigationSection buildDocSection() {

        NavigationItem aboutItem = new NavigationItem(new ViewName("AboutBox", MSG.view_help_docAbout()),
            "[SKIN]/../actions/help.png", new ViewFactory() {
                public Canvas createView() {
                    AboutModalWindow aboutModalWindow = new AboutModalWindow();
                    aboutModalWindow.show();
                    return aboutModalWindow;
                }
            });

        NavigationItem docItem = new NavigationItem(new ViewName("TOC", MSG.view_help_docToc()),
            "[SKIN]/../headerIcons/document.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId("TOC"), "http://www.rhq-project.org/display/JOPR2/Home");
                }
            });

        NavigationItem faqItem = new NavigationItem(new ViewName("FAQ", MSG.view_help_docFaq()),
            "[SKIN]/../headerIcons/document.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId("FAQ"), "http://www.rhq-project.org/display/JOPR2/FAQ");
                }
            });

        return new NavigationSection(SECTION_DOC_VIEW_ID, aboutItem, docItem, faqItem);
    }

    private NavigationSection buildTutorialSection() {

        NavigationItem searchItem = new NavigationItem(new ViewName("TutorialSearch", MSG.view_help_tutorialSearch()),
            "[SKIN]/../actions/help.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId("TutorialSearch"),
                        "http://www.rhq-project.org/display/JOPR2/Search");
                }
            });

        NavigationItem dynaGroupItem = new NavigationItem(new ViewName("TutorialDynaGroup", MSG
            .view_help_tutorialDynaGroup()), "[SKIN]/../actions/help.png", new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(
                    extendLocatorId("TutorialDynaGroup"),
                    "http://docs.redhat.com/docs/en-US/JBoss_Operations_Network/2.4/html/Basic_Admin_Guide/sect-Basic_Admin_Guide-Managing_Groups.html#sect-Basic_Admin_Guide-Managing_Groups-Tutorial");
            }
        });

        NavigationItem demoAllItem = new NavigationItem(new ViewName("DemoAll", MSG.view_help_tutorialDemoAll()),
            "[SKIN]/../headerIcons/document.png", new ViewFactory() {
                public Canvas createView() {
                    return new FullHTMLPane(extendLocatorId("DemoAll"),
                        "http://www.rhq-project.org/display/JOPR2/Demos");
                }
            });

        NavigationItem demoBundleItem = new NavigationItem(new ViewName("DemoBundle", MSG
            .view_help_tutorialDemoBundle()), "[SKIN]/../headerIcons/document.png", new ViewFactory() {
            public Canvas createView() {
                return new FullHTMLPane(extendLocatorId("DemoBundle"),
                    "http://mazz.fedorapeople.org/demos/provisioning_beta/prov-beta.htm");
            }
        });

        return new NavigationSection(SECTION_TUTORIAL_VIEW_ID, searchItem, dynaGroupItem, demoAllItem, demoBundleItem);
    }
}