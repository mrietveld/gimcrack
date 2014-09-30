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
package org.gimcrack.compare;

import org.gimcrack.compare.CompareViaReflectionUtil.DebugContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClassComparer {

    protected static Logger logger = LoggerFactory.getLogger(ClassComparer.class);
    
    private CompareViaReflectionUtil compareViaReflectionUtil;
    protected DebugContext context;
    
    public void register(Class<?> comparedClass, CompareViaReflectionUtil compareUtil) { 
       this.compareViaReflectionUtil = compareUtil;
       this.compareViaReflectionUtil.internalRegisterClassComparer(comparedClass, this);
    }
    
    public abstract boolean compare(Object objA, Object objB);
    
    public boolean executeCompare(DebugContext debugContext, Object objA, Object objB) { 
       this.context = debugContext;
       return compare(objA, objB);
    }
    
    public final boolean compareInstances(Object objA, Object objB, String operationIndicator) { 
        return compareViaReflectionUtil.compareInstances(context.nestedClone(": " + operationIndicator), objA, objB);
    }
}
