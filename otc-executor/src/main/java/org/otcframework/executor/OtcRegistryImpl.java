/**
* Copyright (c) otcframework.org
*
* @author  Franklin Abel
* @version 1.0
* @since   2020-06-08 
*
* This file is part of the OTC framework.
* 
*  The OTC framework is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, version 3 of the License.
*
*  The OTC framework is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  A copy of the GNU General Public License is made available as 'License.md' file, 
*  along with OTC framework project.  If not, see <https://www.gnu.org/licenses/>.
*
*/
package org.otcframework.executor;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.MessagePack;
import org.otcframework.common.OtcConstants;
import org.otcframework.common.config.OtcConfig;
import org.otcframework.common.dto.RegistryDto;
import org.otcframework.common.dto.RegistryDto.CompiledInfo;
import org.otcframework.common.dto.OtcChainDto;
import org.otcframework.common.dto.OtcCommandDto;
import org.otcframework.common.exception.OtcException;
import org.otcframework.common.executor.CodeExecutor;
import org.otcframework.common.factory.OtcCommandDtoFactory;
import org.otcframework.common.util.CommonUtils;
import org.otcframework.common.util.OtcReflectionUtil;
import org.otcframework.common.util.OtcUtils;
import org.otcframework.executor.exception.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Class OtcRegistryImpl.
 */
// TODO: Auto-generated Javadoc
public enum OtcRegistryImpl implements OtcRegistry {

	/** The instance. */
	instance;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(OtcRegistryImpl.class);

	/** The map packaged otc dtos. */
	private Map<String, RegistryDto> mapPackagedOtcDtos = new HashMap<>();

	/** The Constant depFileFilter. */
	private static final FileFilter depFileFilter = CommonUtils.createFilenameFilter(OtcConstants.OTC_TMD_EXTN);

	/** The Constant msgPack. */
	private static final MessagePack msgPack = new MessagePack();

	/** The Constant objectMapper. */
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/** The Constant clzLoader. */
	private static final URLClassLoader clzLoader = OtcConfig.getTargetClassLoader();

	/**
	 * Instantiates a new otc registry impl.
	 */
	private OtcRegistryImpl() {
	}

