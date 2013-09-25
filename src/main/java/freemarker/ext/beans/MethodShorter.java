package freemarker.ext.beans;

import java.beans.MethodDescriptor;

/**
 * Used for JUnit testing method-order dependence bugs via
 * {@link BeansWrapper.SettingAssignments#setMethodShorter(MethodShorter)}.
 */
interface MethodShorter {

    MethodDescriptor[] shortMethodDescriptors(MethodDescriptor[] methodDescriptors);
    
}
