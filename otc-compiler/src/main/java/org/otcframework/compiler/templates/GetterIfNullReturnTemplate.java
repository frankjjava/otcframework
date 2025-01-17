/**
* Copyright (c) otcframework.org
*
* @author  Franklin J Abel
* @version 1.0
* @since   2020-06-08 
*
* This file is part of the OTC framework.
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
*
*/
package org.otcframework.compiler.templates;

import org.otcframework.common.OtcConstants;
import org.otcframework.common.OtcConstants.LogLevel;
import org.otcframework.common.OtcConstants.TARGET_SOURCE;
import org.otcframework.common.dto.OtcCommandDto;
import org.otcframework.common.util.CommonUtils;
import org.otcframework.compiler.command.SourceOtcCommandContext;
import org.otcframework.compiler.command.TargetOtcCommandContext;

import java.util.Map;
import java.util.Set;

/**
 * The Class GetterIfNullReturnTemplate.
 */
// TODO: Auto-generated Javadoc
public final class GetterIfNullReturnTemplate extends AbstractTemplate {

	private static final String inlineComments = "\n// ---- generator - " +
			GetterIfNullReturnTemplate.class.getSimpleName() + "\n";

	/**
	 * Instantiates a new getter if null return template.
	 */
	private GetterIfNullReturnTemplate() {
	}

	/**
	 * Generate code.
	 *
	 * @param targetOCC        the target OCC
	 * @param otcCommandDto    the otc command dto
	 * @param createNewVarName the create new var name
	 * @param varNamesSet      the var names set
	 * @param varNamesMap      the var names map
	 * @return the string
	 */
	public static String generateCode(TargetOtcCommandContext targetOCC, OtcCommandDto otcCommandDto,
			boolean createNewVarName, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		String fieldType = fetchFieldTypeName(targetOCC, null, otcCommandDto, createNewVarName, varNamesMap);
		String varName = createVarName(otcCommandDto, createNewVarName, varNamesSet, varNamesMap);
		String parentVarName = null;
		if (otcCommandDto.isFirstNode) {
			parentVarName = CommonUtils.initLower(otcCommandDto.field.getDeclaringClass().getSimpleName());
		} else {
			parentVarName = createVarName(otcCommandDto.parent, false, varNamesSet, varNamesMap);
		}
		String getterName = otcCommandDto.getter;
		String getterCode = null;
		if (otcCommandDto.enableGetterHelper) {
			String helper = targetOCC.factoryClassDto.addImport(targetOCC.helper);
			getterCode = String.format(helperGetterTemplate, fieldType, varName, helper, getterName, parentVarName);
		} else {
			getterCode = String.format(getterTemplate, fieldType, varName, parentVarName, getterName);
		}
		return addInlineComments(inlineComments, getterCode);
	}

	/**
	 * Generate getter if null return code.
	 *
	 * @param targetOCC        the target OCC
	 * @param createNewVarName the create new var name
	 * @param logLevel         the log level
	 * @param varNamesSet      the var names set
	 * @param varNamesMap      the var names map
	 * @return the string
	 */
	public static String generateGetterIfNullReturnCode(TargetOtcCommandContext targetOCC, boolean createNewVarName,
			LogLevel logLevel, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		return generateGetterIfNullReturnCode(targetOCC, null, createNewVarName, logLevel, varNamesSet, varNamesMap);
	}

	/**
	 * Generate getter if null return code.
	 *
	 * @param targetOCC        the target OCC
	 * @param sourceOCC        the source OCC
	 * @param createNewVarName the create new var name
	 * @param logLevel         the log level
	 * @param varNamesSet      the var names set
	 * @param varNamesMap      the var names map
	 * @return the string
	 */
	public static String generateGetterIfNullReturnCode(TargetOtcCommandContext targetOCC,
			SourceOtcCommandContext sourceOCC, boolean createNewVarName, LogLevel logLevel, Set<String> varNamesSet,
			Map<String, String> varNamesMap) {
		OtcCommandDto otcCommandDto = null;
		boolean hasMapValueInPath = false;
		if (sourceOCC != null) {
			otcCommandDto = sourceOCC.otcCommandDto;
			hasMapValueInPath = sourceOCC.hasMapValueMember() || sourceOCC.hasMapValueDescendant();
		} else {
			otcCommandDto = targetOCC.otcCommandDto;
			hasMapValueInPath = targetOCC.hasMapValueMember() || targetOCC.hasMapValueDescendant();
		}
		String fieldType = fetchFieldTypeName(targetOCC, null, otcCommandDto, createNewVarName, varNamesMap);
		String varName = createVarName(otcCommandDto, createNewVarName, varNamesSet, varNamesMap);
		String parentVarName = null;
		if (otcCommandDto.isFirstNode) {
			parentVarName = CommonUtils.initLower(otcCommandDto.field.getDeclaringClass().getSimpleName());
		} else {
			OtcCommandDto parentOCD = otcCommandDto.parent;
			parentVarName = createVarName(parentOCD, createNewVarName, varNamesSet, varNamesMap);
		}
		String getter = otcCommandDto.getter;
		String ifNotNullParentChildGetterCode = null;
		String logMsg = null;
		if (TARGET_SOURCE.SOURCE == otcCommandDto.enumTargetSource || !hasMapValueInPath) {
			logMsg = "'" + otcCommandDto.tokenPath + "' is null!.";
		} else {
			int endIdx = targetOCC.otcChain.lastIndexOf(OtcConstants.MAP_VALUE_REF) + 3;
			String mapValueTokenPath = targetOCC.otcChain.substring(0, endIdx);
			logMsg = "Corresponding Map-key missing for path: '" + mapValueTokenPath + "'!";
		}
		if (otcCommandDto.enableGetterHelper) {
			String helper = targetOCC.factoryClassDto.addImport(targetOCC.helper);
			ifNotNullParentChildGetterCode = String.format(helperGetIfNullReturnTemplate, fieldType, varName, helper,
					getter, parentVarName, varName, logLevel, logMsg);
		} else {
			ifNotNullParentChildGetterCode = String.format(getterIfNullReturnTemplate, fieldType, varName,
					parentVarName, getter, varName, logLevel, logMsg);
		}
		return addInlineComments(inlineComments, ifNotNullParentChildGetterCode);
	}
}
