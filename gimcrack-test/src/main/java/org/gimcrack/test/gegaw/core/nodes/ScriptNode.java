package org.gimcrack.test.gegaw.core.nodes;

import org.gimcrack.test.gegaw.core.InternalGegawContext;

public abstract class ScriptNode extends AbstractExecuteBranchNode {

    @Override
    public int type() {
        return SCRIPT;
    }

    @Override
    public void execute(InternalGegawContext context) {
        internalExecute();
    }
    
    public abstract void internalExecute();

}
