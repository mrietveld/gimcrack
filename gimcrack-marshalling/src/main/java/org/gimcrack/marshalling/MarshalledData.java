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

import static org.gimcrack.marshalling.MarshallingTestUtil.*;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

@Entity
@SequenceGenerator(name="marshalledDataIdSeq", sequenceName="MARSHALLEDDATA_ID_SEQ")
public class MarshalledData {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO, generator="marshalledDataIdSeq")
    public Integer id;
    
    @Lob
    public byte[] byteArray;
    
    @Lob
    public byte[] serializedKnowledgeBase;
    
    public String testMethodName;
    public Integer snapshotNumber;
  
    public String marshalledObjectClassName;
    public Long marshalledObjectId;
    
    @Transient
    private static HashMap<String, AtomicInteger> testMethodSnapshotNumMap = new HashMap<String, AtomicInteger>();
    
    public MarshalledData() { 
        // for the ORM/persistence which requires a default constructor to initialize entity classes
    }
    
    public MarshalledData(Object marshalledClassInstance) { 
        this(getTestMethodName(), marshalledClassInstance);
    }
    
    public MarshalledData(String testMethodName, Object marshalledClassInstance) { 
        if( testMethodName != null ) { 
            this.testMethodName = testMethodName;
        }
        else { 
            this.testMethodName = getTestMethodName();
        }
        
        // snapshot number
        if( testMethodSnapshotNumMap.get(this.testMethodName) == null ) { 
            testMethodSnapshotNumMap.put(this.testMethodName, new AtomicInteger(-1));
        }
        this.snapshotNumber = testMethodSnapshotNumMap.get(this.testMethodName).incrementAndGet();

        // marshalled object class name
        this.marshalledObjectClassName =  marshalledClassInstance.getClass().getName();
    }
    
    public static Integer getCurrentTestMethodSnapshotNumber() { 
       String testMethodName = getTestMethodName();
       if( testMethodSnapshotNumMap.get(testMethodName) != null ) { 
          return testMethodSnapshotNumMap.get(testMethodName).intValue(); 
       }
       return null;
    }
    
    public String getTestMethodAndSnapshotNum() { 
       return this.testMethodName + ":" + this.snapshotNumber;
    }
    
    public String toString() { 
        StringBuilder string = new StringBuilder();
        string.append( (id != null ? id : "") + ":");
        if( byteArray != null ) { 
            string.append(byteArrayHashCode(byteArray));
        }
        string.append( ":" );
        string.append( (testMethodName != null ? testMethodName : "") + ":" );
        string.append( (snapshotNumber != null ? snapshotNumber : "") + ":" );
        string.append( (marshalledObjectClassName != null ? marshalledObjectClassName : "") + ":" );
        string.append( (marshalledObjectId != null ? marshalledObjectId : "") );
       
        return string.toString();
    }
    

}
