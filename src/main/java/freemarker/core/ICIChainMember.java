package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * An object that has another older version that's used below a certain
 * {@link Configuration#setIncompatibleImprovements(Version) InCompatible Improvements} version.  
 */
interface ICIChainMember {

    /**
     * The minimum ICE version after which this object can be used.
     */
    int getMinimumICIVersion();

    /**
     * The closest object used for ICE lower than {@link #getMinimumICIVersion()}.
     */
    Object getPreviousICIChainMember();
    
}
