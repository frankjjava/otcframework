/**
* Copyright (c) otclfoundation.org
*
* @author  Franklin Abel
* @version 1.0
* @since   2020-06-08 
*/
package org.otcl2.common.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.otcl2.common.config.exception.OtclConfigException;
import org.otcl2.common.exception.OtclException;
import org.otcl2.common.util.CommonUtils;
import org.otcl2.common.util.PackagesFilterUtil;
import org.otcl2.common.util.PropertyConverterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Enum OtclConfig.
 */
public enum OtclConfig {
	
	/** The instance. */
	instance;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OtclConfig.class);

	/** The Constant OTCL_HOME_ENV_VAR. */
	private static final String OTCL_HOME_ENV_VAR = "OTCL_HOME";

	/** The otcl source. */
//	private static final String OTCL_SOURCE = "/otcl-scripts-final";
	
	/** The otcl test source. */
	private static final String OTCL_TEST_SOURCE = "/otcl-scripts-unittest";
	
	/** The Constant COMPILER_CODEGEN_SOURCE_BASEDIR. */
	private static final String COMPILER_CODEGEN_SOURCE_BASEDIR = "compiler.codegen.source.basedir";
	
	/** The Constant COMPILER_TESTPROFILE_ENABLED. */
	private static final String COMPILER_TESTPROFILE_ENABLED = "compiler.testprofile.enable";

	/** The Constant EXECUTOR_PACKAGES_FILTER. */
	private static final String EXECUTOR_PACKAGES_FILTER = "executor.packages.filter";

	/** The Constant engineLogingDetailedDefault. */
	private static final boolean engineLogingDetailedDefault = true;
	
	/** The Constant compilerTestprofileEnableDefault. */
	private static final boolean compilerTestprofileEnableDefault = false;

	/** The Constant otclHome. */
	private static final String otclHome;
	
	/** The Constant otclConfigProps. */
	private static final Properties otclConfigProps = new Properties();
	
	/** The is test profile. */
	private static boolean isTestProfile = false;
	
	/** The Constant clzLoader. */
	private static final URLClassLoader clzLoader;

	/**
	 * Instantiates a new otcl config.
	 */
	private OtclConfig() {
	}

	static {
		Map<String, String> sysEnv = System.getenv();
		if (!sysEnv.containsKey(OTCL_HOME_ENV_VAR)) {
			throw new OtclConfigException("",
					"Oops... Cannot proceed - 'otcl_home' not set! Please set otcl_home environment variable.");
		}
		otclHome = sysEnv.get(OTCL_HOME_ENV_VAR);
		if (CommonUtils.isEmpty(otclHome)) {
			throw new OtclException("", "Oops... Environment variable 'otcl.home' not set! ");
		}
		try (InputStream inStream = new FileInputStream(otclHome + "/config/otcl.properties")) {
			otclConfigProps.load(inStream);
			if (!otclConfigProps.containsKey(EXECUTOR_PACKAGES_FILTER)) {
				throw new OtclConfigException("", "Oops... Cannot proceed - 'otcl.pkgsToInclude' not set in '"
						+ otclHome + "/config/otcl.properties' file");
			}
		} catch (IOException ex) {
			LOGGER.error(ex.getMessage());
			throw new OtclConfigException(ex);
		}
		String filteredPackages = otclConfigProps.getProperty(EXECUTOR_PACKAGES_FILTER);
		if (!filteredPackages.contains(",") && filteredPackages.contains(" ")) {
			filteredPackages = filteredPackages.replace("  ", " ").replace(" ", ",");
		}
		List<String> lstFilteredPackages = Arrays.asList(filteredPackages.split(","));
		PackagesFilterUtil.setFilteredPackages(lstFilteredPackages);
		URL url;
		try {
			String targetDir = getOtclTargetLocation();
			File binFolder = new File(targetDir);
			if (!binFolder.exists()) {
				binFolder.mkdir();
			}
			url = new File(targetDir).toURI().toURL();
			URL[] urls = new URL[] { url };
			clzLoader = URLClassLoader.newInstance(urls);
		} catch (MalformedURLException e) {
			throw new OtclConfigException(e);
		}
		isTestProfile = getConfigCompilerTestProfileEnabled();
	}

	/**
	 * Enables test-profile.
	 */
	public static void enableTestProfile() {
		isTestProfile = true;
	}

	/**
	 * Disables test-profile.
	 */
	public static void disableTestProfile() {
		isTestProfile = false;
	}

	/**
	 * Gets the otcl home location.
	 *
	 * @return the otcl home location
	 */
	public static String getOtclHomeLocation() {
		return otclHome;
	}

	/**
	 * Gets the otcl lib location.
	 *
	 * @return the otcl lib location
	 */
	public static String getOtclLibLocation() {
		if (CommonUtils.isEmpty(otclHome)) {
			throw new OtclException("", "Oops... Environment variable 'otcl.home' not set! ");
		}
		return otclHome + File.separator + "lib" + File.separator;
	}

	/**
	 * Gets the otcl source location.
	 *
	 * @return the otcl source location
	 */
	public static String getOtclSourceLocation() {
		if (CommonUtils.isEmpty(otclHome)) {
			throw new OtclException("", "Oops... Environment variable 'otcl.home' not set! ");
		}
		return otclHome + OTCL_TEST_SOURCE;
	}

	/**
	 * Gets the generated code source location.
	 *
	 * @return the generated code source location
	 */
	public static String getGeneratedCodeSourceLocation() {
		String sourceCodeLocation = null;
		if (otclConfigProps.containsKey(COMPILER_CODEGEN_SOURCE_BASEDIR)) {
			sourceCodeLocation = otclConfigProps.getProperty(COMPILER_CODEGEN_SOURCE_BASEDIR);
			if (!sourceCodeLocation.endsWith(File.separator)) {
				sourceCodeLocation += File.separator;
			}
		}
		if (CommonUtils.isEmpty(sourceCodeLocation)) {
			sourceCodeLocation = otclHome + File.separator + "src" + File.separator;
		}
		return sourceCodeLocation;
	}

	/**
	 * Gets the otcl bin location.
	 *
	 * @return the otcl bin location
	 */
	public static String getOtclBinLocation() {
		if (CommonUtils.isEmpty(otclHome)) {
			throw new OtclException("", "Oops... Environment variable 'otcl.home' not set! ");
		}
		return otclHome + File.separator + "bin" + File.separator;
	}

	/**
	 * Gets the otcl target location.
	 *
	 * @return the otcl target location
	 */
	public static String getOtclTargetLocation() {
		if (CommonUtils.isEmpty(otclHome)) {
			throw new OtclException("", "Oops... Environment variable 'otcl.home' not set! ");
		}
		return otclHome + File.separator + "target" + File.separator;
	}

	/**
	 * Gets the target class loader.
	 *
	 * @return the target class loader
	 */
	public static URLClassLoader getTargetClassLoader() {
		return clzLoader;
	}

	/**
	 * Gets the config compiler test profile enabled.
	 *
	 * @return the config compiler test profile enabled
	 */
	public static Boolean getConfigCompilerTestProfileEnabled() {
		if (otclConfigProps.containsKey(COMPILER_TESTPROFILE_ENABLED)) {
			return PropertyConverterUtil.toBooleanObject(otclConfigProps.getProperty(COMPILER_TESTPROFILE_ENABLED));
		}
		return compilerTestprofileEnableDefault;
	}
}
