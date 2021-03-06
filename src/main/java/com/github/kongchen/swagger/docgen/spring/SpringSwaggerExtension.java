package com.github.kongchen.swagger.docgen.spring;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.annotations.ApiParam;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.ext.AbstractSwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.FileProperty;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author chekong on 15/4/27.
 */
public class SpringSwaggerExtension extends AbstractSwaggerExtension {

    @Override
    public List<Parameter> extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Iterator<SwaggerExtension> chain) {
        if (this.shouldIgnoreType(type, typesToSkip)) {
            return new ArrayList<Parameter>();
        }

        String defaultValue = "";
        List<Parameter> parameters = new ArrayList<Parameter>();
        Parameter parameter = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof ModelAttribute) {
                parameters.addAll(extractParametersFromModelAttributeAnnotation(annotation, type));
            } else {
                parameter = extractParameterFromAnnotation(annotation, defaultValue, type);
            }

            if (parameter != null) {
                parameters.add(parameter);
            }
        }

        return parameters;
    }

    private Parameter extractParameterFromAnnotation(Annotation annotation, String defaultValue, Type type) {
        Parameter parameter = null;

        if (annotation instanceof RequestParam) {
            RequestParam requestParam = (RequestParam) annotation;
            QueryParameter queryParameter = new QueryParameter().name(requestParam.value())
                    .required(requestParam.required());

            if (!defaultValue.isEmpty()) {
                queryParameter.setDefaultValue(defaultValue);
            }
            Property schema = ModelConverters.getInstance().readAsProperty(type);
            if (schema != null) {
                queryParameter.setProperty(schema);
            }

            parameter = queryParameter;
        } else if (annotation instanceof PathVariable) {
            PathVariable pathVariable = (PathVariable) annotation;
            PathParameter pathParameter = new PathParameter().name(pathVariable.value());
            if (!defaultValue.isEmpty()) {
                pathParameter.setDefaultValue(defaultValue);
            }
            Property schema = ModelConverters.getInstance().readAsProperty(type);
            if (schema != null) {
                pathParameter.setProperty(schema);
            }
            parameter = pathParameter;
        } else if (annotation instanceof RequestHeader) {
            RequestHeader requestHeader = (RequestHeader) annotation;
            HeaderParameter headerParameter = new HeaderParameter().name(requestHeader.value())
                    .required(requestHeader.required());
            headerParameter.setDefaultValue(defaultValue);
            Property schema = ModelConverters.getInstance().readAsProperty(type);
            if (schema != null) {
                headerParameter.setProperty(schema);
            }

            parameter = headerParameter;
        } else if (annotation instanceof CookieValue) {
            CookieValue cookieValue = (CookieValue) annotation;
            CookieParameter cookieParameter = new CookieParameter().name(cookieValue.value())
                    .required(cookieValue.required());
            if (!defaultValue.isEmpty()) {
                cookieParameter.setDefaultValue(defaultValue);
            }
            Property schema = ModelConverters.getInstance().readAsProperty(type);
            if (schema != null) {
                cookieParameter.setProperty(schema);
            }

            parameter = cookieParameter;
        } else if (annotation instanceof RequestPart) {
            RequestPart requestPart = (RequestPart) annotation;
            FormParameter formParameter = new FormParameter().name(requestPart.value())
                    .required(requestPart.required());

            if (!defaultValue.isEmpty()) {
                formParameter.setDefaultValue(defaultValue);
            }

            JavaType ct = constructType(type);
            Property schema;

            if (MultipartFile.class.isAssignableFrom(ct.getRawClass())) {
                schema = new FileProperty();
            } else if (ct.isContainerType() &&
                    MultipartFile.class.isAssignableFrom(ct.getContentType().getRawClass())) {
                schema = new ArrayProperty().items(new FileProperty());
            } else {
                schema = ModelConverters.getInstance().readAsProperty(type);
            }

            if (schema != null) {
                formParameter.setProperty(schema);
            }

            parameter = formParameter;
        } else if (annotation instanceof RequestBody) {

            RefModel model = new RefModel();

            if (type instanceof ParameterizedType) {

                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                Class clazz = (Class) actualTypeArguments[0]; // TODO handle multiple types?
                model.set$ref(clazz.getSimpleName());

            } else {

                Class clazz = (Class) type;
                model.set$ref(clazz.getSimpleName());
            }

            BodyParameter bodyParameter = new BodyParameter();
            bodyParameter.setSchema(model);
            bodyParameter.setRequired(((RequestBody) annotation).required());
            parameter = bodyParameter;
        }

        return parameter;
    }

    private List<Parameter> extractParametersFromModelAttributeAnnotation(Annotation annotation, Type type) {
        if (!(annotation instanceof ModelAttribute)) {
            return null;
        }

        Class<?> cls = TypeUtils.getRawType(type, type);

        List<Parameter> parameters = new ArrayList<Parameter>();
        for (PropertyDescriptor propertyDescriptor : BeanUtils.getPropertyDescriptors(cls)) {
            // Get all the valid setter methods inside the bean
            Method propertyDescriptorSetter = propertyDescriptor.getWriteMethod();
            if (propertyDescriptorSetter != null) {
                ApiParam propertySetterApiParam = AnnotationUtils.findAnnotation(propertyDescriptorSetter, ApiParam.class);
                if (propertySetterApiParam == null) {
                    // If we find a setter that doesn't have @ApiParam annotation, then skip it
                    continue;
                }

                // Here we have a bean setter method that is annotted with @ApiParam, but we still
                // need to know what type of parameter to create. In order to do this, we look for
                // any annotation attached to the first method parameter of the setter fucntion.
                Annotation[][] methodAnnotations = propertyDescriptorSetter.getParameterAnnotations();
                if (methodAnnotations == null || methodAnnotations.length == 0) {
                    continue;
                }

                String defaultValue = "";
                Parameter propertySetterExtractedParameter = null;
                for (Annotation firstMethodParameterAnnotation : methodAnnotations[0]) {
                    Class parameterClass = propertyDescriptor.getPropertyType();
                    propertySetterExtractedParameter = this.extractParameterFromAnnotation(
                            firstMethodParameterAnnotation, defaultValue, parameterClass);
                    if (propertySetterExtractedParameter != null) {
                        // When we find a valid parameter type to use, keep it
                        break;
                    }
                }

                if (propertySetterExtractedParameter == null) {
                    QueryParameter queryParameter = new QueryParameter().name(propertyDescriptor.getDisplayName())
                            .description(propertySetterApiParam.value())
                            .required(propertySetterApiParam.required());
                    queryParameter.setAccess(propertySetterApiParam.access());
                    Property schema = ModelConverters.getInstance()
                            .readAsProperty(propertyDescriptor.getPropertyType());
                    if (schema != null) {
                        queryParameter.setProperty(schema);
                    }
                    if (!propertySetterApiParam.name().isEmpty()) {
                        queryParameter.setName(propertySetterApiParam.name());
                    }
                    parameters.add(queryParameter);
                } else {
                    parameters.add(propertySetterExtractedParameter);
                }
            }
        }

        return parameters;
    }

    @Override
    public boolean shouldIgnoreType(Type type, Set<Type> typesToSkip) {
        Class<?> cls = TypeUtils.getRawType(type, type);
        return cls.getName().startsWith("org.springframework") &&
                !cls.getName().equals("org.springframework.web.multipart.MultipartFile");
    }
}
