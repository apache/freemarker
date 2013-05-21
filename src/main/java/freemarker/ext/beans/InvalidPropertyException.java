package freemarker.ext.beans;

import freemarker.template.TemplateModelException;

/**
 * An exception thrown when there is an attempt to access
 * an invalid bean property when we are in a "strict bean" mode
 * @author Jonathan Revusky
 */

public class InvalidPropertyException extends TemplateModelException {
	
    public InvalidPropertyException(String description) {
        super(description);
    }
}
