package org.gimcrack.test.gegaw.core.context;

import org.gimcrack.test.gegaw.core.InternalGegawContext;

public interface ContextAction {

    public void pre(InternalGegawContext context);
    
    public void post(InternalGegawContext context);
}
