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
package org.rhq.enterprise.server.content.metadata.test;

import java.util.List;
import javax.persistence.EntityManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.content.ContentSourceType;

/**
 * Test the handling on Plugin updates / hotdeployments etc.
 *
 * @author John Mazzitelli
 */
@Test
public class ContentSourcePluginHandlingTest extends TestBase {
    @AfterMethod
    @SuppressWarnings("unchecked")
    protected void afterMethod() throws Exception {
        // I tried and tried to get it to work where I create a single tx and just rollback
        // after the test but I could not get the entity manager/tx used by my SLSBs to be the
        // same as used in my test.  So, I had to just let the SLSB create its own entity manager/tx
        // and commit.  I will clean up the tests here.
        getTransactionManager().begin();

        EntityManager em = getEntityManager();
        List<ContentSourceType> list = em.createQuery("select c from ContentSourceType c where c.name like 'test%'")
            .getResultList();

        for (ContentSourceType doomed : list) {
            em.remove(doomed);
        }

        getTransactionManager().commit();
    }

    public void testUpdateContentSourceTypes() throws Throwable {
        ContentSourceType type1;
        ContentSourceType type2;
        ContentSourceType type3;
        ContentSourceType type4;
        ConfigurationDefinition def;

        try {
            registerPlugin("./test/metadata/content-source-update-v1.xml");
            type1 = getContentSourceType("testContentSourceType1");
            type2 = getContentSourceType("testContentSourceType2");
            type3 = getContentSourceType("testContentSourceType3");
            type4 = getContentSourceType("testContentSourceType4");

            assert type1 != null;
            assert type2 != null;
            assert type3 == null;
            assert type4 != null;

            assert type1.getId() > 0;
            assert type1.getName().equals("testContentSourceType1");
            assert type1.getDisplayName().equals("displayName1");
            assert type1.getDescription().equals("description1");
            assert type1.getContentSourceApiClass().equals("org.abc.ApiClass1");
            def = type1.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop1").getType() == PropertySimpleType.DIRECTORY;
            assert def.getPropertyDefinitionSimple("prop1").isRequired() == true;

            assert type2.getId() > 0;
            assert type2.getName().equals("testContentSourceType2");
            assert type2.getDisplayName().equals("displayName2");
            assert type2.getDescription().equals("description2");
            assert type2.getContentSourceApiClass().equals("org.abc.ApiClass2");
            def = type2.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop2").getType() == PropertySimpleType.INTEGER;
            assert def.getPropertyDefinitionSimple("prop2").isRequired() == false;

            assert type4.getId() > 0;
            assert type4.getName().equals("testContentSourceType4");
            assert type4.getDisplayName().equals("displayName4");
            assert type4.getDescription().equals("description4");
            assert type4.getContentSourceApiClass().equals("org.abc.ApiClass4");
            def = type4.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop4").getType() == PropertySimpleType.FILE;
            assert def.getPropertyDefinitionSimple("prop4").isRequired() == false;

            // now hot deploy a new version of that plugin
            registerPlugin("./test/metadata/content-source-update-v2.xml");
            type1 = getContentSourceType("testContentSourceType1");
            type2 = getContentSourceType("testContentSourceType2");
            type3 = getContentSourceType("testContentSourceType3");
            type4 = getContentSourceType("testContentSourceType4");

            assert type1 != null;
            assert type2 == null;
            assert type3 != null;
            assert type4 != null;

            assert type1.getId() > 0;
            assert type1.getName().equals("testContentSourceType1");
            assert type1.getDisplayName().equals("displayName1");
            assert type1.getDescription().equals("description1");
            assert type1.getContentSourceApiClass().equals("org.abc.ApiClass1");
            def = type1.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop1").getType() == PropertySimpleType.DIRECTORY;
            assert def.getPropertyDefinitionSimple("prop1").isRequired() == true;

            assert type3.getId() > 0;
            assert type3.getName().equals("testContentSourceType3");
            assert type3.getDisplayName().equals("displayName3");
            assert type3.getDescription().equals("description3");
            assert type3.getContentSourceApiClass().equals("org.abc.ApiClass3");
            def = type3.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop3").getType() == PropertySimpleType.STRING;
            assert def.getPropertyDefinitionSimple("prop3").isRequired() == true;

            assert type4.getId() > 0;
            assert type4.getName().equals("testContentSourceType4");
            assert type4.getDisplayName().equals("displayName4changed");
            assert type4.getDescription().equals("description4changed");
            assert type4.getContentSourceApiClass().equals("org.abc.ApiClass4changed");
            def = type4.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 2;
            assert def.getPropertyDefinitionSimple("prop4_1").getType() == PropertySimpleType.INTEGER;
            assert def.getPropertyDefinitionSimple("prop4_1").isRequired() == false;
            assert def.getPropertyDefinitionSimple("prop4_2").getType() == PropertySimpleType.BOOLEAN;
            assert def.getPropertyDefinitionSimple("prop4_2").isRequired() == false;

            // Now try the other way round - our first set of asserts from before should again pass
            registerPlugin("./test/metadata/content-source-update-v1.xml");
            type1 = getContentSourceType("testContentSourceType1");
            type2 = getContentSourceType("testContentSourceType2");
            type3 = getContentSourceType("testContentSourceType3");
            type4 = getContentSourceType("testContentSourceType4");

            assert type1 != null;
            assert type2 != null;
            assert type3 == null;
            assert type4 != null;

            assert type1.getId() > 0;
            assert type1.getName().equals("testContentSourceType1");
            assert type1.getDisplayName().equals("displayName1");
            assert type1.getDescription().equals("description1");
            assert type1.getContentSourceApiClass().equals("org.abc.ApiClass1");
            def = type1.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop1").getType() == PropertySimpleType.DIRECTORY;
            assert def.getPropertyDefinitionSimple("prop1").isRequired() == true;

            assert type2.getId() > 0;
            assert type2.getName().equals("testContentSourceType2");
            assert type2.getDisplayName().equals("displayName2");
            assert type2.getDescription().equals("description2");
            assert type2.getContentSourceApiClass().equals("org.abc.ApiClass2");
            def = type2.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop2").getType() == PropertySimpleType.INTEGER;
            assert def.getPropertyDefinitionSimple("prop2").isRequired() == false;

            assert type4.getId() > 0;
            assert type4.getName().equals("testContentSourceType4");
            assert type4.getDisplayName().equals("displayName4");
            assert type4.getDescription().equals("description4");
            assert type4.getContentSourceApiClass().equals("org.abc.ApiClass4");
            def = type4.getContentSourceConfigurationDefinition();
            assert def.getPropertyDefinitions().size() == 1;
            assert def.getPropertyDefinitionSimple("prop4").getType() == PropertySimpleType.FILE;
            assert def.getPropertyDefinitionSimple("prop4").isRequired() == false;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
        }
    }
}