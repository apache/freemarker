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

package freemarker.core;

import java.util.TimeZone;

class XSTemplateDateFormatFactory extends ISOLikeTemplateDateFormatFactory {

    public XSTemplateDateFormatFactory(TimeZone timeZone) {
        super(timeZone);
    }

    public TemplateDateFormat get(int dateType, boolean zonelessInput, String formatDescriptor)
            throws java.text.ParseException, UnknownDateTypeFormattingUnsupportedException {
        // We don't cache these as creating them is cheap (only 10% speedup of ${d?string.xs} with caching)
        return new XSTemplateDateFormat(
                formatDescriptor, 2,
                dateType, zonelessInput,
                getTimeZone(), this);
    }

}
