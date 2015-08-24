package freemarker.manual;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Ignore;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

@Ignore
public abstract class ExamplesTest extends TemplateTest {

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
    
    @Override
    protected final Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.getVersion());
        setupTemplateLoaders(cfg);
        return cfg;
    }

    protected void setupTemplateLoaders(Configuration cfg) {
        cfg.setTemplateLoader(new MultiTemplateLoader(
                new TemplateLoader[] { new StringTemplateLoader(), new ClassTemplateLoader(this.getClass(), "") }));
    }
    
}
