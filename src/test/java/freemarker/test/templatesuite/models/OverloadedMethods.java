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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.utility.StringUtil;

/**
 * For testing overloaded method selection.
 */
public class OverloadedMethods {

	public String oneArg(Object a1) {
		return methodCallToStr("oneArg<Object>", a1);
	}

	public String oneArg(String a1) {
		return methodCallToStr("oneArg<String>", a1);
	}

	public String oneArg(Boolean a1) {
		return methodCallToStr("oneArg<Boolean>", a1);
	}

	public String oneArg(boolean a1) {
		return methodCallToStr("oneArg<boolean>", Boolean.valueOf(a1));
	}
	
	public String oneArg(List a1) {
		return methodCallToStr("oneArg<List>", a1);
	}

	public String oneArg(Map a1) {
		return methodCallToStr("oneArg<Map>", a1);
	}

    public String oneArg2(Map a1) {
        return methodCallToStr("oneArg2<Map>", a1);
    }

    public String oneArg2(List a1) {
        return methodCallToStr("oneArg2<List>", a1);
    }
    
    public String oneArg3(List a1, List a2) {
        return methodCallToStr("oneArg3<List, List>", a1, a2);
    }

    public String oneArg3(List a1) {
        return methodCallToStr("oneArg3<List>", a1);
    }
    
    public String oneArg4(Integer a1) {
        return methodCallToStr("oneArg4<Integer>", a1);
    }

    public String oneArg4(int a1) {
        return methodCallToStr("oneArg4<int>", Integer.valueOf(a1));
    }

    public String notOverloaded(List a1) {
        return methodCallToStr("notOverloaded<List>", a1);
    }
    
	public String varargsIssue1(Map a1, List a2) {
	    return methodCallToStr("varargsIssue1<Map, List>", a1, a2);
	}

    public String varargsIssue1(Object... a1) {
        return methodCallToStr("varargsIssue1<Object...>", a1);
    }

    public String varargsIssue2(String a1, List a2) {
        return methodCallToStr("varargsIssue2<String, List>", a1, a2);
    }

    public String varargsIssue2(String a1, Map a2) {
        return methodCallToStr("varargsIssue2<String, Map>", a1, a2);
    }

    public String varargsIssue2(Object... a1) {
        return methodCallToStr("varargsIssue2<Object...>", a1);
    }
    
    public String numberIssue1(int a1) {
        return methodCallToStr("numberIssue1<int>", a1);
    }
    
    public String numberIssue1(float a1) {
        return methodCallToStr("numberIssue1<float>", a1);
    }
    
    public String numberIssue2(int a1) {
        return methodCallToStr("numberIssue2<int>", a1);
    }
    
    public String numberIssue2(BigDecimal a1) {
        return methodCallToStr("numberIssue2<BigDecimal>", a1);
    }

    public String numberIssue3(int a1) {
        return methodCallToStr("numberIssue3<int>", a1);
    }
    
    public String numberIssue3(double a1) {
        return methodCallToStr("numberIssue3<double>", a1);
    }
    
	private String methodCallToStr(String methodName, Object... args) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(methodName);
		sb.append('(');
		boolean hadItems = false;
		for (Object arg : args) {
			if (hadItems) sb.append(", ");
			sb.append(valueToStr(arg));
			hadItems = true;
		}
		sb.append(')');
		
		return sb.toString();
	}
	
	private String valueToStr(Object value) {
		if (value == null) {
		    return "null";
		} else if (value instanceof Character) {
			return "'" + StringUtil.FTLStringLiteralEnc(value.toString()) + "'"; 
		} else if (value instanceof String){
			return "\"" + StringUtil.FTLStringLiteralEnc((String) value) + "\""; 
		} else if (value instanceof Map){
			StringBuilder sb = new StringBuilder(); 
			sb.append("{");
			boolean hadItems = false;
			for (Map.Entry<?, ?> ent : ((Map<?, ?>) value).entrySet()) {
				if (hadItems) sb.append(", ");
				sb.append(valueToStr(ent.getKey()));
				sb.append(": ");
				sb.append(valueToStr(ent.getValue()));
				hadItems = true;
			}
			sb.append("}");
			return sb.toString();
		} else if (value instanceof Collection || value.getClass().isArray()){
			StringBuilder sb = new StringBuilder();
			
	        if (value.getClass().isArray()) {
	            value = Arrays.asList(value);
                sb.append("array");
	        } else if (value instanceof Set) {
				sb.append("set");
			}
			sb.append("[");
			boolean hadItems = false;
			for (Object i : (Collection) value) {
				if (hadItems) sb.append(", ");
				sb.append(i);
				hadItems = true;
			}
			sb.append("]");
			return sb.toString();
		} else {
			return value.toString(); 
		}
	}
	
}
