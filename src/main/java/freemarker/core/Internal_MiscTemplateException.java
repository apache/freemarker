package freemarker.core;

import freemarker.template.TemplateException;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * {@link TemplateException}-s that don't fit into any category that warrant its own class. In fact, this was added
 * because the API of {@link TemplateException} is too simple for the purposes of the core, but it can't be
 * extended without breaking backward compatibility and exposing internals.  
 */
public class Internal_MiscTemplateException extends TemplateException {

    // Note: On Java 5 we will use `String descPart1, Object... furtherDescParts` instead of `Object[] descriptionParts`
    //       and `String description`. That's why these are at the end of the parameter list.
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_MiscTemplateException(String description) {
        super(description, null);
    }

    public Internal_MiscTemplateException(Environment env, String description) {
        super(description, env);
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:

    public Internal_MiscTemplateException(Throwable cause, String description) {
        this(cause, null, description);
    }

    public Internal_MiscTemplateException(Throwable cause, Environment env) {
        this(cause, env, (String) null);
    }

    public Internal_MiscTemplateException(Throwable cause) {
        this(cause, null, (String) null);
    }
    
    public Internal_MiscTemplateException(Throwable cause, Environment env, String description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_MiscTemplateException(Internal_ErrorDescriptionBuilder description) {
        this(null, description);
    }

    public Internal_MiscTemplateException(Environment env, Internal_ErrorDescriptionBuilder description) {
        this(null, env, description);
    }

    public Internal_MiscTemplateException(Throwable cause, Environment env, Internal_ErrorDescriptionBuilder description) {
        super(cause, env, description, true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_MiscTemplateException(Object[] descriptionParts) {
        this((Environment) null, descriptionParts);
    }

    public Internal_MiscTemplateException(Environment env, Object[] descriptionParts) {
        this((Throwable) null, env, descriptionParts);
    }

    public Internal_MiscTemplateException(Throwable cause, Object[] descriptionParts) {
        this(cause, null, descriptionParts);
    }

    public Internal_MiscTemplateException(Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(descriptionParts), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_MiscTemplateException(Expression blamed, Object[] descriptionParts) {
        this(blamed, null, descriptionParts);
    }

    public Internal_MiscTemplateException(Expression blamed, Environment env, Object[] descriptionParts) {
        this(blamed, null, env, descriptionParts);
    }

    public Internal_MiscTemplateException(Expression blamed, Throwable cause, Environment env, Object[] descriptionParts) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(descriptionParts).blame(blamed), true);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Permutation group:
    
    public Internal_MiscTemplateException(Expression blamed, String description) {
        this(blamed, null, description);
    }

    public Internal_MiscTemplateException(Expression blamed, Environment env, String description) {
        this(blamed, null, env, description);
    }

    public Internal_MiscTemplateException(Expression blamed, Throwable cause, Environment env, String description) {
        super(cause, env, new Internal_ErrorDescriptionBuilder(description).blame(blamed), true);
    }
    
}
