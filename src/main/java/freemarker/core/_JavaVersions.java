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
package freemarker.core;

import freemarker.template.Version;

/**
 * Used internally only, might change without notice!
 */
public final class _JavaVersions {

	private static final int JAVA_8_VERSION = 8;
	private static final int JAVA_17_VERSION = 17;

	public static final boolean IS_AT_LEAST_8 = getJavaVersion() >= JAVA_8_VERSION;
	public static final boolean IS_AT_LEAST_17 = getJavaVersion() >= JAVA_17_VERSION;

	/**
	 * Determines the version of the JVM running the application. This method will
	 * initially rely on the {@literal java.version} system property but, if unsuccessful,
	 * will try to detect JDK classes according to the version they were introduced in.
	 *
	 * @return The Java version of the current JVM.
	 */
	private static final int getJavaVersion() {

		int javaSystemVersion = getJavaVersionFromSystem();
		
		System.out.println("Java version from system = "+ javaSystemVersion);

		if (javaSystemVersion == -1) {
			if (isJava17ClassDetected()) {
				return JAVA_17_VERSION;
			}
			else {
				if (isJava8ClassDetected()) {
					return JAVA_8_VERSION;
				}
			}
		}

		return javaSystemVersion;
	}

	/**
	 * Determines the version of the JVM running the application by relying on the
	 * {@literal java.version} system property.
	 *
	 * @return The Java version of the current JVM, read from the {@literal java.version}
	 *         system property.
	 */
	private static final int getJavaVersionFromSystem() {

		String javaVersion = System.getProperty("java.version");

		if (javaVersion != null) {
			try {
				return Integer.valueOf(javaVersion);
			}
			catch (NumberFormatException e) {
				Version v = new Version(javaVersion);

				if (v.getMajor() == 1) {
					return v.getMinor();
				}
				else {
					return v.getMajor();
				}
			}
		}

		return -1;
	}

	/**
	 * Detects whether a Java 8 class is present on the classpath or not.
	 *
	 * @return {@code true} if a Java 8 class was detected, {@code false otherwise}.
	 */
	private static boolean isJava8ClassDetected() {

		try {
            Class.forName("java.time.Instant");
            return true;
        } catch (Exception e) {
        	return false;
        }
	}

	/**
	 * Detects whether a Java 17 class is present on the classpath or not.
	 *
	 * @return {@code true} if a Java 17 class was detected, {@code false otherwise}.
	 */
	private static boolean isJava17ClassDetected() {

		try {
            Class.forName("java.util.random.RandomGenerator");
            return true;
        } catch (Exception e) {
        	return false;
        }
	}

	/**
	 * Default private constructor to avoid instantiating this class.
	 */
	private _JavaVersions() {
		super();
	}

}
