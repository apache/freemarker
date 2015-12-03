package freemarker.core;

import java.util.Collection;
import java.util.Collections;

/**
 * Used as the return value of {@link TemplateElement#accept(Environment)} when the invoked element has nested elements
 * to invoke. It would be more natural to invoke child elements before returning from
 * {@link TemplateElement#accept(Environment)}, however, if there's nothing to do after the child elements were invoked,
 * that would mean wasting stack space.
 * 
 * @since 2.3.24
 */
class TemplateElementsToVisit {

    private final Collection<TemplateElement> templateElements;

    TemplateElementsToVisit(Collection<TemplateElement> templateElements) {
        this.templateElements = null != templateElements ? templateElements : Collections.<TemplateElement> emptyList();
    }

    TemplateElementsToVisit(TemplateElement nestedBlock) {
        this(Collections.singleton(nestedBlock));
    }

    Collection<TemplateElement> getTemplateElements() {
        return templateElements;
    }
    
}
