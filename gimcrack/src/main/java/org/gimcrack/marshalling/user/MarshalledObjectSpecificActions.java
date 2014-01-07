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
package org.gimcrack.marshalling.user;

import org.gimcrack.marshalling.InternalMarshalledObjectSpecificActions;
import org.gimcrack.marshalling.MarshalledData;

public abstract class MarshalledObjectSpecificActions<MARSHALLED_TYPE, INSTANCE> extends InternalMarshalledObjectSpecificActions<MARSHALLED_TYPE, INSTANCE> {

    public abstract Class<MARSHALLED_TYPE> getMarshalledObjectClass();

    public abstract byte[] getBinaryData(MARSHALLED_TYPE toMarshallClassInstance);

    public abstract Long getMarshalledObjectIndex(MARSHALLED_TYPE toMarshallClassInstance);
    
    public abstract INSTANCE unmarshallObject(MarshalledData marshalledData) throws Exception;
    
}
