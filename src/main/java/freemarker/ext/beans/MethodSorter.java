package freemarker.ext.beans;

import java.beans.MethodDescriptor;

/**
 * Used for JUnit testing method-order dependence bugs via
 * {@link BeansWrapper.PropertyAssignments#setMethodSorter(MethodSorter)}.
 */
interface MethodSorter {

    MethodDescriptor[] sortMethodDescriptors(MethodDescriptor[] methodDescriptors);
    
}
