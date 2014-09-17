package freemarker.template;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class StaticObjectWrappersTest {

    @Test
    public void testNotNull() {
       // There's a such danger because of static initialization order. 
       assertNotNull(ObjectWrapper.BEANS_WRAPPER); 
       assertNotNull(ObjectWrapper.DEFAULT_WRAPPER); 
       assertNotNull(ObjectWrapper.SIMPLE_WRAPPER); 
    }
    
}
