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
package freemarker.template;

import java.time.temporal.Temporal;

/**
 * A simple implementation of the <tt>TemplateDateModel</tt>
 * interface. Note that this class is immutable.
 * <p>This class is thread-safe.
 */
public class SimpleTemporal implements TemplateTemporalModel {
	private final Temporal temporal;

	/**
	 * Creates a new date model wrapping the specified date object and
	 * having the specified type.
	 */
	public SimpleTemporal(Temporal temporal) {
		if (temporal == null) {
			throw new IllegalArgumentException("temporal == null");
		}
		this.temporal = temporal;
	}

	@Override
	public Temporal getAsTemporal() {
		return temporal;
	}

	public String toString() {
		return temporal.toString();
	}
}
