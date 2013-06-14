package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.ClassUtil;

public class Internal_TemplateModelException extends TemplateModelException {

    // Note: On Java 5 we will use `String descPart1, Object... furtherDescParts` instead of `Object[] descriptionParts`
    //       and `String description`. That's why these are at the end of the parameter list.
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_TemplateModelException(String description) {
        super(description);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    public Internal_TemplateModelException(Throwable cause, String description) {
        this(cause, null, description);
    }

    public Internal_TemplateModelException(Environment env, String description) {
        this((Throwable) null, env, description);
    }
    
    public Internal_TemplateModelException(Throwable cause, Environment env) {
        this(cause, env, (String) null);
    }

    public Internal_TemplateModelException(Throwable cause) {
        this(cause, null, (String) null);
    }
    
    public Internal_TemplateModelException(Throwable cause, Environment env, String description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_TemplateModelException(Internal_ErrorDescriptionBuilder description) {
        this(null, description);
    }

    public Internal_TemplateModelException(Environment env, Internal_ErrorDescriptionBuilder description) {
        this(null, env, description);
    }

    public Internal_TemplateModelException(Throwable cause, Environment env, Internal_ErrorDescriptionBuilder description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_TemplateModelException(Object[] descriptionParts) {
        this((Environment) null, descriptionParts);
    }

    public Internal_TemplateModelException(Environment env, Object[] descriptionParts) {
        this((Throwable) null, env, descriptionParts);
    }

    public Internal_TemplateModelException(Throwable cause, Object[] descriptionParts) {
        this(cause, null, descriptionParts);
    }

    public Internal_TemplateModelException(Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(descriptionParts), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_TemplateModelException(Expression blamed, Object[] descriptionParts) {
        this(blamed, null, descriptionParts);
    }

    public Internal_TemplateModelException(Expression blamed, Environment env, Object[] descriptionParts) {
        this(blamed, null, env, descriptionParts);
    }

    public Internal_TemplateModelException(Expression blamed, Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(descriptionParts).blame(blamed), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_TemplateModelException(Expression blamed, String description) {
        this(blamed, null, description);
    }

    public Internal_TemplateModelException(Expression blamed, Environment env, String description) {
        this(blamed, null, env, description);
    }

    public Internal_TemplateModelException(Expression blamed, Throwable cause, Environment env, String description) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(description).blame(blamed), true);
    }

    static Object[] modelHasStoredNullDescription(Class expected, TemplateModel model) {
        return new Object[] {
                "The FreeMarker value exists, but has nothing inside it; the TemplateModel object (class: ",
                model.getClass().getName(), ") has returned a null instead of a ",
                ClassUtil.getShortClassName(expected), ". ",
                "This is possibly a bug in the non-FreeMarker code that builds the data-model." };
    }
    
}
