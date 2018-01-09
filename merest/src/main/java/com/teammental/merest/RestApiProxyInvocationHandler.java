package com.teammental.merest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.teammental.mecore.stereotype.controller.RestApi;
import com.teammental.mehelper.StringHelper;
import com.teammental.merest.exception.NoRequestMappingFoundException;
import com.teammental.mevalidation.dto.ValidationResultDto;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

class RestApiProxyInvocationHandler implements InvocationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestApiProxyInvocationHandler.class);

  RestTemplate restTemplate = new RestTemplate();

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    RestExchangeProperties properties = prepareRestExchangeProperties(proxy, method, args);

    RestResponse<Object> restResponse;
    try {

      restResponse = doRestExchange(properties);

    } catch (HttpStatusCodeException exception) {

      restResponse = handleHttpStatusCodeException(exception);
    }

    return restResponse;
  }

  RestExchangeProperties prepareRestExchangeProperties(Object proxy, Method method, Object[] args) {
    RestApi restApiAnnotation = AnnotationUtils.findAnnotation(proxy.getClass(), RestApi.class);
    String applicationName = restApiAnnotation.value();

    String url = ApplicationExplorer.getApplicationUrl(applicationName);

    Mapping classLevelMapping = extractMapping(method.getDeclaringClass());
    String classLevelUrl = classLevelMapping.getUrl();

    Mapping methodLevelMapping = extractMapping(method);
    String methodLevelUrl = methodLevelMapping.getUrl();

    url = url + classLevelUrl + methodLevelUrl;

    HttpMethod httpMethod = methodLevelMapping.getHttpMethod();

    Map<String, Object> urlVariables = new HashMap<>();
    Object requestBody = null;
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      Object value = args[i];

      if (parameter.isAnnotationPresent(PathVariable.class)) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        String name = pathVariable.value();
        urlVariables.put(name, value);
      } else if (parameter.isAnnotationPresent(RequestParam.class)) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String name = requestParam.value();
        urlVariables.put(name, value);
      } else if (parameter.isAnnotationPresent(RequestBody.class)) {
        requestBody = value;
      }
    }

    HttpEntity httpEntity = requestBody == null ? null : new HttpEntity<>(requestBody);

    Class<?> returnType = extractReturnType(method.getGenericReturnType());

    RestExchangeProperties properties = new RestExchangeProperties(url,
        httpMethod, httpEntity, urlVariables, returnType);

    return properties;
  }

  RestResponse<Object> handleHttpStatusCodeException(HttpStatusCodeException exception) {
    HttpStatus status = exception.getStatusCode();

    RestResponse<Object> restResponse = new RestResponse<>(status);

    ObjectMapper objectMapper = new ObjectMapper();

    ValidationResultDto validationResultDto;

    try {

      validationResultDto = objectMapper
          .readValue(exception.getResponseBodyAsString(), ValidationResultDto.class);

    } catch (Exception ex) {
      LOGGER.error(ex.getLocalizedMessage());

      validationResultDto = null;
    }


    if (validationResultDto == null) {
      restResponse.setResponseMessage(exception.getResponseBodyAsString());
    } else {
      restResponse.setValidationResult(validationResultDto);
    }

    return restResponse;
  }

  RestResponse<Object> doRestExchange(RestExchangeProperties properties)
      throws IOException, HttpStatusCodeException {

    ResponseEntity<String> responseEntity = restTemplate.exchange(properties.getUrl(),
        properties.getHttpMethod(), properties.getHttpEntity(),
        new ParameterizedTypeReference<String>() {
        }, properties.getUrlVariables());


    ObjectMapper objectMapper = new ObjectMapper();

    Object returnBody = null;
    if (!StringHelper.isNullOrEmpty(responseEntity.getBody())) {
      if (responseEntity.getBody().startsWith("[")) {
        returnBody = objectMapper.readValue(responseEntity.getBody(),
            objectMapper.getTypeFactory().constructCollectionType(List.class,
                properties.getReturnType()));
      } else {
        returnBody = objectMapper.readValue(responseEntity.getBody(), properties.getReturnType());
      }
    }
    RestResponse<Object> restResponse = new RestResponse<>(returnBody,
        responseEntity.getHeaders(),
        responseEntity.getStatusCode());

    return restResponse;
  }

  Class<?> extractReturnType(Type genericReturnType) {
    Class<?> returnType;
    if (genericReturnType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
      Type[] types = parameterizedType.getActualTypeArguments();
      if (types.length == 1) {
        Type type = types[0];
        returnType = extractReturnType(type);
      } else {
        returnType = Object.class;
      }
    } else {
      returnType = (Class<?>) genericReturnType;
    }

    return returnType;
  }

  /**
   * Searches {@link org.springframework.web.bind.annotation.RequestMapping RequestMapping}
   * annotation on the given method argument and extracts
   * If RequestMapping annotation is not found, NoRequestMappingFoundException is thrown.
   * {@link org.springframework.http.HttpMethod HttpMethod} type equivalent to
   * {@link org.springframework.web.bind.annotation.RequestMethod RequestMethod} type
   *
   * @param element AnnotatedElement object to be examined.
   * @return Mapping object
   */
  Mapping extractMapping(AnnotatedElement element) {
    Annotation annotation = findMappingAnnotation(element);
    String[] urls;
    RequestMethod requestMethod;

    if (annotation instanceof RequestMapping) {
      RequestMapping requestMapping = (RequestMapping) annotation;
      requestMethod = requestMapping.method().length == 0
          ? RequestMethod.GET : requestMapping.method()[0];
      urls = requestMapping.value();

    } else if (annotation instanceof GetMapping) {

      requestMethod = RequestMethod.GET;
      urls = ((GetMapping) annotation).value();

    } else if (annotation instanceof PostMapping) {

      requestMethod = RequestMethod.POST;
      urls = ((PostMapping) annotation).value();

    } else if (annotation instanceof PutMapping) {

      requestMethod = RequestMethod.PUT;
      urls = ((PutMapping) annotation).value();

    } else if (annotation instanceof DeleteMapping) {

      requestMethod = RequestMethod.DELETE;
      urls = ((DeleteMapping) annotation).value();

    } else if (annotation instanceof PatchMapping) {

      requestMethod = RequestMethod.PATCH;
      urls = ((PatchMapping) annotation).value();

    } else {
      throw new NoRequestMappingFoundException(element);
    }

    HttpMethod httpMethod = HttpMethod.resolve(requestMethod.name());
    String url = StringHelper.getFirstOrEmpty(urls);

    return new Mapping(httpMethod, url);

  }

  Annotation findMappingAnnotation(AnnotatedElement element) {
    Annotation mappingAnnotation = element.getAnnotation(RequestMapping.class);

    if (mappingAnnotation == null) {
      mappingAnnotation = element.getAnnotation(GetMapping.class);

      if (mappingAnnotation == null) {
        mappingAnnotation = element.getAnnotation(PostMapping.class);

        if (mappingAnnotation == null) {
          mappingAnnotation = element.getAnnotation(PutMapping.class);

          if (mappingAnnotation == null) {
            mappingAnnotation = element.getAnnotation(DeleteMapping.class);

            if (mappingAnnotation == null) {
              mappingAnnotation = element.getAnnotation(PatchMapping.class);
            }
          }
        }
      }
    }

    if (mappingAnnotation == null) {
      if (element instanceof Method) {
        Method method = (Method) element;
        mappingAnnotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
      } else {
        Class<?> clazz = (Class<?>) element;
        mappingAnnotation = AnnotationUtils.findAnnotation(clazz, RequestMapping.class);
      }
    }

    return mappingAnnotation;
  }

  class RestExchangeProperties {
    private String url;
    private HttpMethod httpMethod;
    private HttpEntity httpEntity;
    private Map<String, Object> urlVariables;
    private Class<?> returnType;

    public RestExchangeProperties(String url,
                                  HttpMethod httpMethod,
                                  HttpEntity httpEntity,
                                  Map<String, Object> urlVariables,
                                  Class<?> returnType) {
      this.url = url;
      this.httpMethod = httpMethod;
      this.httpEntity = httpEntity;
      this.urlVariables = urlVariables;
      this.returnType = returnType;
    }

    public String getUrl() {
      return url;
    }

    public HttpMethod getHttpMethod() {
      return httpMethod;
    }


    public HttpEntity getHttpEntity() {
      return httpEntity;
    }

    public Map<String, Object> getUrlVariables() {
      return urlVariables;
    }


    public Class<?> getReturnType() {
      return returnType;
    }

  }

  class Mapping {
    private HttpMethod httpMethod;
    private String url;

    public Mapping(HttpMethod httpMethod, String url) {
      this.httpMethod = httpMethod;
      this.url = url;
    }

    public HttpMethod getHttpMethod() {
      return httpMethod;
    }

    public String getUrl() {
      return url;
    }
  }
}