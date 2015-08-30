package freemarker.core;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * @since 2.3.24 
 */
public final class TemplateNumberFormatUtil {
    
    private TemplateNumberFormatUtil() {
        // Not meant to be instantiated
    }

    public static void checkHasNoParameters(String params) throws InvalidFormatParametersException
             {
        if (params.length() != 0) {
            throw new InvalidFormatParametersException(
                    "This number format doesn't support any parameters.");
        }
    }

    /**
     * Utility method to extract the {@link Number} from an {@link TemplateNumberModel}, and throw
     * {@link UnformattableNumberException} with a standard error message if that's {@code null}.
     */
    public static Number getNonNullNumber(TemplateNumberModel numberModel)
            throws TemplateModelException, UnformattableNumberException {
        Number number = numberModel.getAsNumber();
        if (number == null) {
            throw EvalUtil.newModelHasStoredNullException(Number.class, numberModel, null);
        }
        return number;
    }

}
