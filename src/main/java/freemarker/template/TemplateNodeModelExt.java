package freemarker.template;

/**
 * Created by Pmuruge on 10/22/2015.
 */
public interface TemplateNodeModelExt extends TemplateNodeModel {

    /**
     * @return the immediate Previous Sibling of this node
     */
    TemplateNodeModel getPreviousSibling() throws TemplateModelException;

    /**
     * @return the immediate next Sibling of this node
     */
    TemplateNodeModel getNextSibling() throws TemplateModelException;
}
