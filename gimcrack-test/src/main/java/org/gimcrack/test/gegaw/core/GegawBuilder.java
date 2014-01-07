package org.gimcrack.test.gegaw.core;

import javax.persistence.EntityManagerFactory;

import org.gimcrack.test.gegaw.core.context.PersistAction;
import org.gimcrack.test.gegaw.core.nodes.EndNode;
import org.gimcrack.test.gegaw.core.nodes.ScriptNode;
import org.gimcrack.test.gegaw.core.nodes.StartNode;

public class GegawBuilder {

    private GegawBuilder() { 
        
    }
    
    public static GegawBuilder newInstance() {
        return new GegawBuilder();
    }

    public GegawProcessBuilder newProcess() {
        return new GegawProcessBuilder();
    }

    public GegawContextBuilder newContext() {
        return new GegawContextBuilder();
    }

    public class GegawProcessBuilder {
        
        private GegawProcess process;
        
        private GegawProcessBuilder() { 
            process = new GegawProcess();
        }

        public GegawProcessBuilder addStartNode() {
            process.nodes.add(new StartNode());
            return this;
        }

        public GegawProcessBuilder addScriptNode(ScriptNode node) {
            process.nodes.add(node);
            return this;
        }

        public GegawProcessBuilder addEndNode() {
            process.nodes.add(new EndNode());
            return this;
        }
        
        public GegawProcess build() { 
            return process;
        }
    }
    
    public class GegawContextBuilder { 
       
        private InternalGegawContext context;
        
        private GegawContextBuilder() {
           context = new InternalGegawContext(); 
        }
        
        public GegawContextBuilder setEntityManagerFactory(EntityManagerFactory emf) { 
            assert emf != null : EntityManagerFactory.class.getSimpleName() + " is null!";
            context.setEntityManagerFactory(emf);
            context.addAction(new PersistAction(emf));
            return this;
        }
        
        public GegawContext build() { 
            return context;
        }
    }
}
