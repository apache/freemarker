package freemarker.template;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import junit.framework.TestCase;
import freemarker.core.ParseException;

public class ExceptionTest extends TestCase {
    
    public ExceptionTest(String name) {
        super(name);
    }

    public void testParseExceptionSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        try {
            new Template("<string>", new StringReader("<@>"), cfg);
            fail();
        } catch (ParseException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }

    public void testTemplateErrorSerializable() throws IOException, ClassNotFoundException {
        Configuration cfg = new Configuration();
        Template tmp = new Template("<string>", new StringReader("${noSuchVar}"), cfg);
        try {
            tmp.process(Collections.EMPTY_MAP, new StringWriter());
            fail();
        } catch (TemplateException e) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(e);
            new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
        }
    }
    
}
