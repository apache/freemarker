package freemarker.ext.beans;

import freemarker.template.TemplateScalarModel;

/**
 * Represents value unwrapped both to {@link Character} and {@link String}. This is needed for unwrapped overloaded
 * method parameters where both {@link Character} and {@link String} occurs on the same parameter position when the
 * {@link TemplateScalarModel} to unwrapp contains a {@link String} of length 1.
 */
final class CharacterOrString {

    private final String stringValue;

    CharacterOrString(String stringValue) {
        this.stringValue = stringValue;
    }
    
    String getAsString() {
        return stringValue;
    }

    char getAsChar() {
        return stringValue.charAt(0);
    }
    
}
