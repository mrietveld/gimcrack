package org.gimcrack.test.gegaw.core.context;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.gimcrack.test.gegaw.core.Gegaw;
import org.gimcrack.test.gegaw.core.InternalGegawContext;
import org.gimcrack.test.gegaw.core.nodes.EndNode;

public class PersistAction implements ContextAction {

    private EntityManagerFactory emf;
    private ThreadLocal<EntityManager> localEm = new ThreadLocal<EntityManager>();

    public PersistAction(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void pre(InternalGegawContext context) {
        getEntityManager(true);
        startTransaction(context);
    }

    private void startTransaction(InternalGegawContext context) {
        if (context.useJTA()) {
            throw new UnsupportedOperationException(Gegaw.unsupported(context) + "(JTA)");
        } else {
            getEntityManager(true).getTransaction().begin();
        }
    }

    @Override
    public void post(InternalGegawContext context) {
        closeTransaction(context);
        if( context.getCurrentNode() instanceof EndNode ) { 
            EntityManager em = getEntityManager(false);
            if( em != null ) { 
                em.close();
            }
        }
    }

    private void closeTransaction(InternalGegawContext context) {
        if (context.useJTA()) {
            throw new UnsupportedOperationException(Gegaw.unsupported(context) + "(JTA)");
        } else {
            getEntityManager(true).getTransaction().commit();
        }
    }

    private EntityManager getEntityManager(boolean initialize) {
        EntityManager em = localEm.get();
        if (em == null && initialize) {
            em = emf.createEntityManager();
            localEm.set(em);
        }
        return em;
    }
}
