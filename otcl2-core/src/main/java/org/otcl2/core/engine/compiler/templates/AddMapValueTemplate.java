package org.otcl2.core.engine.compiler.templates;

import java.util.Map;
import java.util.Set;

import org.otcl2.common.OtclConstants;
import org.otcl2.common.OtclConstants.LogLevel;
import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.util.PackagesFilterUtil;
import org.otcl2.core.engine.compiler.command.TargetOtclCommandContext;
import org.otcl2.core.engine.compiler.exception.CodeGeneratorException;

public final class AddMapValueTemplate extends AbstractTemplate {

	private AddMapValueTemplate() {}

	public static String generateCode(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, boolean createNewVarName,
			String value, Integer idx, LogLevel logLevel, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		return generateCode(targetOCC, sourceOCD, createNewVarName, value, idx, null, logLevel, varNamesSet, varNamesMap);
	}
	
	public static String generateCode(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, boolean createNewVarName,
			String idxVar, LogLevel logLevel, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		return generateCode(targetOCC, sourceOCD, createNewVarName, null, null, idxVar, logLevel, varNamesSet, varNamesMap);
	}
	
	private static String generateCode(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, boolean createNewVarName,
			String value, Integer idx, String idxVar, LogLevel logLevel, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		OtclCommandDto valueOCD = targetOCC.otclCommandDto;
		if (!valueOCD.isMapValue()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					". Command Object is not of Map-value type.");
		}
		StringBuilder codeSectionBuilder = new StringBuilder();
		OtclCommandDto parentOCD = valueOCD.parent;
		OtclCommandDto keyOCD = parentOCD.children.get(OtclConstants.MAP_KEY_REF + parentOCD.fieldName);
		String keyFieldType = fetchFieldTypeName(targetOCC, null, keyOCD, createNewVarName, varNamesMap);
		String keyFieldTypecastType = fetchSanitizedTypeName(targetOCC, keyOCD);
		String keyVarName = createVarName(keyOCD, createNewVarName, varNamesSet, varNamesMap);
		String getMapKeyValueICDCode = null;
		String keyPcdId = createIcdKey(keyOCD, idxVar, idx);
		String valuePcdId = createIcdKey(valueOCD, idxVar, idx);
		if (idxVar != null || idx != null) {
			int endIdx = targetOCC.otclChain.lastIndexOf(OtclConstants.MAP_VALUE_REF) + 3;
			String mapValueTokenPath = targetOCC.otclChain.substring(0, endIdx);
			String logMsg = "Corresponding Map-key missing for path: '" + mapValueTokenPath + "'!";
			getMapKeyValueICDCode = String.format(ifNullMapKeyIcdReturnTemplate, keyPcdId, logLevel, logMsg, valuePcdId);
			if (targetOCC.hasDescendantCollectionOrMap()) {
				getMapKeyValueICDCode += assignValueToMemberIcdTemplate;
			}
		} else {
			getMapKeyValueICDCode = String.format(retrieveLastMapKeyTemplate, keyPcdId, valuePcdId);
		}
		codeSectionBuilder.append(getMapKeyValueICDCode);
		String retrieveMapKeyFromICDCode = String.format(retrieveMapKeyFromIcdTemplate, keyFieldType, keyVarName, 
				keyFieldTypecastType);
		codeSectionBuilder.append(retrieveMapKeyFromICDCode);
		String valueFieldType = fetchFieldTypeName(targetOCC, null, valueOCD, false, varNamesMap);
		String valOrVar = fetchValueOrVar(targetOCC, sourceOCD, value, createNewVarName, varNamesSet, varNamesMap);
		String valueFieldCastType = fetchSanitizedTypeName(targetOCC, valueOCD);
		String valueVarName = createVarName(valueOCD, false, varNamesSet, varNamesMap);

		String mapValueCode = null;
		if (targetOCC.isLeaf()) {
			String createInstanceTemplateCopy = createInstanceTemplate.replace("new %s()", valOrVar);
			mapValueCode = String.format(createInstanceTemplateCopy, valueFieldType, valueVarName);
			codeSectionBuilder.append(mapValueCode);
			String getterCode = GetterIfNullReturnTemplate.generateCode(targetOCC, valueOCD.parent, createNewVarName,
					varNamesSet, varNamesMap);
			codeSectionBuilder.append(getterCode);
			String mapVarName = createVarName(valueOCD.parent, createNewVarName, varNamesSet, varNamesMap);
			String addMapEntryUpdatePcdCode = String.format(addMapEntryUpdateIcdTemplate, mapVarName, keyVarName,
					valueVarName, valueVarName);
			codeSectionBuilder.append(addMapEntryUpdatePcdCode);
		} else if (PackagesFilterUtil.isFilteredPackage(valueOCD.fieldType)) {
			if (valueOCD.isEnum()) {
				String createInstanceTemplateCopy = createInstanceTemplate.replace("new %s()", valOrVar);
				mapValueCode = String.format(createInstanceTemplateCopy, valueFieldType, valueVarName);
			} else {
				mapValueCode = String.format(retrieveMapValueFromIcdTemplate, valueFieldType, valueVarName, 
					valueFieldCastType);
			}
			codeSectionBuilder.append(mapValueCode);
		}
		return codeSectionBuilder.toString();
	}
	
	public static String generatePostLoopCode(TargetOtclCommandContext targetOCC, boolean createNewVarName, Integer idx,
			String idxVar, LogLevel logLevel, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		OtclCommandDto valueOCD = targetOCC.otclCommandDto;
		if (!valueOCD.isMapValue()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId +
					". Command Object is not of Map-key type.");
		}
		String valueType = fetchFieldTypeName(targetOCC, null, valueOCD, createNewVarName, varNamesMap);
		String valueVarName = createVarName(valueOCD, createNewVarName, varNamesSet, varNamesMap);
		String valueConcreteType = fetchConcreteTypeName(targetOCC, valueOCD);
		boolean hasMapValueInPath = valueOCD.isMapValue() || targetOCC.hasMapValueDescendant();
		String logMsg = null;
		if (hasMapValueInPath) {
			int endIdx = targetOCC.otclChain.lastIndexOf(OtclConstants.MAP_VALUE_REF) + 3;
			String mapValueTokenPath = targetOCC.otclChain.substring(0, endIdx);
			logMsg = "Corresponding Map-key missing for path: '" + mapValueTokenPath + "'!";
		} else {
			logMsg = "'" + valueOCD.tokenPath + "' is null!.";
		}
		String icd = null;
		if (targetOCC.hasAncestralCollectionOrMap()) {
			icd = "parentICD" + idx;
		} else {
			icd = "parentTargetICD";
		}
		String pdcId = createIcdKey(valueOCD, idxVar, null);
		String postTargetLoopMapValueCode = String.format(postTargetLoopMapValueTemplate, idx, icd, pdcId, idx, logLevel,
				logMsg, valueType, valueVarName, valueConcreteType, idx);
		return postTargetLoopMapValueCode;
	}
}