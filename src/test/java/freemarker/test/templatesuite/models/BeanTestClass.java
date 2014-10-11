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

package freemarker.test.templatesuite.models;

/**
 */
public class BeanTestClass extends BeanTestSuperclass implements BeanTestInterface<Integer>
{
    public static final String STATIC_FINAL_FIELD = "static-final-field";
    public static String STATIC_FIELD = "static-field";
    
	public String getFoo()
	{
	    return "foo-value";
	}
	
	public String getBar(int index)
	{
	    return "bar-value-" + index;
	}
	
	public String overloaded(int i)
	{
	    return "overloaded-int-" + i;
	}
	
	public String overloaded(String s)
	{
	    return "overloaded-String-" + s;
	}
	
	public static String staticMethod()
	{
	    return "static-method";
	}
	
	public static String staticOverloaded(int i)
	{
	    return "static-overloaded-int-" + i;
	}

	public static String staticOverloaded(String s)
	{
	    return "static-overloaded-String-" + s;
	}
	
	public PrivateInner getPrivateInner() {
	    return new PrivateInner();
	}

        public PublicInner getPublicInner() {
            return new PublicInner();
        }
	
        public class PublicInner {
            
            public int getX() {
                return 1;
            }
            
            public String m() {
                return "m";
            }
            
        }
        
        @SuppressWarnings("unused")
	private class PrivateInner {
	    
            public int getX() {
	        return 2;
	    }
	    
	    public String m() {
	        return "M";
	    }
	    
	}

}
