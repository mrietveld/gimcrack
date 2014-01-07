package org.gimcrack.test.gegaw.core.nodes;

import org.gimcrack.test.gegaw.core.GegawContext;
import org.gimcrack.test.gegaw.core.GegawNode;

public abstract class AbstractExecuteBranchNode implements GegawNode {

    public GegawNode getBranch(GegawNode[] branches, GegawContext context, int branch) {
        if( branches.length > 1 ) { 
            throw new IllegalStateException(this.getClass().getSimpleName() + " may not have more than one branch.");
        }
        return branches[0];
    }

    @Override
    public int getNumBranches(GegawNode[] branches, GegawContext context) {
        if( branches.length > 1 ) { 
            throw new IllegalStateException(this.getClass().getSimpleName() + " may not have more than one branch.");
        }
        return branches.length;
    }
}
