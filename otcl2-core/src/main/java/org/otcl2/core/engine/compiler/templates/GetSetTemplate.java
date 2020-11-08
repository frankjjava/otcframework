package org.otcl2.core.engine.compiler.templates;

import java.util.Map;
import java.util.Set;

import org.otcl.dateconverters.MutualDateTypesConverterFacade;
import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.util.CommonUtils;
import org.otcl2.core.engine.compiler.command.SourceOtclCommandContext;
import org.otcl2.core.engine.compiler.command.TargetOtclCommandContext;
import org.otcl2.core.engine.compiler.exception.CodeGeneratorException;

public final class GetSetTemplate extends AbstractTemplate {

	private GetSetTemplate() {}

	public static String generateCode(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC, 
			boolean createNewVarName, Set<String> varNamesSet, Map<String, String> varNamesMap) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		if (targetOCD.isCollectionOrMap() || targetOCD.isCollectionOrMapMember()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId +
					". Type should not be a Collecton or Map member.");
		}
		OtclCommandDto sourceOCD = sourceOCC.otclCommandDto;
		String sourceVarName = null;
//		if (sourceOCD.isRootNode) {
//			sourceVarName = CommonUtils.initLower(sourceOCD.field.getDeclaringClass().getSimpleName());
//		} else {
			sourceVarName = createVarName(sourceOCD, createNewVarName, varNamesSet, varNamesMap);
//		}
		String targetParentVarName = null;
		String sourceParentVarName = null;
		if (targetOCD.isRootNode) {
			targetParentVarName = CommonUtils.initLower(targetOCD.field.getDeclaringClass().getSimpleName());
		} else {
			targetParentVarName = createVarName(targetOCD.parent, createNewVarName, varNamesSet, varNamesMap);
		}
		String getSetCode = null;
		if (targetOCD.enableFactoryHelperSetter || sourceOCD.enableFactoryHelperGetter) {
			if (sourceOCD.isRootNode) {
				sourceParentVarName = CommonUtils.initLower(sourceOCD.field.getDeclaringClass().getSimpleName());
			} else {
				sourceParentVarName = createVarName(sourceOCD.parent, createNewVarName, varNamesSet, varNamesMap);
			}
			String helper = targetOCC.factoryClassDto.addImport(targetOCC.helper);
			if (targetOCD.enableFactoryHelperSetter && sourceOCD.enableFactoryHelperGetter) {
				getSetCode = String.format(setHelperGetHelperTemplate, helper, targetOCD.setter, targetParentVarName, 
						helper, sourceOCD.getter, sourceParentVarName);	
			} else if (targetOCD.enableFactoryHelperSetter) {
				getSetCode = String.format(setHelperTemplate, helper, targetOCD.setter, targetParentVarName, 
						sourceParentVarName, sourceOCD.getter);
			} else {
				String targetVarName = null;
				if (targetOCD.isRootNode) {
					targetVarName = CommonUtils.initLower(targetOCD.field.getDeclaringClass().getSimpleName());
				} else {
					targetVarName = createVarName(targetOCD, createNewVarName, varNamesSet, varNamesMap);
				}
				getSetCode = String.format(getHelperTemplate, targetVarName, targetOCD.setter, targetParentVarName,
						helper, sourceOCD.getter, sourceParentVarName);	
			}
		} else {
			if (!sourceOCC.isLeaf()) {
				throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId +
						". Source token is not a leaf.");
			}
			if (targetOCD.isEnum() && sourceOCD.isEnum()) {
				String targetEnumType = fetchSanitizedTypeName(targetOCC, targetOCD);
				getSetCode = String.format(setterBothEnumTemplate, targetParentVarName, targetOCD.setter, 
						targetEnumType, sourceVarName);
			} else if (sourceOCD.isEnum()) {
				getSetCode = String.format(setterSourceEnumTemplate, targetParentVarName, targetOCD.setter,
						sourceVarName);
			} else if (targetOCD.isEnum()) {
				String targetEnumType = fetchSanitizedTypeName(targetOCC, targetOCD);
				getSetCode = String.format(setterTargetEnumTemplate, targetParentVarName, targetOCD.setter, 
						targetEnumType, sourceVarName);
			} else {
				if (MutualDateTypesConverterFacade.isOfAnyDateType(targetOCD.fieldType)) {
					targetOCC.factoryClassDto.addImport(MutualDateTypesConverterFacade.class.getName());
					if (MutualDateTypesConverterFacade.isOfAnyDateType(sourceOCD.fieldType)) {
							getSetCode = String.format(dateConverterTemplate, targetParentVarName, targetOCD.setter, 
									sourceVarName, targetOCD.fieldType);
					} else {
						if (String.class != sourceOCD.fieldType) {
							throw new CodeGeneratorException("", sourceOCD.fieldType + " in from: cannot be converted to " +
									targetOCD.fieldType + " in " + targetOCC.scriptId);
						}
//						String dateFormat = null;
//						if (targetOCC.scriptDto.copy.from.overrides != null) {
//							for (OtclFileDto.Override override : targetOCC.scriptDto.copy.from.overrides) {
//								if (override.dateFormat != null) {
//									dateFormat = override.dateFormat;
//									break;
//								}
//							}
//						}
//						if (dateFormat != null) {
//							getSetCode = String.format(formattedDateConverterTemplate, targetParentVarName,
//									targetOCD.setter, sourceVarName, sourceOCD.fieldType);
//						} else {
							getSetCode = String.format(dateConverterTemplate, targetParentVarName, targetOCD.setter,
									sourceVarName, sourceOCD.fieldType);
//						}
					}
				} else if (MutualDateTypesConverterFacade.isOfAnyDateType(sourceOCD.fieldType)) {
					targetOCC.factoryClassDto.addImport(MutualDateTypesConverterFacade.class.getName());
					if (String.class != targetOCD.fieldType) {
						throw new CodeGeneratorException("", sourceOCD.fieldType + " in from: cannot be converted to " +
								targetOCD.fieldType + " in " + targetOCC.scriptId);
					}
					getSetCode = String.format(dateToStringConverterTemplate, targetParentVarName, targetOCD.setter,
							sourceVarName);
				} else {
					getSetCode = String.format(setterTemplate, targetParentVarName, targetOCD.setter, sourceVarName);
				}
			}

		}
		return getSetCode;
	}
}