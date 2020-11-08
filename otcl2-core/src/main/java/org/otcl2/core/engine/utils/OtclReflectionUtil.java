/**
* Copyright (c) otcl2.org
*
* @author  Franklin Abel
* @version 1.0
* @since   2020-06-08 
*/
package org.otcl2.core.engine.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.otcl2.common.dto.OtclCommandDto;
import org.otcl2.common.exception.OtclException;
import org.otcl2.common.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtclReflectionUtil {

	private static Logger LOGGER = LoggerFactory.getLogger(OtclReflectionUtil.class);

	public static enum GETTER_SETTER {
		GETTER, SETTER
	};

	private static final Field[] ZERO_LENGTH_FIELD_ARRAY = new Field[0];
	private static final Class[] ZERO_LENGTH_CLASS_ARRAY = new Class[0];
	private static final Map<Class<?>, Field[]> fieldsCache = new ConcurrentHashMap<>(100);

	public static Object readFieldValue(Field field, Object value) {
		if (value == null) {
			throw new OtclException("", "Property value cannot be null!.");
		}
		try {
			field.setAccessible(true);
			value = field.get(value);
		} catch (IllegalAccessException ex) {
			LOGGER.warn(ex.getMessage());
		} finally {
			field.setAccessible(false);
		}
		return value;
	}

	public static String findGetterName(OtclCommandDto otclCommandDto) {
		Field field = otclCommandDto.field;
		String fieldName = field.getName();
		String getter = otclCommandDto.getter;
		if (getter == null) {
			getter = "get" + CommonUtils.initCap(fieldName);
		}
		Method method = findMethod(GETTER_SETTER.GETTER, getter, otclCommandDto);
		String methodName = method.getName();
		return methodName;
	}

	public static String findSetterName(OtclCommandDto otclCommandDto) {
		Field field = otclCommandDto.field;
		String fieldName = field.getName();
		String setter = otclCommandDto.setter;
		if (setter == null) {
			setter = "set" + CommonUtils.initCap(fieldName);
		}
		Method method = findMethod(GETTER_SETTER.SETTER, setter, otclCommandDto);
		String methodName = method.getName();
		return methodName;
	}

	private static Method findMethod(GETTER_SETTER enumGetterSetter, String methodName, OtclCommandDto otclCommandDto) {
		Method method = null;
		Field field = otclCommandDto.field;
		Class<?> declaringClz = field.getDeclaringClass();
		Exception ex = null;
		try {
			if (GETTER_SETTER.GETTER == enumGetterSetter) {
				method = declaringClz.getMethod(methodName, ZERO_LENGTH_CLASS_ARRAY);
			} else {
				method = declaringClz.getMethod(methodName, new Class[] { field.getType() });
			}
			return method;
		} catch (NoSuchMethodException | SecurityException e) {
			ex = e;
		}
		Class<?> parentConcreteType = null;
		if (!otclCommandDto.isRootNode) {
			parentConcreteType = otclCommandDto.parent.concreteType;
		}
		if (GETTER_SETTER.GETTER == enumGetterSetter) {
			if (parentConcreteType != null) {
				try {
					method = parentConcreteType.getMethod(methodName, ZERO_LENGTH_CLASS_ARRAY);
					return method;
				} catch (NoSuchMethodException | SecurityException e) {
					ex = e;
				}
			}
		} else {
			Class<?> concreteType = otclCommandDto.concreteType;
			if (concreteType != null) {
				try {
					method = declaringClz.getMethod(methodName, new Class[] { concreteType });
					return method;
				} catch (NoSuchMethodException | SecurityException e) {
					ex = e;
				}
				try {
					method = parentConcreteType.getMethod(methodName, new Class[] { concreteType });
					return method;
				} catch (NoSuchMethodException | SecurityException e) {
					ex = e;
				}
			}
		}
		if (method == null && ex != null) {
			throw new OtclException("", ex);
		}
		return method;
	}

	public static String findHelperMethodName(Class<?> factoryHelper, GETTER_SETTER enumGetterSetter,
			String methodName, OtclCommandDto otclCommandDto) {
		Method method = findFactoryHelperMethod(factoryHelper, enumGetterSetter, methodName, otclCommandDto);
		if (method != null) {
			methodName = method.getName();
		}
		return methodName;
	}
	
	public static Method findFactoryHelperMethod(Class<?> factoryHelper, GETTER_SETTER enumGetterSetter, String methodName,
			OtclCommandDto otclCommandDto) {
		if (factoryHelper == null) {
			throw new OtclException("", "Helper class cannot be null to invoke this method!");
		}
		Method method = null;
		Field field = otclCommandDto.field;
		Class<?> declaringClz = field.getDeclaringClass();
		Exception ex = null;
		try {
			if (GETTER_SETTER.SETTER == enumGetterSetter) {
				Class<?> fieldType = otclCommandDto.fieldType;
				method = findMethod(factoryHelper, enumGetterSetter, methodName, otclCommandDto, declaringClz, fieldType);
				otclCommandDto.enableFactoryHelperSetter = true;
			} else {
				method = findMethod(factoryHelper, enumGetterSetter, methodName, otclCommandDto, declaringClz);
				otclCommandDto.enableFactoryHelperGetter = true;
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new OtclException("", ex);
		}
		return method;
	}

	private static Method findMethod(Class<?> clz, GETTER_SETTER enumGetterSetter, String methodName,
			OtclCommandDto otclCommandDto, Class<?>... paramTypes) throws NoSuchMethodException, SecurityException {
		Method method = null;
		if (GETTER_SETTER.SETTER == enumGetterSetter) {
			method = clz.getMethod(methodName, paramTypes);
		} else {
			method = clz.getMethod(methodName, paramTypes);
		}
		return method;
	}

	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	public static Field findField(Class<?> clazz, String name, Class<?> type) {
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) && (type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	private static Field[] getDeclaredFields(Class<?> clazz) {
		Field[] result = fieldsCache.get(clazz);
		if (result == null) {
			try {
				result = clazz.getDeclaredFields();
				fieldsCache.put(clazz, (result.length == 0 ? ZERO_LENGTH_FIELD_ARRAY : result));
			} catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName()
						+ "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return result;
	}
}