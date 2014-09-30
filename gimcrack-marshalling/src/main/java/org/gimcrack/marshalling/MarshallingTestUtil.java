/*
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

import static org.gimcrack.marshalling.MarshallingDBUtil.getListOfBaseDbVers;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;

import org.gimcrack.compare.CompareViaReflectionUtil;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.TransactionManagerServices;


public class MarshallingTestUtil {

    private final static Logger logger = LoggerFactory.getLogger(MarshallingTestUtil.class);
  
    private ObjectSpecificMarshallingActions objectSpecificMarshallingActions = new ObjectSpecificMarshallingActions();
    private EntityManagerFactory emf; 
    
    private static MessageDigest algorithm = null;
    static { 
       if( algorithm == null ) { 
          try {
              algorithm = MessageDigest.getInstance("SHA-1");
          }
          catch(Exception e) { 
              e.printStackTrace();
          }
       }
    }
    
    public void compareMarshallingDataFromTest(CompareViaReflectionUtil compareUtil, String persistenceUnitName) { 
        Class<?> testClass = null;
        try {
            testClass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
        }
        catch(Exception e){ 
            fail("Unable to retrieve class of test running: [" + e.getClass().getSimpleName() + "] " + e.getMessage() );
        }
        compareMarshallingDataFromTest(compareUtil, testClass, persistenceUnitName);
    }

    /**
     * 
     * @param testClass The class that this method is being called from. Because this class might be located within 
     * a jar when this method is called, it's important to be able to access a "local" ClassLoader in order to retrieve
     * the correct path to the test and base marshalling data db. 
     * @param persistenceUnitName The name of the persistence unit being used. 
     */
    public void compareMarshallingDataFromTest(CompareViaReflectionUtil compareUtil, Class<?> testClass, String persistenceUnitName) { 
        if( ! isTestMarshalling() ) { 
            return;
        } 
       
        if( isMakeBaseDatabase() ) { 
            checkMarshalledSnapshots();
            return;
        }
        
        // Retrieve the test data
        List<MarshalledData> testDataList = retrieveMarshallingData(emf);
        assertNotNull("Not marshalled data found for " + testClass.getSimpleName(), 
                testDataList != null && ! testDataList.isEmpty() );
    
        String [] baseDbVersions = getListOfBaseDbVers(testClass);
    
        for( int v = 0; v < baseDbVersions.length; ++v ) { 
            logger.trace("Loading marshalled data from base DB version: [" + baseDbVersions[v] + "]");
            // Retrieve the base data
            List<MarshalledData> baseDataList =  null;
            try { 
                baseDataList = retrieveMarshallingData(emf);
            }
            finally {
                
            }
            assertTrue("No base marshalled data found", baseDataList != null && ! baseDataList.isEmpty() );
    
            // Compare!
            compareTestAndBaseMarshallingData(compareUtil, testClass, testDataList, baseDataList, baseDbVersions[v]);
        }
    }
    
    private boolean isTestMarshalling() { 
       return false; 
    }
    
    private boolean isMakeBaseDatabase() { 
       return false; 
    }

    private void checkMarshalledSnapshots() { 
        logger.trace( "Checking MarshalledData objects saved in base db." );
        List<MarshalledData> baseDataList = retrieveMarshallingData(emf);
        
        assertNotNull("Could not rerieve list of MarshalledData from base db.", baseDataList);
        assertTrue("List of MarshalledData from base db is empty.", ! baseDataList.isEmpty() );
        
        for( MarshalledData marshalledData : baseDataList ) { 
            try { 
                logger.debug( "Unmarshalling snapshot: " + marshalledData.getTestMethodAndSnapshotNum() );
                objectSpecificMarshallingActions.unmarshallObject(marshalledData);
            } catch( Exception e ) { 
                logger.error( e.getClass().getSimpleName() + " thrown while unmarshalling [" 
                        + marshalledData.getTestMethodAndSnapshotNum() + "] data stored in base database", e );
            }
            
        }
        logger.trace( "MarshalledData objects saved in base db:" );
        for( MarshalledData marshalledData : baseDataList ) { 
           logger.trace( "- " + marshalledData); 
        }
    }
    
    @SuppressWarnings("unchecked")
    public static List<MarshalledData> retrieveMarshallingData(EntityManagerFactory emf) { 
        ArrayList<MarshalledData> marshalledDataList = new ArrayList<MarshalledData>();
       
        TransactionManager txm = null;
        try { 
            txm = TransactionManagerServices.getTransactionManager();
            txm.begin();
        }
        catch( Exception e ) { 
            logger.warn("Unable to retrieve marshalled snapshots from marshalling database.");
            e.printStackTrace();
            return marshalledDataList;
        }
    
        List<Object> mdList = null;
        EntityManager em = null;
        try { 
            em = emf.createEntityManager();
            mdList = em.createQuery("SELECT m FROM MarshalledData m").getResultList();
        } finally { 
            if( em != null ) { 
                em.clear();
                em.close();
            }
        }
        
        for( Object resultObject : mdList ) { 
            MarshalledData marshalledData = (MarshalledData) resultObject;
            if( (marshalledData.testMethodName == null || marshalledData.testMethodName.trim().length() == 0) || marshalledData.snapshotNumber == null ) {
               fail("MarshalledData object does not contain the proper identification information.");
            }
            marshalledDataList.add(marshalledData);
            logger.trace("> " + marshalledData);
        }
        
        try {
            txm.commit();
        } catch (Exception e) {
            logger.warn(e.getClass().getSimpleName() + " thrown when retrieving marshalled snapshots.");
            e.printStackTrace();
        } 
        
        return marshalledDataList;
    }

    /**
     * We do the following in this method: <ul>
     * <li>First, we organize the data in order to do a sanity check on the data
     *   <ul><li>see {@link#sanityCheckMarshalledData(Class, HashMap, HashMap)}</li></ul></li>
     * <li>Then, for every test method <i>snapshot</i> that has passed the sanity check:
     *   <ol><li>Retrieve the marshalled data that was created during the <i>base</i> run</li>
     *   <li>Unmarshall the base marshalled data</li>
     *   <li>Retrieve the marshalled data that was created during the <i>test</i> run</li>
     *   <li>Unmarshall the test marshalled data</li>
     *   <li>Lastly, compare the base unmarshalled object to the test unmarshalled object</li>
     *   </ol>
     * </li>
     * </ul>
     * 
     * @param testClass The class of the test that this is being done in (in order to get the local marshalling db path)
     * @param testData A list of the marshalled data created during this test
     * @param baseData A list of the marshalled data created during the base run (or whichever version of the project)
     */
    private void compareTestAndBaseMarshallingData(CompareViaReflectionUtil compareUtil, 
            Class<?> testClass, List<MarshalledData> testData, 
            List<MarshalledData> baseData, String baseDbVersion ) { 
   
        // Extract the marshalled data info for all methods from THIS test (testClass)
        HashMap<String, List<MarshalledData>> testSnapshotsPerTestMap = extractSnapshotsPerTestMethodMap(testClass, testData);
        HashMap<String, List<MarshalledData>> baseSnapshotsPerTestMap = extractSnapshotsPerTestMethodMap(testClass, baseData);
       
        // Check that the tests for which the marshalled data has been retrieved
        //  haven't changed (diff numbers of 
        sanityCheckMarshalledData(testClass, testSnapshotsPerTestMap, baseSnapshotsPerTestMap);
       
        HashMap<String, MarshalledData> testMarshalledDataSnapshotMap = new HashMap<String, MarshalledData>();
        HashMap<String, MarshalledData> baseMarshalledDataSnapshotMap = new HashMap<String, MarshalledData>();
    
        for( String testMethod : testSnapshotsPerTestMap.keySet() ) { 
            for( MarshalledData testMarshalledData : testSnapshotsPerTestMap.get(testMethod) ) { 
                testMarshalledDataSnapshotMap.put(testMarshalledData.getTestMethodAndSnapshotNum(), testMarshalledData);
            }
         }
        for( String testMethod : baseSnapshotsPerTestMap.keySet() ) { 
            for( MarshalledData baseMarshalledData : baseSnapshotsPerTestMap.get(testMethod) ) { 
                baseMarshalledDataSnapshotMap.put(baseMarshalledData.getTestMethodAndSnapshotNum(), baseMarshalledData);
            }
         }

        List<String> errors = new ArrayList<String>();
        for( String testMethodVer : testMarshalledDataSnapshotMap.keySet() ) { 
            logger.trace("Comparing marshalled info for " + testMethodVer);
            Object baseObject = null;
            // Base 
            MarshalledData baseMarshalledData = baseMarshalledDataSnapshotMap.get(testMethodVer);
            try { 
                baseObject = objectSpecificMarshallingActions.unmarshallObject(baseMarshalledData);
            }
            catch( Exception e) {
                String shortTestMethod = testMethodVer.substring(0, testMethodVer.indexOf(':')); 
                shortTestMethod = shortTestMethod.substring( 
                        shortTestMethod.substring(0, shortTestMethod.lastIndexOf('.')
                        ).lastIndexOf('.')+1 );
                
                String shortTestMethodVer = shortTestMethod + testMethodVer.substring(testMethodVer.indexOf(':'));
                logger.error( "[" + e.getClass().getSimpleName() + ": " + e.getMessage() + "] "
                		+ "when unmarshalling [" + shortTestMethodVer + "] in " + baseDbVersion ); 
                continue;
            }
           
            Object testObject = null;
            // Test
            MarshalledData testMarshalledData = testMarshalledDataSnapshotMap.get(testMethodVer);
            try { 
                testObject = objectSpecificMarshallingActions.unmarshallObject(testMarshalledData);
            }
            catch( Exception e) {
                fail("Unable to unmarshall " + baseDbVersion + " data: [" + e.getClass().getSimpleName() + ": " + e.getMessage() + "]");
            }
            
            assertNotNull("Unmarshalled test data resulted in null object!", testObject);
            assertNotNull("Unmarshalled base data resulted in null object!", baseObject);
            
            if( ! compareUtil.compareInstances(baseObject, testObject) ) { 
                String errorMsg =  "Unmarshalled " + baseObject.getClass().getSimpleName() 
                    + " object from " + baseDbVersion + " data is not equal to test unmarshalled object [" 
                    + baseMarshalledData.getTestMethodAndSnapshotNum() + "]";
                errors.add(errorMsg);
            }
        }
        
        if( errors.size() > 0 ) { 
            int i = errors.size()-1;
            for( ; i > 0; --i ) { 
                logger.warn(errors.get(i));
            }
            fail(errors.get(1));
        }
        
        
    }

    /**
     * This class extracts the following data structure: 
     * - For every test method in the given test class: 
     *   - make a list of the MarshalledData snapshots saved in that test method (testMethodMarshalledDataList). 
     * @param testClass The testClass that we're comparing marshalled data for. 
     * @param marshalledDataList A list of MarshalledData objects retrieved (from the test or a base (version) database).
     * @return A HashMap<String (testMethod), List<MarshalledData>> (testMethodMarshalledDataList)> object, described above. 
     */
    private static HashMap<String, List<MarshalledData>> extractSnapshotsPerTestMethodMap(Class<?> testClass, List<MarshalledData> marshalledDataList) { 
        String testClassName = testClass.getName();
        HashMap<String, List<MarshalledData>> snapshotsPerTestMethod = new HashMap<String, List<MarshalledData>>();
        for( MarshalledData marshalledData : marshalledDataList ) {
            if( ! marshalledData.testMethodName.startsWith(testClassName) ) { 
                continue;
            }
            List<MarshalledData> testMethodMarshalledDataList = snapshotsPerTestMethod.get(marshalledData.testMethodName);
            if( testMethodMarshalledDataList == null ) { 
                testMethodMarshalledDataList = new ArrayList<MarshalledData>();
                snapshotsPerTestMethod.put(marshalledData.testMethodName, testMethodMarshalledDataList); 
            }
            testMethodMarshalledDataList.add(marshalledData);  
        }
        return snapshotsPerTestMethod;
    }

    /**
     * We check three things: <ol>
     * <li>Do the snapshots for a test method from this (test) class exist 
     *     in the data saved from this test run?</li>
     * <li>Are there the same number of snapshots for this test method in the data from this test run 
     *     AND the data from the base being used? </li>
     * <li>Do the snapshots for a test method from this (test) class exist
     *     in the data from the base data being used?</li>
     * </ul>
     * <p/>
     * When these checks fail, it will mean the following:<ol>
     * <li>The test method existed when the base was made (in the past), but not in the current version of the code.</li>
     * <li>The test method exists now and existed when the base was made -- but the test has changed in between 
     *     such that there are more snapshots being made during the test.
     *     <ul><li>If the test has changed, we can't trust the information anymore (or can't know that), 
     *             so we don't do that.</li></ul>
     *     </li> 
     * <li>The test method did not exist when this base was made (in the past). If we're using the "current" version
     *     of the base info, this means that we probably should recreate the base.</li>
     * </ol>
     * @param testClass The class of the test for which marshalled data is being compared.
     * @param testSnapshotsPerTestMap The list of MarshalledData snapshots per test (method) from this test run.
     * @param baseSnapshotsPerTestMap The list of MarshalledData snapshots per test (method) from the base db being used.
     */
    private static void sanityCheckMarshalledData(Class<?> testClass, HashMap<String, List<MarshalledData>> testSnapshotsPerTestMap,
        HashMap<String, List<MarshalledData>> baseSnapshotsPerTestMap ) {     
            
        Set<String> testTestMethods = new HashSet<String>(testSnapshotsPerTestMap.keySet());

        List<String> untestableTestMethods = new ArrayList<String>();
        for( String baseTestMethod : baseSnapshotsPerTestMap.keySet() ) { 
            // 1. In this base db, but NOT in this test run!!
            if( ! testTestMethods.contains(baseTestMethod) ) { 
                logger.trace("Marshalled data snapshots for test " + baseTestMethod 
                        + " exist in the base db, but not in the test db generated by this test run!");
                untestableTestMethods.add(baseTestMethod);
            }
            else { 
                // This is just to make sure we can do the next check.
                // If this fails, something really crazy is going on in the code.. (retrieved from the database.. but didn't ??)
               Assert.assertNotNull("Empty list of marshalled data snapshots in base for " + baseTestMethod, 
                       baseSnapshotsPerTestMap.get(baseTestMethod));
               
               // 2. This means that the test has changed somehow (between when the base db was made and this test run).
               int numBaseSnapshotsForTestMethod = baseSnapshotsPerTestMap.get(baseTestMethod).size(); 
               int numTestSnapshotsForTestMethod = testSnapshotsPerTestMap.get(baseTestMethod).size();
               if( numBaseSnapshotsForTestMethod != numTestSnapshotsForTestMethod ) { 
                   logger.error("Has test changed? Unequal number [" + baseSnapshotsPerTestMap.get(baseTestMethod).size() + "/" 
                           + testSnapshotsPerTestMap.get(baseTestMethod).size() + "] of for test " + baseTestMethod );
                   if( testSnapshotsPerTestMap.remove(baseTestMethod) != null ) { 
                       logger.warn( "Removing data and NOT comparing data for test " + baseTestMethod );
                       untestableTestMethods.add(baseTestMethod);
                   }
               }
              
               testTestMethods.remove(baseTestMethod);
            }
        }
        for( String badTestMethod : untestableTestMethods ) { 
            baseSnapshotsPerTestMap.remove(badTestMethod);
        }
        
        // 3. In this test run, but NOT in the base db!!
        for( String testMethod : testTestMethods ) { 
            String shortTestMethod = testMethod.substring( 
                    testMethod.substring(0, testMethod.lastIndexOf('.')
                    ).lastIndexOf('.')+1 );
            logger.trace( shortTestMethod + " snapshots do not exist in this base db." );
            testSnapshotsPerTestMap.keySet().remove(testMethod);
        }
    }
    
    public static String byteArrayHashCode(byte [] byteArray) { 
        StringBuilder hashCode = new StringBuilder();
        try {
            byte messageDigest[];
            synchronized (algorithm) {
                algorithm.reset();
                algorithm.update(byteArray);
                messageDigest = algorithm.digest();
            }
    
            for (int i=0;i<messageDigest.length;i++) {
                hashCode.append(Integer.toHexString(0xFF & messageDigest[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return hashCode.toString();
    }

    /**
     * Retrieve the name of the actual method running the test, via reflection magic. 
     * @return The method of the (Junit) test running at this moment.
     */
    protected static String getTestMethodName() { 
        String testMethodName = null;
        
        StackTraceElement [] ste = Thread.currentThread().getStackTrace();
        // 0: getStackTrace
        // 1: getTestMethodName (this method)
        // 2: this.persist() or this.merge().. etc.
        FINDTESTMETHOD: for( int i = 3; i < ste.length; ++i ) { 
            Class<?> steClass = getSTEClass(ste[i]);
            if( steClass == null ) { 
                continue;
            }
            
            Method [] classMethods = steClass.getMethods();
            String methodName = ste[i].getMethodName();
            for( int m = 0; m < classMethods.length; ++m ) { 
                if( classMethods[m].getName().equals(methodName) ) { 
                   Annotation [] annos = classMethods[m].getAnnotations(); 
                   for( int a = 0; a < annos.length; ++a ) { 
                       if( annos[a] instanceof Test ) { 
                           testMethodName = steClass.getName() + "." + methodName;
                           break FINDTESTMETHOD;
                       }
                   }
                }
            }
        }
        
        for( int i = 0; testMethodName == null && i < ste.length; ++i ) { 
            Class<?> steClass = getSTEClass(ste[i]);
            if( "runTest".equals(ste[i].getMethodName()) ) { 
                do { 
                    if( TestCase.class.equals(steClass) ) { 
                        StackTraceElement testMethodSTE = ste[i-5];
                        testMethodName = getSTEClass(testMethodSTE).getName() + "." + testMethodSTE.getMethodName();
                    }
                    steClass = steClass.getSuperclass();
                } while( testMethodName == null && steClass != null );
            }
        }
        
        if( testMethodName == null ) { 
            (new Throwable()).printStackTrace();
            if( false ) { 
            for( int i = 0; testMethodName == null && i < ste.length; ++i ) { 
                Class<?> steClass = getSTEClass(ste[i]);
                if( "run".equals(ste[i].getMethodName()) ) { 
                    do { 
                        if( Thread.class.equals(steClass) ) { 
                            StackTraceElement testMethodSTE = ste[i];
                            testMethodName = getSTEClass(testMethodSTE).getName() + "." + testMethodSTE.getMethodName();
                        }
                    } while(testMethodName == null && steClass != null);
                }
            }
            }
            throw new IllegalStateException("BOOM!");
        }
    
        return testMethodName;
    }

    private static Class<?> getSTEClass(StackTraceElement ste) { 
        Class<?> steClass = null;
        try { 
            steClass =  Class.forName(ste.getClassName());
        }
        catch( ClassNotFoundException cnfe ) { 
            // do nothing.. 
        }
          
        return steClass; 
    }


}
