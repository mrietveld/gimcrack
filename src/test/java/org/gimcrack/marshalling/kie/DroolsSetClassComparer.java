package org.gimcrack.marshalling.kie;

import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.drools.core.util.AbstractHashTable;
import org.gimcrack.marshalling.ClassComparer;
import org.junit.Assert;

public class DroolsSetClassComparer extends ClassComparer {

    @Override
    public boolean compare(Object objA, Object objB) {
        boolean same = true;

        int length = 0;
        try { 
            Method sizeMethod = AbstractHashTable.class.getDeclaredMethod("size", (Class []) null);
            Integer sizeA = (Integer) sizeMethod.invoke(objA, (Object []) null);
            Integer sizeB = (Integer) sizeMethod.invoke(objB, (Object []) null);
        
            if( ! sizeA.equals(sizeB) ) { 
                return false;
            }
            length = sizeA.intValue();
        }
        catch( Exception e ) { 
            same = false;
            Assert.fail(e.getClass().getSimpleName() + ": " + e.getMessage() );
        }
       
        if( length == 0 ) { 
            return true;
        }
       
        Method toArrayMethod = null;
        try { 
            toArrayMethod = AbstractHashTable.class.getDeclaredMethod("toArray", (Class []) null);
        }
        catch( Exception e ) { 
            same = false;
            Assert.fail(e.getClass().getSimpleName() + ": " + e.getMessage() );
        }
        
        if( toArrayMethod == null ) { 
            fail("Could not retrieve toArray() method for " + objA.getClass().getName());
        }
        
        Object [] arrayA = null;
        Object [] arrayB = null;
        
        try {
            arrayA = (Object []) toArrayMethod.invoke(objA, (Object []) null);
            arrayB = (Object []) toArrayMethod.invoke(objB, (Object []) null);
        } catch (Exception e) {
            same = false;
            fail(e.getClass().getSimpleName() + ": " + e.getMessage() );
        }

        if( arrayA == null && arrayB == null ) { 
            return true;
        }
        else if( arrayA == null || arrayB == null ) { 
            return false; 
        }

        for( int a = 0; a < length; ++a ) { 
            boolean elementIsSame = false;
            for( int b = 0; b < length; ++b ) { 
                Object subObjA = arrayA[a];
                Object subObjB = arrayA[b];
                try { 
                    Method getValueMethod = subObjA.getClass().getMethod("getValue", (Class []) null);
                    subObjA = getValueMethod.invoke(subObjA, (Object [])null);
                    subObjB = getValueMethod.invoke(subObjB, (Object [])null);
                }
                catch( Exception e ) { 
                    e.printStackTrace();
                    fail("Could not retrieve getValue() method for " + subObjA.getClass().getName());
                }
                
                if( compareInstances(subObjA, subObjB, "<entry> ") ) { 
                    logger.trace(context.getName());
                    elementIsSame = true;
                    break;
                }
            }

            if( ! elementIsSame ) { 
                // a matching element was not found in arrayB
                same = false;
                break;
            } 
        }

        return same;
    }
}
