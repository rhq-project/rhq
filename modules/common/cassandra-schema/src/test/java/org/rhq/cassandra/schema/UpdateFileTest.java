package org.rhq.cassandra.schema;

import org.testng.annotations.Test;

@Test
public class UpdateFileTest {

//    public void noBindingOrdered() {
//        UpdateFile updateFile = new UpdateFile("no_binding.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps();
//        Assert.assertEquals(orderedSteps.size(), 4);
//
//        for (int index = 0; index < orderedSteps.size(); index++) {
//            Assert.assertEquals(Integer.parseInt(orderedSteps.get(index)), index);
//        }
//    }

//    public void noBindingNamedSteps() {
//        UpdateFile updateFile = new UpdateFile("no_binding_named_steps.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps();
//        Assert.assertEquals(orderedSteps.size(), 4);
//
//        for (int index = 0; index < orderedSteps.size(); index++) {
//            String step = updateFile.getNamedStep(index + "");
//            Assert.assertEquals(Integer.parseInt(step), index);
//        }
//    }

//    public void noBindingOrderedExtraTags() {
//        UpdateFile updateFile = new UpdateFile("no_binding.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps();
//        Assert.assertEquals(orderedSteps.size(), 4);
//    }

//    public void noBindingOrderedWithUnrelatedProperties() throws Exception {
//        Properties testProperties = new Properties();
//        testProperties.put("random_property_2", "12345");
//        testProperties.put("random_property_1", "67890");
//
//        UpdateFile updateFile = new UpdateFile("no_binding.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps();
//        Assert.assertEquals(orderedSteps.size(), 4);
//
//        for (int index = 0; index < orderedSteps.size(); index++) {
//            Assert.assertEquals(Integer.parseInt(orderedSteps.get(index)), index);
//        }
//    }

    @Test(expectedExceptions = RuntimeException.class)
    public void bindingErrorNoProperties() throws Exception {
        UpdateFile updateFile = new UpdateFile("required_binding.xml");
        updateFile.getOrderedSteps();
    }

//    @Test(expectedExceptions = RuntimeException.class)
//    public void bindingErrorPartialProperties() throws Exception {
//        Properties testProperties = new Properties();
//        testProperties.put("first_property", "0");
//        testProperties.put("second_property", "1");
//
//        UpdateFile updateFile = new UpdateFile("required_binding.xml");
//        updateFile.getOrderedSteps(testProperties);
//    }

    @Test(expectedExceptions = RuntimeException.class)
    public void badFileNoUpdatePlan() {
        UpdateFile updateFile = new UpdateFile("bad_file_1.xml");
        updateFile.getOrderedSteps();
    }

    public void noUpdateSteps() {
        UpdateFile updateFile = new UpdateFile("bad_file_2.xml");
        updateFile.getOrderedSteps();
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void badFileBadXML() {
        UpdateFile updateFile = new UpdateFile("bad_file_3.xml");
        updateFile.getOrderedSteps();
    }

//    public void binding() {
//        Random random = new Random();
//        double randomNumber = random.nextDouble() * random.nextInt();
//
//        Properties testProperties = new Properties();
//        testProperties.put("first_property", "0");
//        testProperties.put("second_property", "1");
//        testProperties.put("third_property", "2");
//        testProperties.put("fourth_property", "3");
//        testProperties.put("fifth_property", randomNumber + "");
//
//        UpdateFile updateFile = new UpdateFile("required_binding.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps(testProperties);
//        Assert.assertEquals(orderedSteps.size(), 4);
//
//        for (int index = 0; index < orderedSteps.size(); index++) {
//            if (index % 2 == 0) {
//                Assert.assertEquals(orderedSteps.get(index), index + "" + randomNumber);
//            } else {
//                Assert.assertEquals(orderedSteps.get(index), index + " testString " + randomNumber + " testString "
//                    + randomNumber);
//            }
//        }
//    }

//    public void bindingNamedSteps() {
//        Random random = new Random();
//        double randomNumber = random.nextDouble() * random.nextInt();
//
//        Properties testProperties = new Properties();
//        testProperties.put("first_property", "0");
//        testProperties.put("second_property", "1");
//        testProperties.put("third_property", "2");
//        testProperties.put("fourth_property", "3");
//        testProperties.put("fifth_property", randomNumber + "");
//
//        UpdateFile updateFile = new UpdateFile("required_binding_named_steps.xml");
//        List<String> orderedSteps = updateFile.getOrderedSteps(testProperties);
//        Assert.assertEquals(orderedSteps.size(), 4);
//
//        for (int index = 0; index < orderedSteps.size(); index++) {
//            String step = updateFile.getNamedStep(index + "", testProperties);
//            if (index % 2 == 0) {
//                Assert.assertEquals(step, index + "" + randomNumber);
//            } else {
//                Assert.assertEquals(step, index + " testString " + randomNumber + " testString " + randomNumber);
//            }
//        }
//    }

//    public void bindingNamedStepPartialProperties() {
//        Random random = new Random();
//        double randomNumber = random.nextDouble() * random.nextInt();
//
//        Properties testProperties = new Properties();
//        testProperties.put("second_property", "1");
//        testProperties.put("fifth_property", randomNumber + "");
//
//        UpdateFile updateFile = new UpdateFile("required_binding_named_steps.xml");
//        String step = updateFile.getNamedStep("1", testProperties);
//        Assert.assertEquals(step, 1 + " testString " + randomNumber + " testString " + randomNumber);
//    }

//    @Test(expectedExceptions = RuntimeException.class)
//    public void bindingNamedStepWrongPartialProperties() {
//        Random random = new Random();
//        double randomNumber = random.nextDouble() * random.nextInt();
//
//        Properties testProperties = new Properties();
//        testProperties.put("first_property", "0");
//        //second_property is actually needed and not first_property
//        testProperties.put("fifth_property", randomNumber + "");
//
//        UpdateFile updateFile = new UpdateFile("required_binding_named_steps.xml");
//        String step = updateFile.getNamedStep("1", testProperties);
//        Assert.assertEquals(step, 1 + " testString " + randomNumber + " testString " + randomNumber);
//    }

//    public void bindingNamedStepNotFound() {
//        UpdateFile updateFile = new UpdateFile("required_binding_named_steps.xml");
//        String step = updateFile.getNamedStep("randomName");
//        Assert.assertNull(step);
//    }
}