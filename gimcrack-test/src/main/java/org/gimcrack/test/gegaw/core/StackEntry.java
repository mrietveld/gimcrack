package org.gimcrack.test.gegaw.core;

public class StackEntry {

    public int branch;
    public GegawNode node;
    public StackEntry prev;
    
    public StackEntry(GegawNode oldNode, int branchNum, StackEntry prev) {
      this.node = oldNode; 
      this.branch = branchNum; 
      this.prev = prev;
    }
    
    public StackEntry push(GegawNode node, int branch) { 
        return new StackEntry(node, branch, this);
    }
    
    public StackEntry pop() { 
        StackEntry previous = this.prev;
        cleanUp();
        return previous;
    }
    
    public void cleanUp() { 
        this.node = null;
        this.prev = null;
    }
    
}
