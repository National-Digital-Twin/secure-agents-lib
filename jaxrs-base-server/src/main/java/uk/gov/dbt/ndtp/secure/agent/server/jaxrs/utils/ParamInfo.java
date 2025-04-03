// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents information about an API parameter used for error handling
 */
public class ParamInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParamInfo.class);

    private final String name, type;

    /**
     * Creates new parameter information
     *
     * @param name Name
     * @param type Type
     */
    public ParamInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the parameter information for a parameter that has a constraint violation against it
     *
     * @param violation Constraint violation
     * @return Parameter Info
     */
    public static ParamInfo fromViolation(ConstraintViolation<?> violation) {
        Path path = violation.getPropertyPath();
        Iterator<Path.Node> iter = path.iterator();
        Class<?> cls = violation.getRootBeanClass();
        Method m = null;

        while (iter.hasNext()) {
            Path.Node node = iter.next();
            cls = updateClassIfBeanNode(node, cls);
            m = updateMethodIfMethodNode(node, cls, m);
            if (node.getKind() == ElementKind.PARAMETER) {
                ParamInfoResult result = handleParameterNode((Path.ParameterNode) node, cls, m);
                if(result.paramInfo != null){
                    return result.paramInfo;
                }
                cls = result.cls;
            }
            ParamInfo paramInfo = getParamInfoIfPropertyNode(node, cls);
            if (paramInfo != null) {
                return paramInfo;
            }
        }
        return new ParamInfo(path.toString(), null);
    }

    private static ParamInfoResult handleParameterNode(Path.ParameterNode param, Class<?> cls, Method m) {
        boolean shouldContinue = false;
        if (m != null) {
            for (Annotation annotation : m.getParameterAnnotations()[param.getParameterIndex()]) {
                ParamInfo info = findParamInfoFromAnnotation(annotation);
                if (info != null) {
                    return new ParamInfoResult(info, cls);
                } else if (annotation instanceof BeanParam) {
                    cls = m.getParameterTypes()[param.getParameterIndex()];
                    shouldContinue = true;
                    break;
                }
            }
        }
        if (shouldContinue) {
            return new ParamInfoResult(null, cls);
        }
        return new ParamInfoResult(new ParamInfo(param.getName(), null), cls);
    }

    private static class ParamInfoResult {
        final ParamInfo paramInfo;
        final Class<?> cls;

        ParamInfoResult(ParamInfo paramInfo, Class<?> cls) {
            this.paramInfo = paramInfo;
            this.cls = cls;
        }
    }

    private static Class<?> updateClassIfBeanNode(Path.Node node, Class<?> cls) {
        if (node.getKind() == ElementKind.BEAN) {
            Path.BeanNode bean = (Path.BeanNode) node;
            return bean.getContainerClass();
        }
        return cls;
    }

    private static Method updateMethodIfMethodNode(Path.Node node, Class<?> cls, Method m) {
        if (node.getKind() == ElementKind.METHOD && cls != null) {
            Path.MethodNode method = (Path.MethodNode) node;
            try {
                return cls.getMethod(method.getName(), method.getParameterTypes().toArray(new Class[0]));
            } catch (NoSuchMethodException e) {
                LOGGER.warn("Constraint violation path identifies method {} which does not exist on class {}",
                        method.getName(), cls.getCanonicalName());
            }
        }
        return m;
    }
    private static ParamInfo getParamInfoIfPropertyNode(Path.Node node, Class<?> cls) {
        if (node.getKind() == ElementKind.PROPERTY){
            Path.PropertyNode property = (Path.PropertyNode) node;
            if(cls != null) {
                try {
                    Field field = cls.getField(property.getName());
                    for (Annotation annotation : field.getAnnotations()) {
                        ParamInfo info = findParamInfoFromAnnotation(annotation);
                        if (info != null) {
                            return info;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // Ignored, can't get a more specific param info
                }
            }
            return new ParamInfo(property.getName(), null);
        }
        return null;
    }
    /**
     * Finds parameter information based on annotations
     *
     * @param annotation Annotation to test for parameter info
     * @return Param info, or {@code null} if not an annotation that provides parameter info
     */
    protected static ParamInfo findParamInfoFromAnnotation(Annotation annotation) {
        if (annotation instanceof QueryParam queryParam) {
            return new ParamInfo(queryParam.value(), "Query");
        } else if (annotation instanceof PathParam pathParam) {
            return new ParamInfo(pathParam.value(), "Path");
        } else if (annotation instanceof HeaderParam headerParam) {
            return new ParamInfo(headerParam.value(), "Header");
        } else if (annotation instanceof CookieParam cookieParam) {
            return new ParamInfo(cookieParam.value(), "Cookie");
        } else if (annotation instanceof FormParam formParam) {
            return new ParamInfo(formParam.value(), "Form");
        } else {
            return null;
        }
    }

    /**
     * Gets the name of the parameter
     *
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type of the parameter
     *
     * @return Type
     */
    public String getType() {
        return type;
    }
}
