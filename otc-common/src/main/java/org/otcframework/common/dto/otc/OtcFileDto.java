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
package org.otcframework.common.dto.otc;

import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Class OtcFileDto.
 */
public final class OtcFileDto {

	/** The file name. */
	public String fileName;

	/** The metadata. */
	public Metadata metadata;

	/** The otc commands. */
	public List<OtcCommands> otcCommands;

	/**
	 * The Class Metadata.
	 */
	public static final class Metadata {

		/** The entry class name. */
		public String entryClassName;

		/** The helper. */
		public String helper;

		/** The object types. */
		public ObjectTypes objectTypes;

		/**
		 * The Class ObjectTypes.
		 */
		public static final class ObjectTypes {

			/** The source. */
			public String source;

			/** The target. */
			public String target;
		}
	}

	/**
	 * The Class OtcCommands.
	 */
	public static final class OtcCommands {

		/** The copy. */
		public Copy copy;

		/** The execute. */
		public Execute execute;
	}

	/**
	 * The Class CommonCommandParams.
	 */
	public static class CommonCommandParams {

		/** The id. */
		public String id;

		/** The factory class name. */
		public String factoryClassName;

		/** The disable. */
		public boolean disable;

		/** The debug. */
		public boolean debug;
	}

	/**
	 * The Class Copy.
	 */
	public static final class Copy extends CommonCommandParams {

		/** The from. */
		public Source from;

		/** The to. */
		public TargetDto to;

		/**
		 * To string.
		 *
		 * @return the string
		 */
		public String toString() {
			return "Copy [from=" + from + ", to=" + to + ", id=" + id + ", factoryClassName=" + factoryClassName
					+ ", disable=" + disable + ", debug=" + debug + "]";
		}

		/**
		 * The Class Source.
		 */
		public static final class Source extends SourceDto {

			/** The values. */
			public List<String> values;

			/**
			 * To string.
			 *
			 * @return the string
			 */
			public String toString() {
				return "Source [values=" + values + ", otcChain=" + objectPath + ", overrides=" + overrides + "]";
			}
		}
	}

	/**
	 * The Class Execute.
	 */
	public static final class Execute extends CommonCommandParams {

		/** The otc converter. */
		public String otcConverter;

		/** The otc module. */
		public OtcModule otcModule;

		/** The execution order. */
		public List<String> executionOrder;

		/** The source. */
		public SourceDto source;

		/** The target. */
		public TargetDto target;

		/**
		 * The Class OtcModule.
		 */
		public static final class OtcModule {

			/** The otc namespace. */
			public String otcNamespace;
		}
	}
}