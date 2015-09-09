package freemarker.core;

import java.util.Collection;
import java.util.Collections;

class TemplateElementsToVisit {

  private final Collection<TemplateElement> templateElements;
  private final boolean hideInParent;

  TemplateElementsToVisit(Collection<TemplateElement> templateElements, boolean hideInParent) {
    this.templateElements = null != templateElements ? templateElements : Collections.<TemplateElement>emptyList();
    this.hideInParent = hideInParent;
  }

  public TemplateElementsToVisit(Collection<TemplateElement> templateElements) {
    this(templateElements, false);
  }

  public TemplateElementsToVisit(TemplateElement nestedBlock, boolean hideInParent) {
    this(Collections.singleton(nestedBlock), hideInParent);
  }

  Collection<TemplateElement> getTemplateElements() {
    return templateElements;
  }

  boolean isHideInParent() {
    return hideInParent;
  }
}
