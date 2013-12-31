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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class InternalMarshalledObjectSpecificActions<MARSHALLED_TYPE, INSTANCE> { 
    
    public abstract Class<MARSHALLED_TYPE> getMarshalledObjectClass();
    
    public abstract INSTANCE unmarshallObject(MarshalledData marshalledData) throws Exception;

    public abstract byte[] getBinaryData(MARSHALLED_TYPE toMarshallClassInstance);

    public abstract Long getMarshalledObjectIndex(MARSHALLED_TYPE toMarshallClassInstance);

    // ensure that Hibernate does not proxy the Map implementation objects
    private ThreadLocal<Map<MARSHALLED_TYPE, byte[]>> managedMarshalledObjectDataMap;
    private ThreadLocal<Map<Long, byte[]>> marshalledObjectInstanceIdDataMap;

    void lazyInitializeMaps() {
        if (managedMarshalledObjectDataMap == null) {
            managedMarshalledObjectDataMap = new ThreadLocal<Map<MARSHALLED_TYPE, byte[]>>();
            marshalledObjectInstanceIdDataMap = new ThreadLocal<Map<Long, byte[]>>();
        }
        if (managedMarshalledObjectDataMap.get() == null) {
            managedMarshalledObjectDataMap.set(new HashMap<MARSHALLED_TYPE, byte[]>());
            marshalledObjectInstanceIdDataMap.set(new HashMap<Long, byte[]>());
        }
    }

    public void initializeMarshalledData(MarshalledData marshalledData, MARSHALLED_TYPE marshalledClassInstance) { 
        marshalledData.byteArray = getBinaryData(marshalledClassInstance);
        marshalledData.marshalledObjectId = getMarshalledObjectIndex(marshalledClassInstance);
    }

    byte[] getAndSaveBinaryData(MARSHALLED_TYPE toMarshallClassInstance) {
        byte[] byteArray = getBinaryData(toMarshallClassInstance);
        managedMarshalledObjectDataMap.get().put(toMarshallClassInstance, byteArray != null ? byteArray.clone() : null);

        Long id = getMarshalledObjectIndex(toMarshallClassInstance);
        marshalledObjectInstanceIdDataMap.get().put(id, byteArray);

        return byteArray;
    }

    boolean updateMarshalledObjectData(MARSHALLED_TYPE updatedObject, String testMethodName) {
        Map<MARSHALLED_TYPE, byte[]> updatedObjectsMap = new HashMap<MARSHALLED_TYPE, byte[]>();
        byte [] origMarshalledBytes = managedMarshalledObjectDataMap.get().get(updatedObject); 

        byte [] updatedBinaryData = getBinaryData(updatedObject);
        if( ! Arrays.equals(origMarshalledBytes, updatedBinaryData) ) { 
            updatedObjectsMap.put(updatedObject, updatedBinaryData );

            // Retrieve the most recent marshalled data for this object that was saved in this test method
            byte [] thisMarshalledData = marshalledObjectInstanceIdDataMap.get().get(getMarshalledObjectIndex(updatedObject));

            // ? If there has been no data persisted for this object for this test method (yet), 
            // ? Or if the most recently persisted data is NOT the same as what's now been persisted, 
            // ->  then it's "new" marshalled data, so save it in a MarshalledData object.
            return (thisMarshalledData == null || ! Arrays.equals(thisMarshalledData, updatedBinaryData));
        } else { 
            return false;
        }
    }

    void internalInitializeMarshalledData(MarshalledData marshalledData, MARSHALLED_TYPE marshalledClassInstance) {
        initializeMarshalledData(marshalledData, marshalledClassInstance);
        managedMarshalledObjectDataMap.get().put(marshalledClassInstance, marshalledData.byteArray);
    }

    void addToManagedObjects(MARSHALLED_TYPE result) {
        byte[] data = managedMarshalledObjectDataMap.get().get(result);
        if (data == null) {
            byte[] byteArray = getBinaryData(result);
            managedMarshalledObjectDataMap.get().put((MARSHALLED_TYPE) result, byteArray);
        }
    }

    Set<MarshalledData> updateManagedObjects(String testMethodName) {
        Set<MarshalledData> marshalledDataToSave = new LinkedHashSet<MarshalledData>();
        Map<MARSHALLED_TYPE, byte []> updatedObjectsMap = new HashMap<MARSHALLED_TYPE, byte[]>();
        for( MARSHALLED_TYPE sessionInfo : managedMarshalledObjectDataMap.get().keySet()) { 
            byte [] origMarshalledBytes = managedMarshalledObjectDataMap.get().get(sessionInfo); 

            byte [] newMarshalledBytes = getBinaryData(sessionInfo);
            if( Arrays.equals(origMarshalledBytes, newMarshalledBytes) ) { 
                // If the marshalled data in this object has NOT been changed, skip this object.
                continue;
            }
            updatedObjectsMap.put(sessionInfo, newMarshalledBytes);

            // Retrieve the most recent marshalled data for this object that was saved in this test method
            Long index = getMarshalledObjectIndex(sessionInfo);
            byte [] thisMarshalledData = marshalledObjectInstanceIdDataMap.get().get(index);
            
            // ? If there has been no data persisted for this object for this test method (yet), 
            // ? Or if the most recently persisted data is NOT the same as what's now been persisted, 
            // ->  then it's "new" marshalled data, so save it in a MarshalledData object.
            if( thisMarshalledData == null || ! Arrays.equals(thisMarshalledData, newMarshalledBytes) ) {
                MarshalledData marshalledData = new MarshalledData(sessionInfo);
                marshalledObjectInstanceIdDataMap.get().put(index, marshalledData.byteArray);
                marshalledDataToSave.add(marshalledData);
            }
        }
        for( MARSHALLED_TYPE sessionInfo : updatedObjectsMap.keySet() ) { 
            managedMarshalledObjectDataMap.get().put(sessionInfo, updatedObjectsMap.get(sessionInfo)); 
        }
        return marshalledDataToSave;
    }
}
