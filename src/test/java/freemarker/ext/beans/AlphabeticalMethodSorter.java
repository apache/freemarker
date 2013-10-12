package freemarker.ext.beans;

import java.beans.MethodDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

class AlphabeticalMethodSorter implements MethodSorter {

    private final boolean desc;
    
    public AlphabeticalMethodSorter(boolean desc) {
        this.desc = desc;
    }

    public MethodDescriptor[] sortMethodDescriptors(MethodDescriptor[] methodDescriptors) {
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