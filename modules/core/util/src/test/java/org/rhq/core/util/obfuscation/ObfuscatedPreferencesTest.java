/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

package org.rhq.core.util.obfuscation;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.prefs.Preferences;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.util.obfuscation.ObfuscatedPreferences.Restricted;

/**
 * @author Stefan Negrea
 */
@Test
public class ObfuscatedPreferencesTest {

    public interface EmptyObfuscatedConstants {

    }

    public interface SingleContants {
        static final public String TEST_PROPERTY = "com.test.one";
    }

    public interface SingleObfuscatedConstants {

        @Restricted
        static final public String TEST_PROPERTY = "com.test.two";
    }

    public void testPatternCaseSensitivity() throws Exception {
        boolean actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("restricted::asdfasd");
        String actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("restricted::asdfasd");
        Assert.assertEquals(actualIsRestricted, true);
        Assert.assertEquals(actualValue, "asdfasd");

        actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("RESTRICTED::123asdf");
        actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("RESTRICTED::123asdf");
        Assert.assertEquals(actualIsRestricted, true);
        Assert.assertEquals(actualValue, "123asdf");

        actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("ReSTRiCTED::456asd");
        actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("RESTRICTED::456asd");
        Assert.assertEquals(actualIsRestricted, true);
        Assert.assertEquals(actualValue, "456asd");
    }

    public void testPatternMatching() throws Exception {
        boolean actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("restricteds::asdfasd");
        Assert.assertEquals(actualIsRestricted, false);

        actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("RESTRICTED?::asdfasd");
        Assert.assertEquals(actualIsRestricted, false);

        actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("aReSTRiCTED::asdfasd");
        Assert.assertEquals(actualIsRestricted, false);

        actualIsRestricted = ObfuscatedPreferences.RestrictedFormat.isRestrictedFormat("RESTRICTED::asdfasd");
        Assert.assertEquals(actualIsRestricted, true);
    }

    public void testPatternMatchingValue() throws Exception {
        String actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("restricted::asdfasd");
        Assert.assertEquals(actualValue, "asdfasd");

        actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("restricted:: ::asdfasd");
        Assert.assertEquals(actualValue, " ");

        actualValue = ObfuscatedPreferences.RestrictedFormat.retrieveValue("restricted:: asdfasd");
        Assert.assertEquals(actualValue, " asdfasd");
    }

