package org.otcl2.core.engine.compiler.templates;

import java.util.Map;
import java.util.Set;

import org.otcl2.common.OtclConstants.TARGET_SOURCE;
import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.util.CommonUtils;
import org.otcl2.core.engine.compiler.command.TargetOtclCommandContext;
import org.otcl2.core.engine.compiler.exception.CodeGeneratorException;

public final class GetterIfNullCreateSetTemplate extends AbstractTemplate {

	private GetterIfNullCreateSetTemplate() {}

	public static String generateCode(TargetOtclCommandContext targetOCC, OtclCommandDto otclCommandDto,
			boolean createNewVarName, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		if (otclCommandDto.isArray()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId +
					". Type should not be an array.");
		}
		return generateCode(targetOCC, otclCommandDto, null, createNewVarName, varNamesSet, varNamesMap);
	}
	
	public static String generateCodeForArray(TargetOtclCommandContext targetOCC, OtclCommandDto otclCommandDto,
			Integer arraySize, boolean createNewVarName, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		if (!otclCommandDto.isArray()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					". Type should be an array.");
		}
		return generateCode(targetOCC, otclCommandDto, arraySize, createNewVarName, varNamesSet, varNamesMap);
	}
	
	private static String generateCode(TargetOtclCommandContext targetOCC, OtclCommandDto otclCommandDto,
			Integer arraySize, boolean createNewVarName, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		String concreteType = fetchConcreteTypeName(targetOCC, otclCommandDto);
		String fieldType = fetchFieldTypeName(targetOCC, null, otclCommandDto, createNewVarName, varNamesMap);
		String varName = createVarName(otclCommandDto, createNewVarName, varNamesSet, varNamesMap);
		if (otclCommandDto.isArray()) {
			if (TARGET_SOURCE.TARGET == otclCommandDto.enumTargetSource) {
				if (arraySize != null) {
					concreteType = concreteType.replace("[]","[" + arraySize + "]");
				} else {
					concreteType = concreteType.replace("[]","[" + 1 + "]");
				}
			}
		}
		String parentVarName = null;
		if (otclCommandDto.isRootNode) {
			parentVarName = CommonUtils.initLower(otclCommandDto.field.getDeclaringClass().getSimpleName());
		} else {
			parentVarName = createVarName(otclCommandDto.parent, createNewVarName, varNamesSet, varNamesMap);
		}
		String getter = otclCommandDto.getter;
		String getterCode = null;
		if (otclCommandDto.enableFactoryHelperGetter) {
			String helper = targetOCC.factoryClassDto.addImport(targetOCC.helper);
			getterCode = String.format(helperGetterTemplate, fieldType, varName, helper, getter, parentVarName);
		} else {
			getterCode = String.format(getterTemplate, fieldType, varName, parentVarName, getter);
		}
		String ifNullCreateAndSetCode = IfNullCreateAndSetTemplate.generateCode(targetOCC, arraySize, createNewVarName,
				varNamesSet, varNamesMap);
		return getterCode + ifNullCreateAndSetCode;
	}

}