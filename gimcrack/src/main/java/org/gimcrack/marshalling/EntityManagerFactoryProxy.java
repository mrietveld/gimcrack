/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gimcrack.marshalling;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object serves as a proxy for the EntityManagerFactory and the EntityManager instances.
 * </p>
 * It ensures that when objects that contain marshalled data are persisted, a new MarshalledData
 * object is also persisted with the most recent snapshot of the marshalled data. A new MarshalledData
 * is only made when the marshalled data has changed.
 */
public class EntityManagerFactoryProxy implements InvocationHandler {

    private static Logger logger = LoggerFactory.getLogger(EntityManagerFactoryProxy.class);

    private EntityManagerFactory emf;
    private EntityManager em;

    private ObjectSpecificMarshallingActions objectSpecificMarshallingActions;

    /**
     * This method creates a proxy for either a {@link EntityManagerFactory} or a {@link EntityManager} instance.
     * 
     * @param obj The original instance for which a proxy will be made.
     * @return Object a proxy instance of the given object.
     */
    static Object newInstance(Object obj, ObjectSpecificMarshallingActions marshallingActions) {
        if (obj instanceof EntityManagerFactory || obj instanceof EntityManager) {
            return Proxy.newProxyInstance(obj.getClass().getClassLoader(), getAllInterfaces(obj), new EntityManagerFactoryProxy(
                    obj, marshallingActions));
        } else {
            throw new UnsupportedOperationException("This proxy is only for " + EntityManagerFactory.class.getSimpleName()
                    + " and " + EntityManager.class.getSimpleName() + " instances.");
        }
    }

    /**
     * This method is used in the {@link #newInstance(Object)} method to retrieve all applicable interfaces
     * that the proxy object must conform to.
     * 
     * @param obj The object that will be proxied.
     * @return Class<?> [] an array of all applicable interfaces.
     */
    protected static Class<?>[] getAllInterfaces(Object obj) {
        Class<?>[] interfaces = new Class[0];
        Class<?> superClass = obj.getClass();
        while (superClass != null) {
            Class<?>[] addThese = superClass.getInterfaces();
            if (addThese.length > 0) {
                Class<?>[] moreinterfaces = new Class[interfaces.length + addThese.length];
                System.arraycopy(interfaces, 0, moreinterfaces, 0, interfaces.length);
                System.arraycopy(addThese, 0, moreinterfaces, interfaces.length, addThese.length);
                interfaces = moreinterfaces;
            }
            superClass = superClass.getSuperclass();
        }
        return interfaces;
    }

    /**
     * This is the constructor that follows the InvocationHandler design pattern, so to speak. <br/>
     * It saves the @{link {@link EntityManager} or {@link EntityManagerFactory} for use later.
     * 
     * @param obj The object being proxied.
     */
    private EntityManagerFactoryProxy(Object obj, ObjectSpecificMarshallingActions marshallingActions) {
        if (obj instanceof EntityManagerFactory) {
            this.emf = (EntityManagerFactory) obj;
        } else if (obj instanceof EntityManager) {
            this.em = (EntityManager) obj;
        } else {
            throw new UnsupportedOperationException("This proxy is only for " + EntityManagerFactory.class.getSimpleName()
                    + " and " + EntityManager.class.getSimpleName() + " instances.");
        }
        this.objectSpecificMarshallingActions = marshallingActions;
    }

    private synchronized void lazyInitializeStateMaps(Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        objectSpecificMarshallingActions.lazyInitializeMap(args[0]);
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        String methodName = method.getName();

        logger.trace(methodName);
        lazyInitializeStateMaps(args);
        if ("createEntityManager".equals(methodName)) {
            return createEntityManager(methodName, args);
        } else if ("persist".equals(methodName) && args.length == 1) {
            em.persist(args[0]);
            String testMethodName = MarshallingTestUtil.getTestMethodName();
            if (testMethodName != null) {
                persist(testMethodName, args);
            }
            return result;
        } else if ("merge".equals(methodName) && args.length == 1) {
            result = em.merge(args[0]);
            String testMethodName = MarshallingTestUtil.getTestMethodName();
            if (testMethodName != null) {
                merge(testMethodName, result);
            }
            return result;
        } else if ("find".equals(methodName) && args.length == 2) {
            result = em.find((Class<?>) args[0], args[1]);
            find(result);
        } else {
            Class<?> methodClass = method.getDeclaringClass();
            if (methodClass.equals(EntityManagerFactory.class)) {
                result = invoke(method, emf, args);
            } else if (methodClass.equals(EntityManager.class)) {
                result = invoke(method, em, args);
            } else {
                RuntimeException re = new RuntimeException("Unexpected class " + methodClass + " for method " + methodName);
                re.fillInStackTrace();
                throw re;
            }
        }

        return result;
    }

