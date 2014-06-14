/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

import freemarker.template.TemplateModelException;

/**
 * Represents the debugger-side mirror of a TemplateModel object, a Template
 * object, or a Configuration object. The Environment objects are also represented
 * by instances of this model, although not directly but through a separate
 * subinterface {@link DebuggedEnvironment}. The interface is a union of
 * almost all of FreeMarker template models with identical method signatures.
 * For purposes of optimizing network traffic there are bulk retrieval methods
 * for sequences and hashes, as well as a {@link #getModelTypes()} method that
 * returns a bit mask of various <tt>TYPE_xxx</tt> constants flagging which
 * template models are implemented by the mirrored object.
 */
public interface DebugModel extends Remote
{
    public static final int TYPE_SCALAR        =    1;
    public static final int TYPE_NUMBER        =    2;
    public static final int TYPE_DATE          =    4;
    public static final int TYPE_BOOLEAN       =    8;
    public static final int TYPE_SEQUENCE      =   16;
    public static final int TYPE_COLLECTION    =   32;
    public static final int TYPE_HASH          =   64;
    public static final int TYPE_HASH_EX       =  128;
    public static final int TYPE_METHOD        =  256;
    public static final int TYPE_METHOD_EX     =  512;
    public static final int TYPE_TRANSFORM     = 1024;
    public static final int TYPE_ENVIRONMENT   = 2048;
    public static final int TYPE_TEMPLATE      = 4096;
    public static final int TYPE_CONFIGURATION = 8192;
    
    public String getAsString()
    throws
        TemplateModelException,
        RemoteException;
        
    public Number getAsNumber()
    throws
        TemplateModelException,
        RemoteException;
    
    public boolean getAsBoolean()
    throws
        TemplateModelException,
        RemoteException;
    
    public Date getAsDate()
    throws
        TemplateModelException,
        RemoteException;
    
    public int getDateType()
    throws
        TemplateModelException,
        RemoteException;
        
    public int size()
    throws
        TemplateModelException,
        RemoteException;
        
    public DebugModel get(int index)
    throws
        TemplateModelException,
        RemoteException;
    
    public DebugModel[] get(int fromIndex, int toIndex)
    throws
        TemplateModelException,
        RemoteException;
        
    public DebugModel get(String key)
    throws
        TemplateModelException,
        RemoteException;
        
    public DebugModel[] get(String[] keys)
    throws
        TemplateModelException,
        RemoteException;
    
    public DebugModel[] getCollection()
    throws
        TemplateModelException,
        RemoteException;

    public String[] keys()
    throws
        TemplateModelException,
        RemoteException;
    
    public int getModelTypes()
    throws
        RemoteException;
}
