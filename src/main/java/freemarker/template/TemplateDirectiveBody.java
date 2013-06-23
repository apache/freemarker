package freemarker.template;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents the nested content of a directive ({@link TemplateDirectiveModel}) invocation. An implementation of this 
 * class is passed to {@link TemplateDirectiveModel#execute(freemarker.core.Environment, 
 * java.util.Map, TemplateModel[], TemplateDirectiveBody)}. The implementation of the method is 
 * free to invoke it for any number of times, with any writer.
 *
 * @since 2.3.11
 * @author Attila Szegedi
 */
public interface TemplateDirectiveBody
{
    /**
     * Renders the body of the directive body to the specified writer. The 
     * writer is not flushed after the rendering. If you pass the environment's
     * writer, there is no need to flush it. If you supply your own writer, you
     * are responsible to flush/close it when you're done with using it (which
     * might be after multiple renderings).
     * @param out the writer to write the output to.
     */
    public void render(Writer out) throws TemplateException, IOException;
}
