/*
 * Copyright 2011 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gimcrack.marshalling;

import static org.gimcrack.marshalling.MarshallingDBUtil.MARSHALLING_BASE_DB;
import static org.gimcrack.marshalling.MarshallingDBUtil.MARSHALLING_TEST_DB;
import static org.gimcrack.marshalling.MarshallingDBUtil.generatePathToTestDb;
import static org.gimcrack.marshalling.MarshallingTestUtil.retrieveMarshallingData;
import static org.gimcrack.marshalling.PersistenceUtil.DATASOURCE;
import static org.gimcrack.marshalling.PersistenceUtil.DROOLS_PERSISTENCE_UNIT_NAME;
import static org.gimcrack.marshalling.PersistenceUtil.ENTITY_MANAGER_FACTORY;
import static org.gimcrack.marshalling.PersistenceUtil.cleanUp;
import static org.gimcrack.marshalling.PersistenceUtil.getDatasourceProperties;
import static org.gimcrack.marshalling.PersistenceUtil.setupPoolingDataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.impl.EnvironmentFactory;
import org.gimcrack.marshalling.kie.KieGimcrack;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.TimerJobFactoryOption;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class TestMarshallingUtilsTest {

    private static Logger logger = LoggerFactory.getLogger(TestMarshallingUtilsTest.class);
    
    private static boolean debug = false;
    private KieGimcrack kieGimcrack;
    
    @Before
    public void before() { 
        kieGimcrack = new KieGimcrack();
    } 
    
    @Test
    public void testUnmarshallingMarshalledData() {
        HashMap<String, Object> testContext = null;
        List<MarshalledData> marshalledDataList = null;
        try {
            testContext = initializeMarshalledDataEMF(DROOLS_PERSISTENCE_UNIT_NAME, this.getClass(), true);
            EntityManagerFactory emf = (EntityManagerFactory) testContext.get(ENTITY_MANAGER_FACTORY);
            marshalledDataList = retrieveMarshallingData(emf);
        } finally {
            cleanUp(testContext);
        }

        for (MarshalledData marshalledData : marshalledDataList) {
            String className = marshalledData.marshalledObjectClassName.substring(marshalledData.marshalledObjectClassName
                    .lastIndexOf('.') + 1);
            try {
                kieGimcrack.unmarshallObject(marshalledData);
                logger.debug("- " + className + ": " + marshalledData.getTestMethodAndSnapshotNum());
            } catch (Exception e) {
                logger.debug("X " + className + ": " + marshalledData.getTestMethodAndSnapshotNum());
            }
        }
    }

    @Test
    public void testUnmarshallingSpecificMarshalledData() {
        String testMethodAndSnapNum = "org.drools.persistence.session.RuleFlowGroupRollbackTest.testRuleFlowGroupRollback:1";
        // =
        // "org.kie.timer.integrationtests.TimerAndCalendarTest.testTimerRuleAfterIntReloadSession:1";
        HashMap<String, Object> testContext = initializeMarshalledDataEMF(DROOLS_PERSISTENCE_UNIT_NAME, this.getClass(), true);
        EntityManagerFactory emf = (EntityManagerFactory) testContext.get(ENTITY_MANAGER_FACTORY);
        List<MarshalledData> marshalledDataList = retrieveMarshallingData(emf);
        MarshalledData marshalledData = null;
        for (MarshalledData marshalledDataElement : marshalledDataList) {
            if (testMethodAndSnapNum.equals(marshalledDataElement.getTestMethodAndSnapshotNum())) {
                marshalledData = marshalledDataElement;
            }
        }
    
        try {
            Object unmarshalledObject = kieGimcrack.unmarshallObject(marshalledData);
            assertNotNull(unmarshalledObject);
        } catch (Exception e) {
            e.printStackTrace();
            fail("[" + e.getClass().getSimpleName() + "]: " + e.getMessage());
        } finally {
            cleanUp(testContext);
        }
    }

    @Test
    public void testCompareArrays() {

        int[] testA = { 1, 3 };
        int[] testB = { 1, 3 };

        boolean same = kieGimcrack.compareInstances(testA, testA);
        assertTrue(same);
        printResult(same, testA, testB);

        // setup for test
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();

        KnowledgeBase[] testArrA = { kbase };
        KnowledgeBase[] testArrB = { kbase, null };

        same = kieGimcrack.compareInstances(testArrA, testArrB);
        assertTrue(!same);
        printResult(same, testArrA, testArrB);

        Environment[] testEnvA = { EnvironmentFactory.newEnvironment(), EnvironmentFactory.newEnvironment() };
        Environment[] testEnvB = { EnvironmentFactory.newEnvironment(), EnvironmentFactory.newEnvironment() };

        testEnvA[0].set(DROOLS_PERSISTENCE_UNIT_NAME, DROOLS_PERSISTENCE_UNIT_NAME);

        same = kieGimcrack.compareInstances(testEnvA, testEnvB);
        assertTrue(!same);
        printResult(same, testEnvA, testEnvB);

        PriorityQueue<Short> priShortA = new PriorityQueue<Short>();
        PriorityQueue<Short> priShortB = new PriorityQueue<Short>();

        short[] shortList = { (short) 6, (short) 8, (short) 6, (short) 1, (short) 8, (short) 5, (short) 9 };
        for (int i = 0; i < shortList.length; ++i) {
            priShortA.add(shortList[i]);
            priShortB.add(shortList[i]);
        }
        priShortB.add((short) 0);

        assertFalse("Should be unequal", kieGimcrack.compareInstances(priShortA, priShortB));

        assertEquals(new Short((short) 0), priShortB.poll());
        assertTrue("Should be equal", kieGimcrack.compareInstances(priShortA, priShortB));

    }

    private static void printResult(boolean same, Object objA, Object objB) {
        if (!debug) {
            return;
        }

        logger.debug("Same: " + same);
        String outLine = "a: {";
        for (int i = 0; i < Array.getLength(objA); ++i) {
            outLine += Array.get(objA, i) + ",";
        }
        outLine = outLine.substring(0, outLine.lastIndexOf(",")) + "}";
        logger.debug(outLine);
        outLine = "b: {";
        for (int i = 0; i < Array.getLength(objB); ++i) {
            outLine += Array.get(objB, i) + ",";
        }
        outLine = outLine.substring(0, outLine.lastIndexOf(",")) + "}";
        logger.debug(outLine);
    }

    @Test
    public void testCompareAtomicPrimitives() {
        AtomicInteger objA = new AtomicInteger(-1);
        AtomicInteger objB = new AtomicInteger(-1);

        int a = objA.get();
        int b = objB.get();
        assertFalse("objs?", objA.equals(objB));
        assertTrue("ints?", a == b);
        assertTrue("compare a?", kieGimcrack.compareAtomicPrimitives(objA, objB));

        AtomicBoolean objC = new AtomicBoolean(false);
        AtomicBoolean objD = new AtomicBoolean(false);

        boolean c = objC.get();
        boolean d = objD.get();

        assertFalse("objs?", objC.equals(objD));
        assertTrue("bools?", c == d);
        assertTrue("compare c?", kieGimcrack.compareAtomicPrimitives(objC, objD));
    }

    @Test
    public void testCompareInstances() throws Exception {

        KieBaseConfiguration config = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        config.setOption(EventProcessingOption.STREAM);
        KnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase(config);
        KieSessionConfiguration ksconf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        ksconf.setOption(ClockTypeOption.get("pseudo"));
        ksconf.setOption(TimerJobFactoryOption.get("trackable"));
        
        StatefulKnowledgeSession ksessionA = knowledgeBase.newStatefulKnowledgeSession(ksconf, null);
        StatefulKnowledgeSession ksessionB = knowledgeBase.newStatefulKnowledgeSession(ksconf, null);

        Assert.assertTrue(KieGimcrack.class.getSimpleName() + " is broken!", kieGimcrack.compareInstances(ksessionA, ksessionB));
    }

    public static HashMap<String, Object> initializeMarshalledDataEMF(String persistenceUnitName, Class<?> testClass, boolean useBaseDb) { 
        return initializeMarshalledDataEMF(persistenceUnitName, testClass, useBaseDb, null );
    }
    
    /**
     * This method initializes an EntityManagerFactory with a connection to the base (marshalled) data DB. 
     * This database stores the marshalled data that we expect (for a given drools/jbpm version).
     * @param persistenceUnitName The persistence unit name.
     * @param testClass The class of the (local) unit test running.
     * @return A HashMap<String, Object> containg the datasource and entity manager factory.
     */
    public static HashMap<String, Object> initializeMarshalledDataEMF(String persistenceUnitName, Class<?> testClass, 
            boolean useBaseDb, String baseDbVer ) { 
        HashMap<String, Object> context = new HashMap<String, Object>();
        
        Properties dsProps = getDatasourceProperties();
        String driverClass = dsProps.getProperty("driverClassName");
        if ( ! driverClass.startsWith("org.h2")) {
            return null;
        }
    
        String dbFilePath = generatePathToTestDb(testClass);
        if( useBaseDb ) { 
            dbFilePath = dbFilePath.replace(MARSHALLING_TEST_DB, MARSHALLING_BASE_DB);
            if( baseDbVer != null && baseDbVer.length() > 0) { 
                dbFilePath = dbFilePath.replace("current", baseDbVer);
            }
        }
        
        String jdbcURLBase = dsProps.getProperty("url");
        // trace level file = 0 means that modifying the inode of the db file will _not_ cause a "corrupted" exception
        String jdbcUrl =  jdbcURLBase + dbFilePath;
        
        // Setup the datasource
        PoolingDataSource ds1 = setupPoolingDataSource(dsProps);
        ds1.getDriverProperties().setProperty("url", jdbcUrl );
        ds1.init();
        context.put(DATASOURCE, ds1);
    
        // Setup persistence
        Properties overrideProperties = new Properties();
        overrideProperties.setProperty("hibernate.connection.url", jdbcUrl);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnitName, overrideProperties);
        context.put(ENTITY_MANAGER_FACTORY, emf);
        
        return context;
    }

}