    private Object invoke(Method method, Object object, Object[] args) throws Throwable {
        Object result = null;
        try {
            result = method.invoke(object, args);
        } catch (InvocationTargetException ite) {
            logger.warn(method.getName() + " threw " + ite.getClass().getSimpleName() + ": " + ite.getMessage());
            throw ite;
        }
        return result;
    }

    /**
     * This method creates a proxy for an EntityManager generated by the real EntityManagerFactory.
     * 
     * @param methodName The name of the test method in which this method is called.
     * @param args The arguments to the EntityManagerFactory.createEntityManager(...) method
     * @return Object a proxy of a EntityManager instance.
     */
    private Object createEntityManager(String methodName, Object[] args) {
        EntityManager realEm = null;
        if (args == null) {
            realEm = (EntityManager) emf.createEntityManager();
        } else if (args[0] instanceof Map<?, ?>) {
            realEm = (EntityManager) emf.createEntityManager((Map<?, ?>) args[0]);
        } else {
            String message = "Method " + methodName + " with args (";
            for (int i = 0; i < args.length; ++i) {
                message += args[i].getClass() + ", ";
            }
            message = message.substring(0, message.lastIndexOf(",")) + ") not supported!";
            throw new UnsupportedOperationException(message);
        }
        return newInstance(realEm, objectSpecificMarshallingActions);
    }

    /**
     * This method stores a MarshalledData object for all objects that contain marshalled data.
     * 
     * @param methodName The name of the test method in which this happens.
     * @param args The arguments to EntityManager.persist(...)
     */
    private void persist(String testMethodName, Object[] args) {
        Object toMarshallClassInstance = args[0];
        byte[] byteArray = objectSpecificMarshallingActions.getAndSaveBinaryData(toMarshallClassInstance);
        if (byteArray != null) {
            MarshalledData marshalledData = new MarshalledData(testMethodName, toMarshallClassInstance);
            objectSpecificMarshallingActions.initializeMarshalledData(marshalledData, toMarshallClassInstance);
            em.persist(marshalledData);
            logger.trace("-.-: " + marshalledData);
        }
    }

    private void merge(String testMethodName, Object updatedObject) {
        if (objectSpecificMarshallingActions.updateMarshalledObjectData(updatedObject, testMethodName)) {
            MarshalledData marshalledData = new MarshalledData(testMethodName, updatedObject);
            objectSpecificMarshallingActions.initializeMarshalledData(marshalledData, updatedObject);
            em.persist(marshalledData);
            logger.trace("-!-: " + marshalledData);
        }
    }

    /**
     * Add the object to the internal map of managed objects.
     * 
     * @param result The objec
     */
    private void find(Object result) {
        if (result == null) {
            return;
        }
        objectSpecificMarshallingActions.addToManagedObjects(result);
    }

    /**
     * This is the method that checks whether or not (managed) objects that (can) contain
     * marshalled data, have marshalled data fields that have been updated. If so, this method
     * (and the methods it calls) create a new MarshalledData object to store a snapshot of
     * the new marshalled data and persist it.
     * 
     * @param testMethodName The name of the test method in which the marshalled data has been created.
     * @param em An EntityManager in order to persist the MarshalledData object.
     */
    protected void updateManagedObjects(String testMethodName, EntityManager em) {
        Set<MarshalledData> newMarshalledData = objectSpecificMarshallingActions.updateManagedObjects(testMethodName);
        
        for( MarshalledData marshalledData : newMarshalledData ) { 
            em.persist(marshalledData);
            logger.trace("-!-: " + marshalledData);
        }
    }

}
