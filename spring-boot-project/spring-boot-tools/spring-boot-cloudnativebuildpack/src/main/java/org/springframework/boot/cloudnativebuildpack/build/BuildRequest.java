/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cloudnativebuildpack.build;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.boot.cloudnativebuildpack.docker.type.ImageReference;
import org.springframework.boot.cloudnativebuildpack.io.Owner;
import org.springframework.boot.cloudnativebuildpack.io.TarArchive;
import org.springframework.util.Assert;

/**
 * A build request to be handled by the {@link Builder}.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class BuildRequest {

	private static final ImageReference DEFAULT_BUILDER = ImageReference.of("cloudfoundry/cnb:0.0.43-bionic");

	private final ImageReference name;

	private final Function<Owner, TarArchive> applicationContent;

	private final ImageReference builder;

	private final Map<String, String> env;

	private final boolean cleanCache;

	private final boolean versboseLogging;

	BuildRequest(ImageReference name, Function<Owner, TarArchive> applicationContent) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(applicationContent, "ApplicationContent must not be null");
		this.name = name.inTaggedForm();
		this.applicationContent = applicationContent;
		this.builder = DEFAULT_BUILDER;
		this.env = Collections.emptyMap();
		this.cleanCache = false;
		this.versboseLogging = false;
	}

	BuildRequest(ImageReference name, Function<Owner, TarArchive> applicationContent, ImageReference builder,
			Map<String, String> env, boolean cleanCache, boolean versboseLogging) {
		this.name = name;
		this.applicationContent = applicationContent;
		this.builder = builder;
		this.env = env;
		this.cleanCache = cleanCache;
		this.versboseLogging = versboseLogging;
	}

	/**
	 * Return a new {@link BuildRequest} with an updated builder.
	 * @param builder the new builder to use
	 * @return an updated build request
	 */
	public BuildRequest withBuilder(ImageReference builder) {
		Assert.notNull(builder, "Builder must not be null");
		return new BuildRequest(this.name, this.applicationContent, builder.inTaggedForm(), this.env, this.cleanCache,
				this.versboseLogging);
	}

	/**
	 * Return a new {@link BuildRequest} with an additional env variable.
	 * @param name the variable name
	 * @param value the variable value
	 * @return an updated build request
	 */
	public BuildRequest withEnv(String name, String value) {
		Assert.hasText(name, "Name must not be empty");
		Assert.hasText(value, "Value must not be empty");
		Map<String, String> env = new LinkedHashMap<String, String>(this.env);
		env.put(name, value);
		return new BuildRequest(this.name, this.applicationContent, this.builder, Collections.unmodifiableMap(env),
				this.cleanCache, this.versboseLogging);
	}

	/**
	 * Return a new {@link BuildRequest} with an additional env variables.
	 * @param env the additional variables
	 * @return an updated build request
	 */
	public BuildRequest withEnv(Map<String, String> env) {
		Assert.notNull(env, "Env must not be null");
		Map<String, String> updatedEnv = new LinkedHashMap<String, String>(this.env);
		updatedEnv.putAll(env);
		return new BuildRequest(this.name, this.applicationContent, this.builder,
				Collections.unmodifiableMap(updatedEnv), this.cleanCache, this.versboseLogging);
	}

	/**
	 * Return a new {@link BuildRequest} with an specific clean cache settings.
	 * @param cleanCache if the cache should be cleaned
	 * @return an updated build request
	 */
	public BuildRequest withCleanCache(boolean cleanCache) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.env, cleanCache,
				this.versboseLogging);
	}

	/**
	 * Return a new {@link BuildRequest} with an specific verbose logging settings.
	 * @param verboseLogging if verbose logging should be used
	 * @return an updated build request
	 */
	public BuildRequest withVerboseLogging(boolean verboseLogging) {
		return new BuildRequest(this.name, this.applicationContent, this.builder, this.env, this.cleanCache,
				verboseLogging);
	}

	/**
	 * Return the name of the image that should be created.
	 * @return the name of the image
	 */
	public ImageReference getName() {
		return this.name;
	}

	/**
	 * Return a {@link TarArchive} containing the application content that the buildpack
	 * should package. This is typically the contents of the Jar.
	 * @param owner the owner of the tar entries
	 * @return the application content
	 * @see TarArchive#fromZip(File, Owner)
	 */
	public TarArchive getApplicationContent(Owner owner) {
		return this.applicationContent.apply(owner);
	}

	/**
	 * Return the builder that should be used.
	 * @return the builder to use
	 */
	public ImageReference getBuilder() {
		return this.builder;
	}

	/**
	 * Return any env variable that should be passed to the builder.
	 * @return the builder env
	 */
	public Map<String, String> getEnv() {
		return this.env;
	}

	/**
	 * Return if caches should be cleaned before packaging.
	 * @return if caches should be cleaned
	 */
	public boolean isCleanCache() {
		return this.cleanCache;
	}

	/**
	 * Return if verbose logging output should be used.
	 * @return if verbose logging should be used
	 */
	public boolean isVerboseLogging() {
		return this.versboseLogging;
	}

	/**
	 * Factory method to create a new {@link BuildRequest} from a JAR file.
	 * @param jarFile the source jar file
	 * @return a new build request instance
	 */
	public static BuildRequest forJarFile(File jarFile) {
		assertJarFile(jarFile);
		return forJarFile(ImageReference.forJarFile(jarFile).inTaggedForm(), jarFile);
	}

	/**
	 * Factory method to create a new {@link BuildRequest} from a JAR file.
	 * @param name the name of the image that should be created
	 * @param jarFile the source jar file
	 * @return a new build request instance
	 */
	public static BuildRequest forJarFile(ImageReference name, File jarFile) {
		assertJarFile(jarFile);
		return new BuildRequest(name, (owner) -> TarArchive.fromZip(jarFile, owner));
	}

	/**
	 * Factory method to create a new {@link BuildRequest} with specific content.
	 * @param name the name of the image that should be created
	 * @param applicationContent function to provide the application content
	 * @return a new build request instance
	 */
	public static BuildRequest of(ImageReference name, Function<Owner, TarArchive> applicationContent) {
		return new BuildRequest(name, applicationContent);
	}

	private static void assertJarFile(File jarFile) {
		Assert.notNull(jarFile, "JarFile must not be null");
		Assert.isTrue(jarFile.exists(), "JarFile must exist");
		Assert.isTrue(jarFile.isFile(), "JarFile must be a file");
	}

}
