package org.otcl2.core.engine.compiler.templates;

import java.util.Set;

import org.otcl2.common.dto.ClassDto;
import org.otcl2.common.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassBeginTemplate extends AbstractTemplate {

	private ClassBeginTemplate() {}

	public static String generateMainClassCode(ClassDto mainClassDto, String targetType, String sourceType,
			String otclChain, Set<String> varNamesSet) {
		String rootTargetVariable = CommonUtils.initLower(targetType);
		rootTargetVariable = sanitizeVarName(rootTargetVariable, varNamesSet);
		String rootSourceVariable = null;
		if (sourceType != null) {
			rootSourceVariable = CommonUtils.initLower(sourceType);
			rootSourceVariable = sanitizeVarName(rootSourceVariable, varNamesSet);
		}
		String packageName = mainClassDto.packageName;
		String mainClassName = mainClassDto.className;
		if (packageName == null) {
			packageName = "";
		}
		if (sourceType == null) {
			sourceType = "Object";
			rootSourceVariable = "arg1";
		}
		String classBeginBody = String.format(mainClassBeginCodeTemplate, packageName, mainClassName, sourceType, 
				targetType, targetType, sourceType, rootSourceVariable, targetType, rootTargetVariable, targetType);
		if (CommonUtils.isEmpty(packageName)) {
			classBeginBody = classBeginBody.replace("package ;\n", "");
		}
		return classBeginBody;
	}
	
	public static String generateFactoryClassCode(ClassDto classDto, String sourceType, String targetType, 
			boolean addLogger, Set<String> varNamesSet) {
		return generateClassCode(classDto, sourceType, targetType, addLogger, factoryClassBeginCodeTemplate, varNamesSet);
	}
	
	public static String generateModuleClassCode(ClassDto classDto, String sourceType, String targetType,
			boolean addLogger, Set<String> varNamesSet) {
		return generateClassCode(classDto, sourceType, targetType, addLogger, factoryModuleClassBeginCodeTemplate,
				varNamesSet);
	}
	
	private static String generateClassCode(ClassDto classDto, String sourceType, String targetType, boolean addLogger, 
			String template, Set<String> varNamesSet) {
		String targetVar = CommonUtils.initLower(targetType);
		targetVar = sanitizeVarName(targetVar, varNamesSet);
		String sourceVar = null;
		if (sourceType != null) {	
			sourceVar = CommonUtils.initLower(sourceType);
		}
		sourceVar = sanitizeVarName(sourceVar, varNamesSet);
		String classBeginBody = null;
		String packageName = classDto.packageName;
		if (packageName == null) {
			packageName = "";
		}
		if (sourceType == null) {
			sourceType = "Object";
			sourceVar = "arg1";
		}
		if (!addLogger) {
			String supportClassBeginTemplateCopy = template.replace(loggerInitTemplate, "");
			classBeginBody = String.format(supportClassBeginTemplateCopy, packageName, classDto.className, 
					sourceType, sourceVar, targetType, targetVar);
		} else {
			classDto.addImport(Logger.class.getName());
			classDto.addImport(LoggerFactory.class.getName());
			classBeginBody = String.format(template, packageName, classDto.className, classDto.className, 
					sourceType, sourceVar, targetType, targetVar);
		}
		if (CommonUtils.isEmpty(packageName)) {
			classBeginBody = classBeginBody.replace("package ;\n", "");
		}
		return classBeginBody;
	}

}