/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
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
 *  
 * @author Attila Szegedi
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
