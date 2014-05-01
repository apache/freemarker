package freemarker.test.templatesuite.models;

import java.util.Date;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Implements all template models that are interesting when calling overloaded Java methods.
 */
public class AllTemplateModels implements
        TemplateScalarModel, TemplateNumberModel, TemplateDateModel, TemplateBooleanModel,
        TemplateHashModelEx, TemplateSequenceModel, TemplateCollectionModel {

    public static final AllTemplateModels INSTANCE = new AllTemplateModels();
    
    private final TemplateModelIterator EMPTY_ITERATOR = new TemplateModelIterator() {

        public TemplateModel next() throws TemplateModelException {
            return null;
        }

        public boolean hasNext() throws TemplateModelException {
            return false;
        }
        
    };
    
    private final TemplateCollectionModel EMPTY_COLLECTION = new TemplateCollectionModel() {

        public TemplateModelIterator iterator() throws TemplateModelException {
            return EMPTY_ITERATOR;
        }
    };
    
    public TemplateModel get(String key) throws TemplateModelException {
        return new SimpleScalar("value for key " + key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return true;
    }

    public TemplateModelIterator iterator() throws TemplateModelException {
        return EMPTY_ITERATOR;
    }

    public TemplateModel get(int index) throws TemplateModelException {
        return null;
    }

    public int size() throws TemplateModelException {
        return 0;
    }

    public TemplateCollectionModel keys() throws TemplateModelException {
        return EMPTY_COLLECTION;
    }

    public TemplateCollectionModel values() throws TemplateModelException {
        return EMPTY_COLLECTION;
    }

    public boolean getAsBoolean() throws TemplateModelException {
        return true;
    }

    public Date getAsDate() throws TemplateModelException {
        return new Date(0);
    }

    public int getDateType() {
        return TemplateDateModel.DATETIME;
    }

    @SuppressWarnings("boxing")
    public Number getAsNumber() throws TemplateModelException {
        return 1;
    }

    public String getAsString() throws TemplateModelException {
        return "s";
    }

}
