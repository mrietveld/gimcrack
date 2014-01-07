package org.gimcrack.test.gegaw.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.gimcrack.test.gegaw.core.context.ContextAction;

public class InternalGegawContext implements GegawContext {

    private volatile boolean multiThreaded = false;
    private volatile boolean useJTA = false;
    
    private volatile EntityManagerFactory emf;
    private Set<ContextAction> referenceContextActions
        = new LinkedHashSet<ContextAction>();
    private ThreadLocal<Set<ContextAction>> localContextActions 
        = new ThreadLocal<Set<ContextAction>>();
    
    private Map<GegawNode, NodeStatus> history = new LinkedHashMap<GegawNode, NodeStatus>();
    
    private GegawNode currentNode = null;
    
    InternalGegawContext() { 
        
    }
    
    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public boolean isMultiThreaded() {
        return multiThreaded;
    }

    public boolean useJTA() {
        return useJTA;
    }

    public GegawNode getCurrentNode() {
        return currentNode;
    }

    public void initialize() { 
        localInitialize();
    }
    
    void localInitialize() { 
        Set<ContextAction> threadContextActions
             = new LinkedHashSet<ContextAction>();
        threadContextActions.addAll(referenceContextActions);
        localContextActions.set(threadContextActions);
    }
    
    @Override
    public void execute(GegawNode node) {
        pre();
        System.out.println( "EXEC: " + node.getClass().getSimpleName() );
        node.execute(this);
        post();
    }

    void pre() {
       for( ContextAction action : localContextActions.get() ) { 
           action.pre(this);
       }
    }
    
    void post() { 
        for( ContextAction action : localContextActions.get() ) { 
            action.post(this);
        }
    }

    public void merge(InternalGegawContext startContext) {
       throw new UnsupportedOperationException(Gegaw.unsupported(startContext));
    }

    public GegawNode[] getNextNodes(GegawNode executingNode) {
        throw new UnsupportedOperationException(Gegaw.unsupported(executingNode));
    }

    public void addAction(ContextAction action) {
        this.referenceContextActions.add(action);
    }

    @Override
    public void beforeBranch(GegawNode branch, int i) {
        System.out.println( "BEFO: [" + i + "] " + branch.getClass().getSimpleName() );
        
    }

    @Override
    public void skipBranch(GegawNode branch, int i) {
        System.out.println( "SKIP: [" + i + "] " + branch.getClass().getSimpleName() );
    }

    @Override
    public void afterBranch(GegawNode node, int i) {
        System.out.println( "AFTR: [" + i + "] " + node.getClass().getSimpleName() );
    }

    @Override
    public void postorder(GegawNode node) {
        System.out.println( "POST: " + node.getClass().getSimpleName() );
    }

    @Override
    public boolean isVisited(GegawNode node) {
        NodeStatus status = history.get(node);
        return status == null ? false : status.visited;
    }

    @Override
    public void setVisited(GegawNode node) {
        history.put(node, new NodeStatus());
    }
    
    private class NodeStatus { 
        boolean visited = true;
        boolean exception = false;
        int compensated = -1;
    }

    @Override
    public StackEntry push(StackEntry stack, GegawNode oldNode, int branchNum, GegawNode branchNode, int numBranches) {
        StackEntry newStack = stack.push(oldNode, branchNum);
        if( branchNum == numBranches ) { 
            newStack.prev.cleanUp();
            newStack.prev = null;
        } 
        return newStack;
    }

}
