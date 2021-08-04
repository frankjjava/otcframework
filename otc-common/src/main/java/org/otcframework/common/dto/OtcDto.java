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
package org.otcframework.common.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.otcframework.common.dto.otc.OtcFileDto;

/**
 * The Class OtcDto.
 */
// TODO: Auto-generated Javadoc
public final class OtcDto {

	/** The otc file dto. */
	public OtcFileDto otcFileDto;

	/** The otc namespace. */
	public String otcNamespace;

	/** The otc file name. */
	public String otcFileName;

	/** The source clz. */
	public Class<?> sourceClz;

	/** The target clz. */
	public Class<?> targetClz;

	/** The main class dto. */
	public ClassDto mainClassDto;

	/** The script dtos. */
	public List<ScriptDto> scriptDtos;

	/** The source OCD stems. */
	public Map<String, OtcCommandDto> sourceOCDStems;

	/** The target OCD stems. */
	public Map<String, OtcCommandDto> targetOCDStems;

	/**
	 * Instantiates a new otc dto.
	 *
	 * @param builder the builder
	 */
	private OtcDto(Builder builder) {
		otcNamespace = builder.otcNamespace;
		otcFileName = builder.otcFileName;
		sourceClz = builder.sourceClz;
		targetClz = builder.targetClz;
//		groupedScriptDtos = builder.groupedScriptDtos;
		targetOCDStems = builder.targetOCDStems;
		sourceOCDStems = builder.sourceOCDStems;
		scriptDtos = builder.scriptDtos;
	}

	/**
	 * New builder.
	 *
	 * @return the builder
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * The Class Builder.
	 */
	public static class Builder {

		/** The otc namespace. */
		private String otcNamespace;

		/** The otc file name. */
		private String otcFileName;

		/** The deployment id. */
		public String deploymentId;

		/** The source clz. */
		private Class<?> sourceClz;

		/** The target clz. */
		private Class<?> targetClz;

		/** The script dtos. */
		public List<ScriptDto> scriptDtos;

		/** The source OCD stems. */
		private Map<String, OtcCommandDto> sourceOCDStems;

		/** The target OCD stems. */
		private Map<String, OtcCommandDto> targetOCDStems;

		/**
		 * Adds the otc namespace.
		 *
		 * @param otcNamespace the otc namespace
		 * @return the builder
		 */
		public Builder addOtcNamespace(String otcNamespace) {
			this.otcNamespace = otcNamespace;
			return this;
		}

		/**
		 * Adds the otc file name.
		 *
		 * @param otcFileName the otc file name
		 * @return the builder
		 */
		public Builder addOtcFileName(String otcFileName) {
			this.otcFileName = otcFileName;
			return this;
		}

		/**
		 * Adds the deployment id.
		 *
		 * @param deploymentId the deployment id
		 * @return the builder
		 */
		public Builder addDeploymentId(String deploymentId) {
			this.deploymentId = deploymentId;
			return this;
		}

		/**
		 * Adds the source clz.
		 *
		 * @param sourceClz the source clz
		 * @return the builder
		 */
		public Builder addSourceClz(Class<?> sourceClz) {
			this.sourceClz = sourceClz;
			return this;
		}

		/**
		 * Adds the target clz.
		 *
		 * @param targetClz the target clz
		 * @return the builder
		 */
		public Builder addTargetClz(Class<?> targetClz) {
			this.targetClz = targetClz;
			return this;
		}

		/**
		 * Adds the script dto.
		 *
		 * @param scriptDto the script dto
		 * @return the builder
		 */
		public Builder addScriptDto(ScriptDto scriptDto) {
			if (scriptDtos == null) {
				scriptDtos = new ArrayList<>();
			}
			scriptDtos.add(scriptDto);
			return this;
		}

		/**
		 * Adds the source otc command dto stem.
		 *
		 * @param sourceOCD the source OCD
		 * @return the builder
		 */
		public Builder addSourceOtcCommandDtoStem(OtcCommandDto sourceOCD) {
			if (sourceOCDStems == null) {
				sourceOCDStems = new HashMap<>();
			}
			if (!sourceOCDStems.containsKey(sourceOCD.otcToken)) {
				sourceOCDStems.put(sourceOCD.otcToken, sourceOCD);
			}
			return this;
		}

		/**
		 * Adds the target otc command dto stem.
		 *
		 * @param targetOCD the target OCD
		 * @return the builder
		 */
		public Builder addTargetOtcCommandDtoStem(OtcCommandDto targetOCD) {
			if (targetOCDStems == null) {
				targetOCDStems = new HashMap<>();
			}
			if (!targetOCDStems.containsKey(targetOCD.otcToken)) {
				targetOCDStems.put(targetOCD.otcToken, targetOCD);
			}
			return this;
		}

		/**
		 * Builds the.
		 *
		 * @return the otc dto
		 */
		public OtcDto build() {
			return new OtcDto(this);
		}
	}
}