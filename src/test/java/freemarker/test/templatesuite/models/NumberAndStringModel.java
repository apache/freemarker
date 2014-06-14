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

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

public class NumberAndStringModel implements TemplateNumberModel,
		TemplateScalarModel {
	
	private final String s;
	
	public NumberAndStringModel(String s) {
		super();
		this.s = s;
	}

	public String getAsString() throws TemplateModelException {
		return s;
	}

	@SuppressWarnings("boxing")
    public Number getAsNumber() throws TemplateModelException {
		return s.length();
	}

}
