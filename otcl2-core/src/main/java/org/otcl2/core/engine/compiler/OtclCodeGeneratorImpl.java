package org.otcl2.core.engine.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.tools.JavaFileObject;

import org.otcl2.common.config.OtclConfig;
import org.otcl2.common.dto.ClassDto;
import org.otcl2.common.dto.OtclChainDto;
import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.dto.OtclDto;
import org.otcl2.common.dto.OtclFileDto;
import org.otcl2.common.dto.ScriptDto;
import org.otcl2.common.exception.OtclException;
import org.otcl2.common.util.CommonUtils;
import org.otcl2.core.engine.compiler.command.ExecutionContext;
import org.otcl2.core.engine.compiler.command.JavaCodeStringObject;
import org.otcl2.core.engine.compiler.command.OtclCommand;
import org.otcl2.core.engine.compiler.command.SourceOtclCommandContext;
import org.otcl2.core.engine.compiler.command.TargetOtclCommandContext;
import org.otcl2.core.engine.compiler.exception.CodeGeneratorException;
import org.otcl2.core.engine.compiler.templates.ClassBeginTemplate;
import org.otcl2.core.engine.compiler.templates.MethodEndTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OtclCodeGeneratorImpl extends AbstractOtclCodeGenerator implements OtclCodeGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(OtclCodeGeneratorImpl.class);
	
	private static OtclCodeGenerator otclCodeGenerator = new OtclCodeGeneratorImpl();
	private static final String otclBinDir = OtclConfig.getOtclTargetLocation();

	private OtclCodeGeneratorImpl() {
		otclCodeGenerator = this;
	}

	public static OtclCodeGenerator getInstance() {
		return otclCodeGenerator;
	}

	@Override
	public List<JavaFileObject> generateSourcecode(OtclDto otclDto) {
		LOGGER.info("Kickstarting Code generator....");
		OtclFileDto otclFileDto = otclDto.otclFileDto;
		ClassDto mainClassDto = otclDto.mainClassDto;
		List<JavaFileObject> javaFileObjects = null;
		try {
			File file = null;
			String clzPackage = otclBinDir.replace("/", File.separator);
			if (otclDto.otclNamespace != null) {
				clzPackage += otclDto.otclNamespace.replace(".", File.separator) + File.separator;
				file = new File(clzPackage);
				file.mkdirs();
			} else {
				file = new File(clzPackage);
			}
    		javaFileObjects = generateSourceCode(otclDto, otclFileDto, mainClassDto);
		} catch (Exception e) {
			if (!(e instanceof OtclException)) {
				throw new CodeGeneratorException(e);
			} else {
				throw (OtclException) e;
			}
		}
		return javaFileObjects;
	}

	private static List<JavaFileObject> generateSourceCode(OtclDto otclDto, OtclFileDto otclFileDto, 
			ClassDto mainClassDto) {
		Map<String, OtclCommandDto> sourceOCDStems = otclDto.sourceOCDStems;
		Map<String, OtclCommandDto> targetOCDStems = otclDto.targetOCDStems;
		Class<?> sourceClz = otclDto.sourceClz;
		Class<?> targetClz = otclDto.targetClz;
		SourceOtclCommandContext sourceOCC = new SourceOtclCommandContext();
		TargetOtclCommandContext targetOCC = new TargetOtclCommandContext();
		targetOCC.mainClassDto = mainClassDto;
		OtclCommand otclCommand = new OtclCommand();
		String otclNamespace = otclDto.otclNamespace;
		String targetType = targetOCC.factoryClassDto.addImport(targetClz.getName());
		String sourceType = null;
		if (sourceClz != null) {
			sourceType = targetOCC.factoryClassDto.addImport(sourceClz.getName());
		}
		String classBeginBody = ClassBeginTemplate.generateMainClassCode(mainClassDto, targetType, sourceType, null,
				new HashSet<>());
		String codeToImport = "\nimport " + targetClz.getName() + ";";
		if (sourceClz != null) {
			codeToImport += "\nimport " + sourceClz.getName() + ";";
		}
		classBeginBody = classBeginBody.replace(OtclCommand.CODE_TO_IMPORT, codeToImport);
		targetOCC.mainClassDto.codeBuilder.append(classBeginBody);
		if (otclFileDto.metadata != null) {
			targetOCC.helper = otclFileDto.metadata.helper;
		}
		List<JavaFileObject> javaFileObjects = null;
		ExecutionContext executionContext = new ExecutionContext();
		for (ScriptDto scriptDto : otclDto.scriptDtos) {
			if (scriptDto.command.debug) { 
				@SuppressWarnings("unused")
				int dummy = 0;
				// -- not guaranteed to be on first iteration - coz this may not be the 
				// --- first scriptDto in the group marked as debug
			}
			targetOCC.factoryClassDto.packageName = otclNamespace;
			resetOCC(targetOCC, scriptDto);
			OtclCommandDto targetOCD = targetOCDStems.get(targetOCC.otclTokens[0]);
			targetOCC.otclCommandDto = targetOCD;
			resetOCC(sourceOCC, scriptDto);
			if (scriptDto.sourceOtclChainDto != null) {
				OtclCommandDto sourceOCD = sourceOCDStems.get(sourceOCC.otclTokens[0]);
				sourceOCC.otclCommandDto = sourceOCD;
			}
			boolean isCopyValues = false;
			boolean isExtensions = false;
			if (javaFileObjects == null) {
				javaFileObjects = new ArrayList<>();
			}
			executionContext.otclCommand = otclCommand;
			executionContext.targetClz = targetClz;
			executionContext.sourceClz = sourceClz;
			executionContext.targetOCC = targetOCC;
			executionContext.sourceOCC = sourceOCC;
//			executionContext.javaFileObjects = javaFileObjects;
			if (scriptDto.hasSetValues) {
				CopyValuesCommandCodeGenerator.generateSourceCode(executionContext);
				isCopyValues = true;
			}
			if (scriptDto.hasExecuteModule || scriptDto.hasExecuteConverter) {
				ExecuteCommandCodeGenerator.generateSourceCode(executionContext);
				isExtensions = true;
			}
			if (!isCopyValues && !isExtensions) {
				OtclChainDto sourceOtclChainDto = scriptDto.sourceOtclChainDto;
				OtclChainDto targetOtclChainDto = scriptDto.targetOtclChainDto;
				int targetCollectionsCount = targetOtclChainDto.collectionCount + targetOtclChainDto.dictionaryCount;
				int sourceCollectionsCount = sourceOtclChainDto.collectionCount + sourceOtclChainDto.dictionaryCount;
				if (targetCollectionsCount > 0 && sourceCollectionsCount > 0) {
					CopyCollectionPathsCodeGenerator.generateSourceCode(executionContext);
				} else {
					CopyFlatAndMixedPathsCodeGenerator.generateSourceCode(executionContext);
				}
			}
			LOGGER.info("Generated code '" + scriptDto.command.factoryClassName + ".java' for Script-Id : " +
					scriptDto.command.id);
		}
		String rootTargetVariable = CommonUtils.initLower(targetClz.getSimpleName());
		String endExecuteMethod = MethodEndTemplate.generateCode(rootTargetVariable);
		targetOCC.mainClassDto.codeBuilder.append(endExecuteMethod).append("\n}");
		String javaCode = targetOCC.mainClassDto.codeBuilder.toString();
		String fqClzName = mainClassDto.className;
		JavaCodeStringObject javaStringObject = new JavaCodeStringObject(fqClzName, javaCode);
		javaFileObjects.add(javaStringObject);
		otclCommand.createJavaFile(mainClassDto);
		return javaFileObjects;
	}

}