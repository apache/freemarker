/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.debug;

import org.apache.freemarker.core.TemplateException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Represents the debugger-side mirror of a TemplateModel object, a Template
 * object, or a Configuration object. The Environment objects are also represented
 * by instances of this model, although not directly but through a separate
 * subinterface {@link DebuggedEnvironment}. The interface is a union of
 * almost all of FreeMarker template models with identical method signatures.
 * For purposes of optimizing network traffic there are bulk retrieval methods
 * for sequences and hashes, as well as a {@link #getModelTypes()} method that
 * returns a bit mask of various {@code TYPE_xxx} constants flagging which
 * template models are implemented by the mirrored object.
 */
public interface DebugModel extends Remote {
    public static final int TYPE_STRING        =    1;
    public static final int TYPE_NUMBER        =    2;
    public static final int TYPE_DATE          =    4;
    public static final int TYPE_BOOLEAN       =    8;
    public static final int TYPE_SEQUENCE      =   16;
    public static final int TYPE_COLLECTION    =   32;
    public static final int TYPE_HASH          =   64;
    public static final int TYPE_HASH_EX       =  128;
    public static final int TYPE_FUNCTION      =  256;
    public static final int TYPE_DIRECTIVE     = 1024;
    public static final int TYPE_ENVIRONMENT   = 2048;
    public static final int TYPE_TEMPLATE      = 4096;
    public static final int TYPE_CONFIGURATION = 8192;
    
    public String getAsString()
    throws TemplateException,
        RemoteException;
        
    public Number getAsNumber()
    throws TemplateException,
        RemoteException;
    
    public boolean getAsBoolean()
    throws TemplateException,
        RemoteException;
    
    public Date getAsDate()
    throws TemplateException,
        RemoteException;
    
    public int getDateType()
    throws TemplateException,
        RemoteException;
        
    public int size()
    throws TemplateException,
        RemoteException;
        
    public DebugModel get(int index)
    throws TemplateException,
        RemoteException;
    
    public DebugModel[] get(int fromIndex, int toIndex)
    throws TemplateException,
        RemoteException;
        
    public DebugModel get(String key)
    throws TemplateException,
        RemoteException;
        
    public DebugModel[] get(String[] keys)
    throws TemplateException,
        RemoteException;
    
    public DebugModel[] getCollection()
    throws TemplateException,
        RemoteException;

    public String[] keys()
    throws TemplateException,
        RemoteException;
    
    public int getModelTypes()
    throws RemoteException;
}
