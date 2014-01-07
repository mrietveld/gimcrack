package org.gimcrack.marshalling.kie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.drools.core.SessionConfiguration;
import org.drools.core.impl.EnvironmentFactory;
import org.drools.core.marshalling.impl.InputMarshaller;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.marshalling.impl.ProcessMarshaller;
import org.drools.core.marshalling.impl.ProtobufMarshaller;
import org.drools.core.util.AbstractHashTable;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.persistence.info.SessionInfo;
import org.drools.persistence.info.WorkItemInfo;
import org.gimcrack.marshalling.Gimcrack;
import org.gimcrack.marshalling.MarshalledData;
import org.gimcrack.marshalling.user.MarshalledObjectSpecificActions;
import org.jbpm.marshalling.impl.ProtobufProcessMarshaller;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.junit.Assert;
import org.kie.api.KieBase;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.KnowledgeBase;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.marshalling.MarshallerFactory;

public class KieGimcrack extends Gimcrack {

    public boolean STORE_KNOWLEDGE_BASE = false;

    public KieGimcrack() {
        registerClassComparer(AbstractHashTable.class, new DroolsSetClassComparer());
        registerMarshalledObjectSpecticAction(new KieSessionSpecific());
        registerMarshalledObjectSpecticAction(new WorkItemSpecific());
        registerMarshalledObjectSpecticAction(new ProcessInstanceSpecific());
    }

    private class KieSessionSpecific extends MarshalledObjectSpecificActions<SessionInfo, KieSession> {

        @Override
        public void initializeMarshalledData(MarshalledData marshalledData, SessionInfo marshalledClassInstance) {
            SessionInfo sessionInfo = (SessionInfo) marshalledClassInstance;
            marshalledData.byteArray = sessionInfo.getData();
            marshalledData.marshalledObjectId = sessionInfo.getId().longValue();
            if (STORE_KNOWLEDGE_BASE) {
                try {
                    storeAssociatedKnowledgeBase(marshalledData, sessionInfo);
                } catch (IOException ioe) {
                    Assert.fail("Unable to retrieve marshalled data or id for " + marshalledClassInstance.getClass().getName()
                            + " object: [" + ioe.getClass().getSimpleName() + ", " + ioe.getMessage());
                }
            }
        }

        private void storeAssociatedKnowledgeBase(MarshalledData marshalledData, SessionInfo sessionInfo) throws IOException {
            KieBase kbase = sessionInfo.getJPASessionMashallingHelper().getKbase();
            marshalledData.serializedKnowledgeBase = DroolsStreamUtils.streamOut(kbase);
        }

        @Override
        public KieSession unmarshallObject(MarshalledData marshalledData) throws Exception {
            // Setup marshaller
            KnowledgeBase kbase;
            if (STORE_KNOWLEDGE_BASE) {
                kbase = (KnowledgeBase) DroolsStreamUtils.streamIn(marshalledData.serializedKnowledgeBase);
            } else {
                kbase = KnowledgeBaseFactory.newKnowledgeBase();
            }
            ObjectMarshallingStrategy[] strategies = new ObjectMarshallingStrategy[] { MarshallerFactory
                    .newSerializeMarshallingStrategy() };
            strategies = addProcessInstanceResolverStrategyIfAvailable(strategies);
            Marshaller marshaller = MarshallerFactory.newMarshaller(kbase, strategies);

            // Prepare input for marshaller
            ByteArrayInputStream bais = new ByteArrayInputStream(marshalledData.byteArray);
            SessionConfiguration conf = SessionConfiguration.getDefaultInstance();
            Environment env = EnvironmentFactory.newEnvironment();

            // Unmarshall
            KieSession ksession = marshaller.unmarshall(bais, conf, env);

            return ksession;
        }

        @Override
        public Class<SessionInfo> getMarshalledObjectClass() {
            return SessionInfo.class;
        }

        private final static String PROCESS_INSTANCE_RESOLVER_STRATEGY = "org.jbpm.marshalling.impl.ProcessInstanceResolverStrategy";

