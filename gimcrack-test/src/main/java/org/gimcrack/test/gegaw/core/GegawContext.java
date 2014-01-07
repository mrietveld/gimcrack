package org.gimcrack.test.gegaw.core;

public interface GegawContext {

    public void initialize();
    
    public boolean isVisited(GegawNode node);

    public void setVisited(GegawNode node);

    public void execute(GegawNode v);

    public void beforeBranch(GegawNode node, int i);

    public void skipBranch(GegawNode node, int i);

    public void afterBranch(GegawNode node, int i);

    public void postorder(GegawNode node);

    public StackEntry push(StackEntry stack, GegawNode node, int branchNum, GegawNode branchNode, int numBranches);

}
