package freemarker.ext.beans;

import java.beans.MethodDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import freemarker.template.Version;

/**
 * Used so that the order in which the methods are added to the introspection cache is deterministic. 
 */
public abstract class BeansWrapperWithShortedMethods extends BeansWrapper {
    
    private final boolean desc;

    public BeansWrapperWithShortedMethods(boolean desc) {
        this.desc = desc;
    }

    public BeansWrapperWithShortedMethods(Version incompatibleImprovements, boolean desc) {
        super(incompatibleImprovements);
        this.desc = desc;
    }

    @Override
    MethodDescriptor[] shortMethodDescriptors(MethodDescriptor[] methodDescriptors) {
        ArrayList<MethodDescriptor> ls = new ArrayList<MethodDescriptor>(Arrays.asList(methodDescriptors));
        Collections.sort(ls, new Comparator<MethodDescriptor>() {
            public int compare(MethodDescriptor o1, MethodDescriptor o2) {
                int res = o1.getMethod().toString().compareTo(o2.getMethod().toString());
                return desc ? -res : res;
            }
        });
        return ls.toArray(new MethodDescriptor[ls.size()]);
    }

}
