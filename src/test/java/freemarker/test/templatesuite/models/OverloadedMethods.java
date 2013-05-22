package freemarker.test.templatesuite.models;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.utility.StringUtil;

public class OverloadedMethods {

	public String oneArg(Object a1) {
		return methodCallToStr("oneArg", a1);
	}

	public String oneArg(String a1) {
		return methodCallToStr("oneArg", a1);
	}

	public String oneArg(Boolean a1) {
		return methodCallToStr("oneArg", a1);
	}

	public String oneArg(boolean a1) {
		return methodCallToStr("oneArg#p", Boolean.valueOf(a1));
	}
	
	public String oneArg(List a1) {
		return methodCallToStr("oneArg", a1);
	}

	public String oneArg(Map a1) {
		return methodCallToStr("oneArg", a1);
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
		} else if (value instanceof Character){
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
		} else if (value instanceof Collection){
			StringBuilder sb = new StringBuilder();
			if (value instanceof Set) {
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
