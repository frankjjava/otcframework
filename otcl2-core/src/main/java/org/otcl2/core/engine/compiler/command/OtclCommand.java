/**
* Copyright (c) otcl2.org
*
* @author  Franklin Abel
* @version 1.0
* @since   2020-06-08 
*/
package org.otcl2.core.engine.compiler.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.otcl2.common.OtclConstants;
import org.otcl2.common.OtclConstants.LogLevel;
import org.otcl2.common.OtclConstants.TARGET_SOURCE;
import org.otcl2.common.config.OtclConfig;
import org.otcl2.common.dto.ClassDto;
import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.engine.compiler.OtclCommandContext;
import org.otcl2.core.engine.compiler.exception.CodeGeneratorException;
import org.otcl2.core.engine.compiler.templates.AbstractTemplate;
import org.otcl2.core.engine.compiler.templates.AddMapKeyTemplate;
import org.otcl2.core.engine.compiler.templates.AddMapValueTemplate;
import org.otcl2.core.engine.compiler.templates.AddToCollectionTemplate;
import org.otcl2.core.engine.compiler.templates.ClassBeginTemplate;
import org.otcl2.core.engine.compiler.templates.ExecuteConverterTemplate;
import org.otcl2.core.engine.compiler.templates.ExecuteFactoryMethodCallTemplate;
import org.otcl2.core.engine.compiler.templates.ExecuteModuleTemplate;
import org.otcl2.core.engine.compiler.templates.ForLoopTemplate;
import org.otcl2.core.engine.compiler.templates.GetSetTemplate;
import org.otcl2.core.engine.compiler.templates.GetterIfNullCreateSetTemplate;
import org.otcl2.core.engine.compiler.templates.GetterIfNullReturnTemplate;
import org.otcl2.core.engine.compiler.templates.IfNullContinueTemplate;
import org.otcl2.core.engine.compiler.templates.MethodEndTemplate;
import org.otcl2.core.engine.compiler.templates.PcdInitTemplate;
import org.otcl2.core.engine.compiler.templates.PreloopVarsTemplate;
import org.otcl2.core.engine.compiler.templates.RetrieveMemberFromPcdTemplate;
import org.otcl2.core.engine.compiler.templates.SetterTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtclCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(OtclCommand.class);
	public static final String CODE_TO_IMPORT = "CODE_TO_IMPORT";

	private static final String otclBinDir = OtclConfig.getOtclTargetLocation();
	private static final String otclSourceDir = OtclConfig.getOtclSourceLocation();
	private static final String sourceFileLocation = OtclConfig.getGeneratedCodeSourceLocation();
	
	private Set<String> varNamesSet = new HashSet<>();
	private Map<String, String> varNamesMap = new HashMap<>();

	public void clearCache() {
		varNamesSet.clear();
		varNamesMap.clear();
	}
	
	public void clearTargetCache() {
		Iterator<Entry<String, String>> iterator = varNamesMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, String> entry = iterator.next();
			if (entry.getKey().startsWith(TARGET_SOURCE.TARGET.name())) {
				iterator.remove();
			}
		}
	}
	
	public String createJavaFile(ClassDto classDto) {
		File file = new File(sourceFileLocation);
		if (!file.exists()) {
			file.mkdir();
		}
		String fileName = classDto.fullyQualifiedClassName.replace(".", File.separator) + ".java"; 
		String fileLocationAndName = sourceFileLocation + File.separator + fileName;
		file = new File(fileLocationAndName);
		FileOutputStream fileOutputStream = null;
		File dir = null;
		if (classDto.packageName == null) {
			dir = new File(sourceFileLocation);
		} else {
			dir = new File(sourceFileLocation  + File.separator + classDto.packageName.replace(".", File.separator));
		}
		try {
			if (!dir.exists() ) {
				dir.mkdirs();
			}
			file.createNewFile();
			fileOutputStream = new FileOutputStream(file);
			String javaCode = classDto.codeBuilder.toString();
			javaCode = JavaCodeFormatter.format(javaCode);
			fileOutputStream.write(javaCode.getBytes()); 
		} catch (IOException e) {
			LOGGER.warn("", e);
		} finally {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				LOGGER.warn("", e);
			}
		}
		return fileName;
	}

	public JavaCodeStringObject createJavaFile(TargetOtclCommandContext targetOCC, Class<?> targetClz, 
			Class<?> sourceClz) {
		String methodEndCode = MethodEndTemplate.generateCode("");
		targetOCC.appendCode(methodEndCode);
		targetOCC.appendCode("\n}");
		StringBuilder importsBuilder = new StringBuilder();
		for (String fqTypeName : targetOCC.factoryClassDto.retrieveImportFqNames()) {
			importsBuilder.append("\nimport ").append(fqTypeName).append(";");
		}
		StringBuilder codeBuilder = targetOCC.factoryClassDto.codeBuilder;
		int idx = codeBuilder.indexOf(CODE_TO_IMPORT);
		codeBuilder.replace(idx, idx + CODE_TO_IMPORT.length(), importsBuilder.toString());
		createJavaFile(targetOCC.factoryClassDto);
		String fqClzName = targetOCC.factoryClassDto.className;
		String factoryClass = codeBuilder.toString();
		JavaCodeStringObject javaStringObject = new JavaCodeStringObject(fqClzName, factoryClass);
		return javaStringObject;
	}
	
	public static OtclCommandDto retrieveMemberOCD(OtclCommandContext otclCommandContext) {
		OtclCommandDto otclCommandDto = otclCommandContext.otclCommandDto;
		String[] rawOtclTokens = otclCommandContext.rawOtclTokens;
		if (!otclCommandDto.isCollectionOrMap()) {
			throw new CodeGeneratorException("", 
					"Invalid call to method! Token should be for a " + "Map / collection / array in "
							+ otclCommandDto.tokenPath);
		}
		String memberName = otclCommandDto.fieldName;
		if (otclCommandDto.isMap()) {
			if (rawOtclTokens[otclCommandDto.otclTokenIndex].contains(OtclConstants.MAP_KEY_REF)) {
				memberName = OtclConstants.MAP_KEY_REF + memberName;
			} else {
				memberName = OtclConstants.MAP_VALUE_REF + memberName;
			}
		}
		otclCommandDto = otclCommandDto.children.get(memberName);
		return otclCommandDto;
	}

	public static OtclCommandDto retrieveNextOCD(OtclCommandContext otclCommandContext) {
		OtclCommandDto otclCommandDto = otclCommandContext.otclCommandDto;
		OtclCommandDto childOCD = otclCommandContext.otclCommandDto;
		if (childOCD.isCollectionOrMap()) {
			childOCD = retrieveMemberOCD(otclCommandContext);
			otclCommandContext.otclCommandDto = childOCD;
		}
		if (otclCommandContext.hasChildren()) { 
			childOCD = childOCD.children.get(otclCommandContext.otclTokens[childOCD.otclTokenIndex + 1]);
			otclCommandContext.otclCommandDto = otclCommandDto;
		}
		return childOCD;
	}

	public static OtclCommandDto retrieveNextCollectionOrMapOCD(OtclCommandContext otclCommandContext) {
		if (!otclCommandContext.hasDescendantCollectionOrMap()) {
			throw new CodeGeneratorException("", 
					"Invalid call to method! Token does not contain decendant Collection / Map.");
		}
		OtclCommandDto childOCD = otclCommandContext.otclCommandDto;
		while (true) {
			childOCD = retrieveNextOCD(otclCommandContext);
			otclCommandContext.otclCommandDto = childOCD;
			if (childOCD.isCollectionOrMap()) {
				break;
			}
		}
		return childOCD;
	}

	public static OtclCommandDto retrieveLeafOCD(OtclCommandContext otclCommandContext) {
		OtclCommandDto otclCommandDto = otclCommandContext.otclCommandDto;
		String[] otclTokens = otclCommandContext.otclTokens;
		String[] rawOtclTokens = otclCommandContext.rawOtclTokens;
		OtclCommandContext clonedOCC = otclCommandContext.clone();
		while (true) {
			if (otclCommandDto.isCollectionOrMap()) {
				String memberName = otclCommandDto.fieldName;
				if (otclCommandDto.isMap()) {
					if (rawOtclTokens[otclCommandDto.otclTokenIndex].contains(OtclConstants.MAP_KEY_REF)) {
						memberName = OtclConstants.MAP_KEY_REF + memberName;
					} else {
						memberName = OtclConstants.MAP_VALUE_REF + memberName;
					}
				}
				otclCommandDto = otclCommandDto.children.get(memberName);
				clonedOCC.otclCommandDto = otclCommandDto;
			}
			if (!clonedOCC.hasChildren() || clonedOCC.isLeaf()) { 
				break;
			}
			otclCommandDto = otclCommandDto.children.get(otclTokens[otclCommandDto.otclTokenIndex + 1]);
			clonedOCC.otclCommandDto = otclCommandDto;
		}
		return otclCommandDto;
	}

	private void appendMethodCall(TargetOtclCommandContext targetOCC,  Class<?> targetClz, Class<?> sourceClz,
			boolean isModule) {
		StringBuilder executeMethodCallCodeBuilder = new StringBuilder("\n");
		String factoryMethodCallCode = null;
		String factoryClzName = targetOCC.factoryClassDto.fullyQualifiedClassName;
		factoryMethodCallCode = ExecuteFactoryMethodCallTemplate.generateCode(factoryClzName, targetClz, sourceClz);
		executeMethodCallCodeBuilder.append(factoryMethodCallCode);
		targetOCC.mainClassDto.codeBuilder.append(executeMethodCallCodeBuilder);
	}
	
	public void appendBeginModuleClass(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			Class<?> targetClz, Class<?> sourceClz, boolean addLogger) {
		appendBeginClass(targetOCC, sourceOCC, targetClz, sourceClz, addLogger, true);
	}

	public void appendBeginClass(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			Class<?> targetClz, Class<?> sourceClz, boolean addLogger) {
		appendBeginClass(targetOCC, sourceOCC, targetClz, sourceClz, addLogger, false);
	}
	
	private void appendBeginClass(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			Class<?> targetClz, Class<?> sourceClz, boolean addLogger, boolean isModule) {
		String fileName = targetOCC.factoryClassDto.fullyQualifiedClassName.replace(".", File.separator);
		File file = new File(otclBinDir + fileName + ".class");
		file.delete();
		file = new File(otclSourceDir + fileName + ".java");
		file.delete();
		targetOCC.factoryClassDto.codeBuilder = new StringBuilder();
		targetOCC.factoryClassDto.clearImports();
		targetOCC.factoryClassDto.addImport(Map.class.getName());
		String factoryClassBegin = null;
		String targetType = targetOCC.factoryClassDto.addImport(targetClz.getName());
		String sourceType = null;
		if (sourceClz != null) {
			sourceType = targetOCC.factoryClassDto.addImport(sourceClz.getName());
		}
		if (isModule) {
			factoryClassBegin = ClassBeginTemplate.generateModuleClassCode(targetOCC.factoryClassDto, sourceType, 
					targetType, addLogger, varNamesSet);
		} else {
			factoryClassBegin = ClassBeginTemplate.generateFactoryClassCode(targetOCC.factoryClassDto, sourceType, 
					targetType, addLogger, varNamesSet);
		}
		factoryClassBegin += PcdInitTemplate.generateMemberPcdCode(targetOCC, sourceOCC, varNamesSet);
		appendMethodCall(targetOCC, targetClz, sourceClz, isModule); 
		targetOCC.appendCode(factoryClassBegin);
		return;
	}

	public void appendPreloopVars(TargetOtclCommandContext targetOCC) {
		targetOCC.appendCode(PreloopVarsTemplate.generateCode());
		return;
	}
	
	public void appendAssignAnchoredPcdToParentPcd(TargetOtclCommandContext targetOCC) {
		String assignAnchoredPcdToMemberPcdCode = PcdInitTemplate.generateAssignAnchoredPcdToParentPcdTemplateCode();
		targetOCC.appendCode(assignAnchoredPcdToMemberPcdCode);
		return;
	}
	
	public void appendAssignParentPcdToAnchoredPcd(TargetOtclCommandContext targetOCC) {
		String assignAnchoredPcdToMemberPcdCode = PcdInitTemplate.generateAssignParentPcdToAnchoredPcdTemplateCode();
		targetOCC.appendCode(assignAnchoredPcdToMemberPcdCode);
		return;
	}
	
	public void appendRetrieveNextSourceCollectionOrMapParent(TargetOtclCommandContext targetOCC,
			SourceOtclCommandContext sourceOCC, int idx, boolean createNewVarName, LogLevel logLevel) {
		OtclCommandDto sourceOCD = sourceOCC.otclCommandDto;
		if (!sourceOCD.isCollectionOrMap()) {
			if (!sourceOCC.hasDescendantCollectionOrMap()) {
				throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"!. Token does not have descentant Collection/Map.");
			}
			sourceOCD = retrieveNextCollectionOrMapOCD(sourceOCC);
			sourceOCC.otclCommandDto = sourceOCD;
		}
		String icdCode = null;
		if (!sourceOCC.hasAncestralCollectionOrMap()) {
			icdCode = PcdInitTemplate.generateIfNullSourceRootPcdReturnCode(sourceOCC, logLevel);
		} else {
			icdCode = PcdInitTemplate.generateIfNullSourceParentPcdReturnCode(sourceOCC, logLevel);
		}
		StringBuilder icdCodeBuilder = new StringBuilder(icdCode);
		sourceOCD = retrieveMemberOCD(sourceOCC);
		sourceOCC.otclCommandDto = sourceOCD;
		icdCode = PcdInitTemplate.generateIfNullSourceMemberPcdReturnCode(sourceOCC, idx, logLevel);
		icdCodeBuilder.append(icdCode);
		if (!sourceOCC.hasDescendantCollectionOrMap()) {
			String retrieveMemberCode = RetrieveMemberFromPcdTemplate.generateCode(targetOCC, sourceOCC, createNewVarName,
					varNamesSet, varNamesMap);
			icdCodeBuilder.append(retrieveMemberCode);
		}
		targetOCC.appendCode(icdCodeBuilder);
		return ;
	}
	
	public void appendIfNullSourceReturn(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			Integer idx, LogLevel logLevel) {
		OtclCommandDto sourceOCD = sourceOCC.otclCommandDto;
		if (sourceOCD.isCollectionOrMapMember()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Token should not be of a member for this operation.");
		}
		String ifNullReturnCode = null;
		ifNullReturnCode = GetterIfNullReturnTemplate.generateGetterIfNullReturnCode(targetOCC, sourceOCC, false, logLevel,
				varNamesSet, varNamesMap);
		targetOCC.appendCode(ifNullReturnCode);
		return ;
	}

	public void appendIfNullSourceContinue(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC, 
			LogLevel logLevel) {
		OtclCommandDto sourceOCD = sourceOCC.otclCommandDto;
		if (sourceOCD.isCollectionOrMapMember()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Token should not be of a Collection/Map member for this operation.");
		}
		StringBuilder ifNullReturnCodeBuilder = new StringBuilder();
		String ifNullContinueCode = IfNullContinueTemplate.generateCode(targetOCC, sourceOCC, false, logLevel, 
				varNamesSet, varNamesMap);
		ifNullReturnCodeBuilder.append(ifNullContinueCode);
		if (sourceOCD.isCollectionOrMap()) {
			OtclCommandDto memberOCD = retrieveMemberOCD(sourceOCC);
			sourceOCC.otclCommandDto = memberOCD;
		}
		targetOCC.appendCode(ifNullReturnCodeBuilder);
		return ;
	}
	
	public void appendForLoop(TargetOtclCommandContext targetOCC, String idxPrefix, boolean createNewVarName,
			LogLevel logLevel) {
		appendForLoop(targetOCC, null, idxPrefix, createNewVarName, logLevel);
	}
	
	public void appendForLoop(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC, String idxPrefix,
			boolean createNewVarName, LogLevel logLevel) {
		OtclCommandDto otclCommandDto = null;
		if (sourceOCC != null) {
			otclCommandDto = sourceOCC.otclCommandDto;
		} else {
			otclCommandDto = targetOCC.otclCommandDto;
		}
		if (!otclCommandDto.isCollectionOrMap()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Token should be a collection/map for this operation for target:otclChain : "
					+ otclCommandDto.tokenPath);
		}
		String forLoopCode = null;
		if (TARGET_SOURCE.TARGET == otclCommandDto.enumTargetSource) {
			forLoopCode = ForLoopTemplate.generateTargetLoopCode(targetOCC, idxPrefix, createNewVarName, logLevel,
					varNamesSet, varNamesMap);
		} else {
			forLoopCode = ForLoopTemplate.generateSourceLoopCode(targetOCC, sourceOCC, idxPrefix, createNewVarName,
					logLevel, varNamesSet, varNamesMap);
		} 
		targetOCC.loopCounter++;
		targetOCC.appendCode(forLoopCode);
		return;
	}
	
	public void appendInit(TargetOtclCommandContext targetOCC, boolean createNewVarName, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		if (targetOCD.isCollectionOrMapMember() || (targetOCD.isEnum() && targetOCC.isLeaf())) {
			throw new CodeGeneratorException("", "Invalid call to method  in Script-block : " + targetOCC.scriptId +
					"! Token should not be a Enum / Collection member / Map member for this operation.");
		}
		String ifNullCreateAndSetCode = null;
		StringBuilder ifNullReturnCodeBuilder = new StringBuilder();
		if (targetOCD.isArray()) {
			ifNullCreateAndSetCode = GetterIfNullCreateSetTemplate.generateCodeForArray(targetOCC, targetOCD, 1,
					createNewVarName, varNamesSet, varNamesMap);
		} else {
			ifNullCreateAndSetCode = GetterIfNullCreateSetTemplate.generateCode(targetOCC, targetOCD, createNewVarName,
					varNamesSet, varNamesMap);
		}
		ifNullReturnCodeBuilder.append(ifNullCreateAndSetCode);
		if (targetOCD.isCollectionOrMap()) {
			String icdCode = null;
			if (!targetOCC.hasAncestralCollectionOrMap()) {
				if (OtclConstants.ALGORITHM_ID.MODULE != targetOCC.algorithmId &&
						OtclConstants.ALGORITHM_ID.CONVERTER != targetOCC.algorithmId) {
					icdCode = PcdInitTemplate.generateIfNullTargetRootPcdCreateCode(targetOCC, varNamesSet, varNamesMap);
					ifNullReturnCodeBuilder.append(icdCode);
				}
			} else {
				icdCode = PcdInitTemplate.generateIfNullTargetParentPcdCreateCode(targetOCC, varNamesSet, varNamesMap);
				ifNullReturnCodeBuilder.append(icdCode);
			}
		} 
		targetOCC.appendCode(ifNullReturnCodeBuilder);
		return;
	}

	public void appendInitIfNullTargetReturn(TargetOtclCommandContext targetOCC, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		if (targetOCD.isCollectionOrMapMember()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Token should not be of a member for this operation.");
		}
		StringBuilder ifNullReturnCodeBuilder = new StringBuilder();
		String ifNullReturnCode = GetterIfNullReturnTemplate.generateGetterIfNullReturnCode(targetOCC, false, logLevel, 
				varNamesSet, varNamesMap);
		ifNullReturnCodeBuilder.append(ifNullReturnCode);
		if (targetOCD.isCollectionOrMap()) {
			String icdCode = null;
			if (!targetOCC.hasAncestralCollectionOrMap()) {
				if (OtclConstants.ALGORITHM_ID.MODULE != targetOCC.algorithmId &&
						OtclConstants.ALGORITHM_ID.CONVERTER != targetOCC.algorithmId) { // &&
					icdCode = PcdInitTemplate.generateIfNullTargetRootPcdReturnCode(targetOCC, logLevel);
					ifNullReturnCodeBuilder.append(icdCode);
				}
			} else {
				icdCode = PcdInitTemplate.generateIfNullTargetParentPcdReturnCode(targetOCC, logLevel);
				ifNullReturnCodeBuilder.append(icdCode);
			}
		}
		targetOCC.appendCode(ifNullReturnCodeBuilder);
		return ;
	}
	
	public void appendInitIfNullTargetContinue(TargetOtclCommandContext targetOCC, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		if (targetOCD.isCollectionOrMapMember()) {
			throw new CodeGeneratorException("", "Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Token should not be of a member for this operation.");
		}
		StringBuilder ifNullContinueCodeBuilder = new StringBuilder();
		String ifNullContinueCode = IfNullContinueTemplate.generateCode(targetOCC, null, false, logLevel, 
				varNamesSet, varNamesMap);
		ifNullContinueCodeBuilder.append(ifNullContinueCode);
		targetOCC.appendCode(ifNullContinueCodeBuilder);
		return ;
	}
	
	public void appendInitMember(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, Integer idx,
			boolean createNewVarName, LogLevel logLevel) {
		OtclCommandDto memberOCD = targetOCC.otclCommandDto;
		if (!memberOCD.isCollectionOrMapMember() || (memberOCD.isEnum() && targetOCC.isLeaf())) {
			throw new CodeGeneratorException("","Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Type should be of a member of Collection/Map.");
		}
		String addToCollectionCode = null;
		if (memberOCD.isMapKey()) {
			addToCollectionCode = AddMapKeyTemplate.generateCode(targetOCC, sourceOCD, createNewVarName,
					null, idx, varNamesSet, varNamesMap);
		} else if (memberOCD.isMapValue()) {
			addToCollectionCode = AddMapValueTemplate.generateCode(targetOCC, sourceOCD, createNewVarName,
					null, idx, logLevel, varNamesSet, varNamesMap);
		} else if (memberOCD.isCollectionMember()) {
			addToCollectionCode = AddToCollectionTemplate.generateCode(targetOCC, null, sourceOCD, idx, 
					createNewVarName, varNamesSet, varNamesMap);
		}
		targetOCC.appendCode(addToCollectionCode);
		return;
	}

	public void appendGetSet(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			boolean createNewVarName) {
		String getSetCode = GetSetTemplate.generateCode(targetOCC, sourceOCC, createNewVarName, varNamesSet, 
				varNamesMap);
		targetOCC.appendCode(getSetCode);
		return;
	}

	public void appendAddMapKey(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, String value, Integer idx) {
		String addMapKeysCode = AddMapKeyTemplate.generateCode(targetOCC, sourceOCD, false, value, idx, 
				varNamesSet, varNamesMap);
		targetOCC.appendCode(addMapKeysCode);
		return;
	}
	
	public void appendAddMapValue(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, String value, Integer idx,
			LogLevel logLevel) {
		String addMapValueCode = AddMapValueTemplate.generateCode(targetOCC, sourceOCD, false, value, idx, logLevel,
				varNamesSet, varNamesMap);
		targetOCC.appendCode(addMapValueCode);
		return;
	}
	
	public void appendIfNullTargetPcdReturn(TargetOtclCommandContext targetOCC, LogLevel logLevel) {
		String ifNullChildPcdReturnCode = PcdInitTemplate.generateIfNullTargetParentPcdReturnCode(targetOCC, logLevel);
		targetOCC.appendCode(ifNullChildPcdReturnCode);
	}
	
	public void appendIfNullTargetMemberPcdReturn(TargetOtclCommandContext targetOCC, Integer idx, LogLevel logLevel) {
		String ifNullChildPcdReturnCode = PcdInitTemplate.generateIfNullTargetMemberPcdReturnCode(targetOCC, idx, logLevel);
		targetOCC.appendCode(ifNullChildPcdReturnCode);
	}
	
	public void appendAddToCollection(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, Integer idx,
			String value) {
		String addToCollectionCode = AddToCollectionTemplate.generateCode(targetOCC, value, sourceOCD, idx, false, 
				varNamesSet, varNamesMap);
		targetOCC.appendCode(addToCollectionCode);
	}
	
	public void appendSetter(TargetOtclCommandContext targetOCC, String value) {
		String setterCode = SetterTemplate.generateCode(targetOCC, false, value, varNamesSet, varNamesMap);
		targetOCC.appendCode(setterCode);
	}
	
	public void appendGetter(TargetOtclCommandContext targetOCC, OtclCommandDto otclCommandDto, boolean createNewVarName) {
		String getter = GetterIfNullReturnTemplate.generateCode(targetOCC, otclCommandDto, createNewVarName, varNamesSet, varNamesMap);
		targetOCC.appendCode(getter);
	}
	
	public void appendExecuteModule(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			boolean createNewVarName) {
		String executeModuleCode = ExecuteModuleTemplate.generateCode(targetOCC, sourceOCC, createNewVarName,
				varNamesSet, varNamesMap);
		targetOCC.appendCode(executeModuleCode); 
	}

	public void appendExecuteConverter(TargetOtclCommandContext targetOCC, SourceOtclCommandContext sourceOCC,
			boolean createNewVarName) {
		String executeModuleCode = ExecuteConverterTemplate.generateCode(targetOCC, sourceOCC, createNewVarName,
				varNamesSet, varNamesMap);
		targetOCC.appendCode(executeModuleCode); 
	}

	public void appendInitUptoAnchoredOrLastCollectionOrLeaf(TargetOtclCommandContext targetOCC, Integer idx, 
			boolean uptoLeafParent, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		while (targetOCC.hasChildren()) {
			boolean hasMapValueInPath = targetOCC.hasMapValueMember() || targetOCC.hasMapValueDescendant();
			if (hasMapValueInPath) {
				appendInitIfNullTargetReturn(targetOCC, logLevel);
			} else {
				appendInit(targetOCC, false, LogLevel.WARN);
			}
			if (targetOCD.isCollectionOrMap()) {
				boolean isAnchoredOrHavingCollections = targetOCC.isAnchored() || !targetOCC.hasDescendantCollectionOrMap();
				if (isAnchoredOrHavingCollections && !uptoLeafParent) {
					break;
				}
				targetOCD = OtclCommand.retrieveMemberOCD(targetOCC);
				targetOCC.otclCommandDto = targetOCD;
				if (hasMapValueInPath) { 
					appendIfNullTargetMemberPcdReturn(targetOCC, idx, logLevel);
					String retrieveTargetObjectFromPcdCode = RetrieveMemberFromPcdTemplate.generateCode(targetOCC,
							false, AbstractTemplate.MEMBER_TARGET_ICD, varNamesSet, varNamesMap);
					targetOCC.appendCode(retrieveTargetObjectFromPcdCode);
				} else {
					if (targetOCC.hasDescendantCollectionOrMap()) {
						appendInitMember(targetOCC, null, 0, false, logLevel);
						targetOCD = targetOCC.otclCommandDto;
					}
				}
			}
			if ((targetOCC.isLeafParent() && !targetOCD.isCollectionOrMap()) || !targetOCC.isLeaf()) {
				targetOCD = retrieveNextOCD(targetOCC);
				targetOCC.otclCommandDto = targetOCD;
			}
		}
	}

	public void appendInitUptoNextCollectionWithReturnOrContinue(TargetOtclCommandContext targetOCC, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		while (targetOCC.hasChildren()) {
			boolean hasMapValueInPath = targetOCC.hasMapValueMember() || targetOCC.hasMapValueDescendant();
			if (hasMapValueInPath) { 
				if (targetOCC.hasAncestralCollectionOrMap()) {
					appendInitIfNullTargetContinue(targetOCC, logLevel);
				} else {
					appendInitIfNullTargetReturn(targetOCC, logLevel);
				}
			} else {
				if (targetOCC.hasAncestralCollectionOrMap()) {
					appendGetterIfNullCreateSet(targetOCC);
				} else {
					appendInit(targetOCC, false, LogLevel.WARN);
				}
			}
			if (targetOCD.isCollectionOrMap()) {
				break;
			}
			if ((targetOCC.isLeafParent() && !targetOCD.isCollectionOrMap()) || !targetOCC.isLeaf()) {
				targetOCD = retrieveNextOCD(targetOCC);
				targetOCC.otclCommandDto = targetOCD;
			}
		}
	}
	
	public void appendGetterIfNullCreateSet(TargetOtclCommandContext targetOCC) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		String ifNullCreateAndSetCode = null;
		if (targetOCD.isArray()) {
			ifNullCreateAndSetCode = GetterIfNullCreateSetTemplate.generateCodeForArray(targetOCC, targetOCD, 1,
					false, varNamesSet, varNamesMap);
		} else {
			ifNullCreateAndSetCode = GetterIfNullCreateSetTemplate.generateCode(targetOCC, targetOCD, false,
					varNamesSet, varNamesMap);
		}
		targetOCC.appendCode(ifNullCreateAndSetCode); 
	}

	public void appendInitUptoNextCollectionWithContinue(TargetOtclCommandContext targetOCC, LogLevel logLevel) {
		OtclCommandDto targetOCD = targetOCC.otclCommandDto;
		while (targetOCC.hasChildren()) {
			boolean hasMapValueInPath = targetOCC.hasMapValueMember() || targetOCC.hasMapValueDescendant();
			if (hasMapValueInPath) { 
				appendInitIfNullTargetContinue(targetOCC, logLevel);
				if (targetOCD.isCollectionOrMap()) {
					String icdCode = null;
					if (!targetOCC.hasAncestralCollectionOrMap()) {
						if (OtclConstants.ALGORITHM_ID.MODULE != targetOCC.algorithmId &&
								OtclConstants.ALGORITHM_ID.CONVERTER != targetOCC.algorithmId) {
							icdCode = PcdInitTemplate.generateIfNullTargetRootPcdCreateCode(targetOCC, varNamesSet, varNamesMap);
						}
					} else {
						icdCode = PcdInitTemplate.generateIfNullTargetParentPcdCreateCode(targetOCC, varNamesSet, varNamesMap);
					}
					targetOCC.appendCode(icdCode);
				} 
			} else {
				appendInit(targetOCC, false, LogLevel.WARN);
			}
			if (targetOCD.isCollectionOrMap()) {
				break;
			}
			if ((targetOCC.isLeafParent() && !targetOCD.isCollectionOrMap()) || !targetOCC.isLeaf()) {
				targetOCD = retrieveNextOCD(targetOCC);
				targetOCC.otclCommandDto = targetOCD;
			}
		}
	}

	public void appendInitMember(TargetOtclCommandContext targetOCC, OtclCommandDto sourceOCD, String idxVar, 
			boolean createNewVarName, LogLevel logLevel) {
		OtclCommandDto memberOCD = targetOCC.otclCommandDto;
		if (!memberOCD.isCollectionOrMapMember() || (memberOCD.isEnum() && targetOCC.isLeaf())) {
			throw new CodeGeneratorException("","Invalid call to method in Script-block : " + targetOCC.scriptId + 
					"! Type should be of a member of Collection/Map.");
		}
		String addToCollectionCode = null;
		if (memberOCD.isMapKey()) {
			addToCollectionCode = AddMapKeyTemplate.generateCode(targetOCC, sourceOCD, createNewVarName, idxVar,
					varNamesSet, varNamesMap);
		} else if (memberOCD.isMapValue()) {
			addToCollectionCode = AddMapValueTemplate.generateCode(targetOCC, sourceOCD, createNewVarName,
					idxVar, logLevel, varNamesSet, varNamesMap);
		} else if (memberOCD.isCollectionMember()) {
			addToCollectionCode = AddToCollectionTemplate.generateCode(targetOCC, sourceOCD, idxVar,
					createNewVarName, varNamesSet, varNamesMap);
		}
		targetOCC.appendCode(addToCollectionCode);
		return;
	}
	
	public void appendIncrementOffsetIdx(TargetOtclCommandContext targetOCC) {
		targetOCC.appendCode(AbstractTemplate.incrementOffsetIdx); 
	}
	
	public void appendInitOffsetIdx(TargetOtclCommandContext targetOCC) {
		targetOCC.appendCode(AbstractTemplate.initOffsetIdx); 
	}
}