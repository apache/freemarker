package freemarker.manual;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Ignore;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;

@Ignore
public abstract class ExamplesTest {

    protected Properties loadPropertiesFile(String name) throws IOException {
        Properties props = new Properties();
        InputStream in = this.getClass().getResourceAsStream(name);
        try {
            props.load(in);
        } finally {
            in.close();
        }
        return props;
    }
    
    protected Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.getVersion());
        setupConfiguration(cfg);
        return cfg;
    }

    protected void setupConfiguration(Configuration cfg) {
        StringTemplateLoader tl = new StringTemplateLoader();
        cfg.setTemplateLoader(tl);
        tl.putTemplate("mail/t.ftl", "");
        tl.putTemplate("t.html", "");
        tl.putTemplate("t.htm", "");
        tl.putTemplate("t.xml", "");
        tl.putTemplate("t.rtf", "");
    }
    
}
