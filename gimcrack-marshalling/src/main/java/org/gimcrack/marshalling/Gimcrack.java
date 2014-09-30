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

import org.gimcrack.compare.ClassComparer;
import org.gimcrack.compare.CompareViaReflectionUtil;
import org.gimcrack.marshalling.user.MarshalledObjectSpecificActions;


public abstract class Gimcrack {

    private CompareViaReflectionUtil compareUtil = new CompareViaReflectionUtil();
    private ObjectSpecificMarshallingActions marshallingActions = new ObjectSpecificMarshallingActions();
    
    public final void registerClassComparer(Class<?> comparedClass, ClassComparer classComparer) { 
        this.compareUtil.registerClassComparer(comparedClass, classComparer);
    }
    
    public void registerMarshalledObjectSpecticAction(MarshalledObjectSpecificActions marshalledObjectSpecificActions) {
        marshallingActions.registerAction(marshalledObjectSpecificActions);
    }
    
    public boolean compareInstances(Object objA, Object objB) { 
        return compareUtil.compareInstances(objA, objB);
    }
    
    public boolean compareAtomicPrimitives(Object objA, Object objB) {
        return compareUtil.compareAtomicPrimitives(objA, objB);
    }
    

    public Object unmarshallObject(MarshalledData marshalledData) throws Exception {
        return marshallingActions.unmarshallObject(marshalledData);
    }
}