        private ObjectMarshallingStrategy[] addProcessInstanceResolverStrategyIfAvailable(ObjectMarshallingStrategy[] strategies) {

            ObjectMarshallingStrategy processInstanceResolverStrategyObject = null;
            try {
                Class<?> strategyClass = Class.forName(PROCESS_INSTANCE_RESOLVER_STRATEGY);
                Constructor<?> constructor = strategyClass.getConstructors()[0];

                processInstanceResolverStrategyObject = (ObjectMarshallingStrategy) constructor.newInstance(new Object[0]);
            } catch (Throwable t) {
                // do nothing, strategy class could not be
            }

            ObjectMarshallingStrategy[] newStrategies = new ObjectMarshallingStrategy[strategies.length + 1];
            if (processInstanceResolverStrategyObject != null) {
                for (int i = 0; i < strategies.length; ++i) {
                    newStrategies[i] = strategies[i];
                }
                newStrategies[strategies.length] = processInstanceResolverStrategyObject;
                strategies = newStrategies;
            }

            return strategies;
        }

        @Override
        public byte[] getBinaryData(SessionInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getData();
        }

        @Override
        public Long getMarshalledObjectIndex(SessionInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getId().longValue();
        }

    }

    private class WorkItemSpecific extends MarshalledObjectSpecificActions<WorkItemInfo, WorkItem> {

        @Override
        public WorkItem unmarshallObject(MarshalledData marshalledData) throws Exception {
            // Setup env/context/stream
            Environment env = EnvironmentFactory.newEnvironment();
            ByteArrayInputStream bais = new ByteArrayInputStream(marshalledData.byteArray);
            MarshallerReaderContext context = new MarshallerReaderContext(bais, null, null, null, null, env);

            // Unmarshall
            WorkItem unmarshalledWorkItem = InputMarshaller.readWorkItem(context);

            context.close();

            return unmarshalledWorkItem;
        }

        @Override
        public Class<WorkItemInfo> getMarshalledObjectClass() {
            return WorkItemInfo.class;
        }

        @Override
        public byte[] getBinaryData(WorkItemInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getWorkItemByteArray();
        }

        @Override
        public Long getMarshalledObjectIndex(WorkItemInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getId();
        }
    }
    
    private class ProcessInstanceSpecific extends MarshalledObjectSpecificActions<ProcessInstanceInfo, ProcessInstance> {

        @Override
        public ProcessInstance unmarshallObject(MarshalledData marshalledData) throws Exception {
            // Unmarshall
            List <ProcessInstance> processInstances = unmarshallProcessInstances(marshalledData.byteArray);
            
            return processInstances.get(0);
        }

        @Override
        public Class<ProcessInstanceInfo> getMarshalledObjectClass() {
            return ProcessInstanceInfo.class;
        }

        private List<ProcessInstance> unmarshallProcessInstances(byte [] marshalledSessionByteArray) throws Exception { 
            // Setup env/context/stream
            Environment env = EnvironmentFactory.newEnvironment();
            ByteArrayInputStream bais = new ByteArrayInputStream(marshalledSessionByteArray);
            MarshallerReaderContext context = new MarshallerReaderContext(bais, null, null, null, ProtobufMarshaller.TIMER_READERS, env);

            // Unmarshall
            ProcessMarshaller processMarshaller = new ProtobufProcessMarshaller();
            List<ProcessInstance> processInstanceList = null;
            try { 
                processInstanceList = processMarshaller.readProcessInstances(context);
            }
            catch( Exception e ) { 
                e.printStackTrace();
                throw e;
            }
            
            context.close();
            
            return processInstanceList;
        }

        @Override
        public byte[] getBinaryData(ProcessInstanceInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getProcessInstanceByteArray();
        }

        @Override
        public Long getMarshalledObjectIndex(ProcessInstanceInfo toMarshallClassInstance) {
            return toMarshallClassInstance.getId();
        }

    }

}