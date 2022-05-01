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
package org.otcframework.core.engine.compiler;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.otcframework.common.OtcConstants;
import org.otcframework.common.OtcConstants.TARGET_SOURCE;
import org.otcframework.common.dto.OtcCommandDto;
import org.otcframework.common.dto.ScriptDto;
import org.otcframework.common.dto.otc.OtcFileDto.Copy;
import org.otcframework.common.dto.otc.OtcFileDto.Execute;
import org.otcframework.common.dto.otc.OverrideDto;
import org.otcframework.common.dto.otc.TargetDto;
import org.otcframework.common.exception.OtcException;
import org.otcframework.common.factory.OtcCommandDtoFactory;
import org.otcframework.common.util.OtcReflectionUtil;
import org.otcframework.common.util.OtcUtils;
import org.otcframework.common.util.PackagesFilterUtil;
import org.otcframework.core.engine.compiler.exception.SemanticsException;

/**
 * The Class OtcSemanticsChecker.
 */
// TODO: Auto-generated Javadoc
final class OtcSemanticsChecker {

	private static final String err_msg = "Oops... OTC-command didn't pass Semantics-Checker for Id : ";
	/**
	 * Check semantics.
	 *
	 * @param script        the script
	 * @param clz           the clz
	 * @param otcChain      the otc chain
	 * @param otcCommandDto the otc command dto
	 * @param otcTokens     the otc tokens
	 * @return true, if successful
	 */
	static boolean checkSemantics(ScriptDto script, Class<?> clz, String otcChain, OtcCommandDto otcCommandDto,
			String[] otcTokens) {
		try {
			checkNotations(script, clz, otcChain, otcCommandDto);
			String concreteTypeName = otcCommandDto.concreteTypeName;
			Class<?> concreteType = null;
			if (concreteTypeName != null && otcCommandDto.concreteTypeName == null) {
				concreteType = OtcUtils.loadClass(concreteTypeName);
				otcCommandDto.concreteType = concreteType;
			}
			GetterSetterOverridesProcessor.process(script, otcCommandDto);
			OtcCommandDtoFactory.createMembers(script.command.id, otcCommandDto, otcChain, otcTokens);
		} catch (Exception ex) {
			if (ex instanceof OtcException) {
				throw ex;
			} else {
				throw new SemanticsException("",
						"Oops... Semantics-Checker error in OTC-command : " + script.command.id
								+ (TARGET_SOURCE.SOURCE == otcCommandDto.enumTargetSource ? " in from/source field '"
										: " in to/target field '")
								+ otcCommandDto.fieldName + "' not found.");
			}
		}
		return true;
	}

	/**
	 * Check notations.
	 *
	 * @param script        the script
	 * @param clz           the clz
	 * @param otcChain      the otc chain
	 * @param otcCommandDto the otc command dto
	 */
	private static void checkNotations(ScriptDto script, Class<?> clz, String otcChain, OtcCommandDto otcCommandDto) {
		if (otcCommandDto.fieldName.equals(OtcConstants.ROOT)) {
			otcCommandDto.declaringClass = clz;
			return;
		}
		Field field = OtcReflectionUtil.findField(clz, otcCommandDto.fieldName);
		if (field == null) {
			throw new SemanticsException("",
					"Oops... Semantics-Checker error in OTC-command : " + script.command.id
							+ (TARGET_SOURCE.SOURCE == otcCommandDto.enumTargetSource ? " in from/source field '"
									: " in to/target field '")
							+ otcCommandDto.fieldName + "' not found.");
		}
		Class<?> fieldType = field.getType();
		otcCommandDto.field = field;
		otcCommandDto.fieldType = fieldType;
		List<TargetDto.Override> targetOverrides = null;
		String targetOtcChain = null;
		List<OverrideDto> sourceOverrides = null;
		String sourceOtcChain = null;
		if (script.command instanceof Execute) {
			Execute execute = (Execute) script.command;
			if (execute.otclModule != null || execute.otclConverter != null) {
				String typeName = otcCommandDto.fieldType.getName();
				// TODO this seem to be right - may need correction
				if (!PackagesFilterUtil.isFilteredPackage(typeName) && !otcCommandDto.hasCollectionNotation
						&& !otcCommandDto.hasMapNotation) {
					throw new SemanticsException("", err_msg + execute.id + " - Type : '" + typeName + 
							"' not included in filter found.");
				}
			}
			targetOverrides = execute.target.overrides;
			targetOtcChain = execute.target.objectPath;
			sourceOverrides = execute.source.overrides;
			sourceOtcChain = execute.source.objectPath;
		} else {
			Copy copy = (Copy) script.command;
			targetOverrides = copy.to.overrides;
			targetOtcChain = copy.to.objectPath;
			sourceOverrides = copy.from.overrides;
			sourceOtcChain = copy.from.objectPath;
		}
		if (targetOverrides != null && targetOtcChain == null) {
			throw new SemanticsException("", err_msg + script.command.id + 
					" 'target: overrides' may be defined only if 'target: otcChain' is defined.");
		}
		if (sourceOverrides != null && sourceOtcChain == null) {
			throw new SemanticsException("", err_msg + script.command.id + 
					" 'source: overrides' may be defined only if 'source: otcChain' is defined.");
		}
		if (otcCommandDto.hasCollectionNotation) {
			boolean isCollection = Collection.class.isAssignableFrom(fieldType);
			if (!isCollection) {
				boolean isArray = fieldType.isArray();
				if (!isArray) {
					throw new SemanticsException("",err_msg + script.command.id
									+ ". Field is not a Collection/Array, but Collection-notation is found.");
				}
			}
		} else if (otcCommandDto.hasMapNotation) {
			boolean isMap = Map.class.isAssignableFrom(fieldType);
			if (!isMap) {
				throw new SemanticsException("", err_msg + script.command.id + 
						". Field is not a Map, but Map-notation is found.");
			}
		}
	}
}
