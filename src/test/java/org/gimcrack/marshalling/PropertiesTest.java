package org.gimcrack.marshalling;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

public class PropertiesTest {

    private final static String DATASOURCE_PROPERTIES = "/gimcrack.compare.properties";

    @Test
    public void og() throws Exception {
        Set<Field> doNotCompareFieldsMap = new HashSet<Field>();
        
        InputStream propsInputStream = PersistenceUtil.class.getResourceAsStream(DATASOURCE_PROPERTIES);
        assertNotNull("BOOM!", propsInputStream);
        Properties props = new Properties();
        try {
            props.load(propsInputStream);
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
        for( Entry<Object, Object> ent : props.entrySet() ) { 
            String[] classField = ((String) ent.getKey()).trim().split("#");
            System.out.println( classField[0] + "[" + classField[1] + "]" );
            Class<?> clazz = Class.forName(classField[0]);
            doNotCompareFieldsMap.add(clazz.getDeclaredField(classField[1]));
        }
    }
}
