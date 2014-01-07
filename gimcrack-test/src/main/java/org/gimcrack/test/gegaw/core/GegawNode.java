package org.gimcrack.test.gegaw.core;


public interface GegawNode {

    public int type();
    public void execute(InternalGegawContext context);
    public int getNumBranches(GegawNode[] branches, GegawContext context);
    public GegawNode getBranch(GegawNode[] branches, GegawContext context, int branch);
    
    final static int INTERNAL_START = 1;
    final static int START = 2;
    final static int END = 3;
    final static int SCRIPT = 4;
    
}
