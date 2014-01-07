package org.gimcrack.test.gegaw;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.gimcrack.test.gegaw.core.GegawBuilder;
import org.gimcrack.test.gegaw.core.GegawContext;
import org.gimcrack.test.gegaw.core.GegawProcess;
import org.gimcrack.test.gegaw.core.nodes.ScriptNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BasicTest {

    private static EntityManagerFactory emf;

    @BeforeClass
    public static void beforeClass() {
        emf = Persistence.createEntityManagerFactory("org.gimcrack.test.gegaw");
    }

    @AfterClass
    public static void afterClass() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    public void simpleTest() {
        GegawBuilder builder = GegawBuilder.newInstance();

        GegawProcess process 
            = builder.newProcess()
                .addStartNode()
                .addScriptNode(new ScriptNode() {
                    public void internalExecute() {
                        System.out.println("Script Node node");
                    } 
                })
                .addEndNode()
                .build();

        GegawContext context = builder.newContext()
                .setEntityManagerFactory(emf)
                .build();

        System.out.println( "START" );
        process.start(context);
        System.out.println( "DONE" );
    }
}