	/**
	 * register.
	 */
	@Override
	public void register() {
		String binDir = OtcConfig.getOtcTmdLocation();
		File directory = new File(binDir);
		File[] files = directory.listFiles(depFileFilter);
		if (files == null) {
			return;
		}
		LOGGER.info("Begining OTC registrations from {}", binDir);
		long startTime = System.nanoTime();
		boolean hasRegistrations = false;
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}
			try {
				FileInputStream fis = new FileInputStream(file);
				byte[] contents = msgPack.read(fis, byte[].class);
				RegistryDto registryDto = objectMapper.readValue(contents, RegistryDto.class);
				if (registryDto.isError) {
					LOGGER.error(
							"Ignoring registry of {}. "
									+ "Probable cause: full compilation did not succeed on previous attempt.",
							file.getAbsolutePath());
					continue;
				}
				for (CompiledInfo compiledInfo : registryDto.compiledInfos.values()) {
					// init source
					initOtcCommandDto(compiledInfo.id, compiledInfo.sourceOCDStem, registryDto.sourceClz,
							compiledInfo.sourceOtcChainDto);
					// init target
					initOtcCommandDto(compiledInfo.id, compiledInfo.targetOCDStem, registryDto.targetClz,
							compiledInfo.targetOtcChainDto);
				}
				register(registryDto);
				hasRegistrations = true;
			} catch (IOException e) {
				throw new RegistryException("", e);
			}
		}
		long endTime = System.nanoTime();
		if (hasRegistrations) {
			LOGGER.info("Completed OTC registrations in {} millis.", ((endTime - startTime) / 1000000.0));
		} else {
			LOGGER.info("Nothing to register - no registration files found !!");
		}
		return;
	}

	/**
	 * Inits the otc command dto.
	 *
	 * @param id            the id
	 * @param otcCommandDto the otc command dto
	 * @param clz           the clz
	 * @param otcChainDto   the otc chain dto
	 */
	private void initOtcCommandDto(String id, OtcCommandDto otcCommandDto, Class<?> clz, OtcChainDto otcChainDto) {
		if (otcCommandDto == null) {
			return;
		}
		Field field = OtcReflectionUtil.findField(clz, otcCommandDto.fieldName);
		otcCommandDto.field = field;
		if (otcCommandDto.children == null) {
			return;
		}
		for (int idx = 1; idx < otcChainDto.otcTokens.length; idx++) {
			String otcToken = otcChainDto.otcTokens[idx];
			if (otcCommandDto.isCollection() || otcCommandDto.isMap()) {
				OtcCommandDtoFactory.createMembers(id, otcCommandDto, otcChainDto.otcChain, otcChainDto.rawOtcTokens);
				if (otcCommandDto.isCollection()) {
					otcCommandDto = otcCommandDto.children.get(otcCommandDto.fieldName);
				} else if (otcCommandDto.isMap()) {
					if (otcChainDto.rawOtcTokens[otcCommandDto.otcTokenIndex].contains(OtcConstants.MAP_KEY_REF)) {
						otcCommandDto = otcCommandDto.children.get(OtcConstants.MAP_KEY_REF + otcCommandDto.fieldName);
					} else {
						otcCommandDto = otcCommandDto.children
								.get(OtcConstants.MAP_VALUE_REF + otcCommandDto.fieldName);
					}
				}
			}
			OtcCommandDto childOCD = otcCommandDto.children.get(otcToken);
			field = OtcReflectionUtil.findField(otcCommandDto.fieldType, childOCD.fieldName);
			childOCD.field = field;
			otcCommandDto = childOCD;
		}
		return;
	}

	/**
	 * register.
	 *
	 * @param registryDto the registry dto
	 */
	@Override
	public void register(RegistryDto registryDto) {
		if (registryDto == null) {
			LOGGER.warn("Nothing to register!");
			return;
		}
		String mainClass = registryDto.mainClass;
		// exception will be thrown for loadclass if class is not compiled.
		Class<?> mainClz = null;
		try {
			mainClz = OtcUtils.loadClass(mainClass);
		} catch (Exception ex) {
			try {
				mainClz = clzLoader.loadClass(mainClass);
			} catch (Exception e) {
				throw new OtcException("", e);
			}
		}
		CodeExecutor codeExecutor;
		try {
			codeExecutor = (CodeExecutor) mainClz.newInstance();
			registryDto.codeExecutor = codeExecutor;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new OtcException("", e);
		}
		mapPackagedOtcDtos.put(registryDto.registryId, registryDto);
		return;
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param otcNamespace the otc namespace
	 * @param source       the source
	 * @param targetClz    the target clz
	 * @return the registry dto
	 */
	@Override
	public RegistryDto retrieveRegistryDto(String otcNamespace, Object source, Class<?> targetClz) {
		String registryId = OtcUtils.createRegistryId(otcNamespace, source, targetClz);
		return retrieveRegistryDto(registryId);
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param otcNamespace the otc namespace
	 * @param sourceClz    the source clz
	 * @param targetClz    the target clz
	 * @return the registry dto
	 */
	@Override
	public RegistryDto retrieveRegistryDto(String otcNamespace, Class<?> sourceClz, Class<?> targetClz) {
		String registryId = OtcUtils.createRegistryId(otcNamespace, sourceClz, targetClz);
		return retrieveRegistryDto(registryId);
	}

	/**
	 * Retrieve registry dto.
	 *
	 * @param regisgryId the registry id
	 * @return the registry dto
	 */
	private RegistryDto retrieveRegistryDto(String registryId) {
		RegistryDto registryDto = mapPackagedOtcDtos.get(registryId);
		if (registryDto == null) {
			throw new RegistryException("",
					"OTC registry with ID '" + registryId + "' not found or not compiled and registered!");
		}
		return registryDto;
	}
}