    public void testSimpleNonObfuscatedPut() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, EmptyObfuscatedConstants.class);

        //exercise code
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, testValue);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(1)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY), eq(testValue));
        verifyNoMoreInteractions(mockPreferences);
    }

    public void testSimpleNonObfuscatedGet() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            testValue);

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, EmptyObfuscatedConstants.class);

        //exercise code
        String actualValue = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, testValue);
    }

    public void testSimpleObfuscatedNotRestrictedGet() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = PicketBoxObfuscator.encode("123Test");

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            null, testValue);

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, EmptyObfuscatedConstants.class);

        //exercise code
        String actualValue = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, testValue);
    }

    public void testSimpleAttemptToPutRestricted() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = PicketBoxObfuscator.encode("123Test");

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + testValue);

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, EmptyObfuscatedConstants.class);

        //exercise code
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, "RESTRICTED::" + testValue);
        String actualValue = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + testValue));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, PicketBoxObfuscator.decode(testValue));
    }

    public void testSimpleAttemptToPutRestrictedTwice() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = PicketBoxObfuscator.encode("123Test");
        final String testValue2 = PicketBoxObfuscator.encode("Test456");

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            null,
            "RESTRICTED::" + testValue,
            "RESTRICTED::" + testValue2);

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, EmptyObfuscatedConstants.class);

        //exercise code
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, "RESTRICTED::" + testValue);
        String actualValue1 = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, testValue2);
        String actualValue2 = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(3)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + testValue));
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + testValue2));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue1, PicketBoxObfuscator.decode(testValue));
        Assert.assertEquals(actualValue2, PicketBoxObfuscator.decode(testValue2));
    }

    public void testSimpleObfuscatedPut() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[]{});
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(null);

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, SingleObfuscatedConstants.class);

        //exercise code
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, testValue);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(1)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValue)));
        verifyNoMoreInteractions(mockPreferences);
    }

    public void testSimpleObfuscatedValue() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue));

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, SingleObfuscatedConstants.class);

        //exercise code
        test.put(SingleObfuscatedConstants.TEST_PROPERTY, testValue);
        String actualValue = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValue)));
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).keys();
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue,testValue);
    }

    public void testIntialValueNotObfuscated() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            testValue).thenReturn(
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue));

        //create object to test and inject required dependencies
        ObfuscatedPreferences test = new ObfuscatedPreferences(mockPreferences, SingleObfuscatedConstants.class);

        //exercise code
        String actualValue = test.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValue)));
        //2 = 1 time from the constructor and then 1 time from the actual test
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, testValue);
    }

    public void testIntialValueObfuscated() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue));

        //create object to test and inject required dependencies
        ObfuscatedPreferences testObject = new ObfuscatedPreferences(mockPreferences, SingleObfuscatedConstants.class);

        //exercise code
        String actualValue = testObject.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, never()).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValue)));
        //2 = 1 time from the constructor and then 1 time from the actual test
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, testValue);
    }

    public void testIntialValueBadlyObfuscated() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleObfuscatedConstants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + testValue).thenReturn("RESTRICTED::" + PicketBoxObfuscator.encode(testValue));

        //create object to test and inject required dependencies
        ObfuscatedPreferences testObject = new ObfuscatedPreferences(mockPreferences, SingleObfuscatedConstants.class);

        //exercise code
        String actualValue = testObject.get(SingleObfuscatedConstants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        verify(mockPreferences, times(1)).put(eq(SingleObfuscatedConstants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValue)));
        //2 = 1 time from the constructor and then 1 time from the actual test
        verify(mockPreferences, times(2)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue, testValue);
    }

    public void testIntialValueObfuscatedButNotRestricted() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";
        final String testDefaultValue = "Test456";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleContants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleContants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue));

        //create object to test and inject required dependencies
        ObfuscatedPreferences testObject = new ObfuscatedPreferences(mockPreferences, SingleContants.class);

        //exercise code
        String actualValue1 = testObject.get(SingleContants.TEST_PROPERTY, null);
        String actualValue2 = testObject.get(SingleObfuscatedConstants.TEST_PROPERTY, testDefaultValue);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        //2 = 1 time from the constructor and then 1 time from the actual test
        verify(mockPreferences, times(2)).get(eq(SingleContants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).get(eq(SingleObfuscatedConstants.TEST_PROPERTY), isNull(String.class));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue1, testValue);
        Assert.assertEquals(actualValue2, testDefaultValue);
    }

    public void testIntialValueObfuscatedButNotRestrictedPut() throws Exception {
        //setup mocks
        Preferences mockPreferences = mock(Preferences.class);

        //setup values to be used
        final String testValue = "123Test";
        final String testValueAfter = "Test456";

        //setup mocked replies
        when(mockPreferences.keys()).thenReturn(new String[] { SingleContants.TEST_PROPERTY });
        when(mockPreferences.get(eq(SingleContants.TEST_PROPERTY), isNull(String.class))).thenReturn(
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue),
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValue),
            "RESTRICTED::" + PicketBoxObfuscator.encode(testValueAfter));

        //create object to test and inject required dependencies
        ObfuscatedPreferences testObject = new ObfuscatedPreferences(mockPreferences, SingleContants.class);

        //exercise code
        String actualValue1 = testObject.get(SingleContants.TEST_PROPERTY, null);
        testObject.put(SingleContants.TEST_PROPERTY, testValueAfter);
        String actualValue2 = testObject.get(SingleContants.TEST_PROPERTY, null);

        //verify assertions
        verify(mockPreferences, times(1)).keys();
        //2 = 1 time from the constructor and then 2 times from the actual test
        verify(mockPreferences, times(3)).get(eq(SingleContants.TEST_PROPERTY), isNull(String.class));
        verify(mockPreferences, times(1)).put(eq(SingleContants.TEST_PROPERTY),
            eq("RESTRICTED::" + PicketBoxObfuscator.encode(testValueAfter)));
        verifyNoMoreInteractions(mockPreferences);

        Assert.assertEquals(actualValue1, testValue);
        Assert.assertEquals(actualValue2, testValueAfter);
    }
}
