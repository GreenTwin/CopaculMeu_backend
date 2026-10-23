package tech.petrepopescu.common.spring.interceptors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import tech.petrepopescu.common.spring.annotation.LogRequestData;
import tech.petrepopescu.common.utils.CookieUtil;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Interceptor that is called for each request. It populates the NDC and other information on the Thread Context
 * and logs useful information:
 * <ul>
 *     <li>Request body (if applicable)</li>
 *     <li>Request parameters (if applicable)</li>
 *     <li>Response return code (if applicable)</li>
 *     <li>Request execution time</li>
 *     <li>Request rendering time</li>
 * </ul>
 */
@Slf4j
@Component
public class RequestInterceptor implements HandlerInterceptor {
    private static final String MONITORING = "MONITORING";
    private static final String NDC_COOKIE_NAME = "ndc";
    public static final String RENDERING_START_TIME = "rendering-start-time";

    // Object that is used as an in-memory cache for endpoints so that lookup is faster on future calls
    private final Map<Method, DataToLog> requestsToLog = new HashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long executionStartTime = System.currentTimeMillis();
        request.setAttribute("start-time", executionStartTime);

        persistNdc(request, response);
        ThreadContext.put("path", request.getServletPath());

        logRequestData(request, handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) {
        long executionStartTime = (Long) request.getAttribute("start-time");
        long renderingStartTime = System.currentTimeMillis();
        request.setAttribute(RENDERING_START_TIME, renderingStartTime);
        long executionDuration = renderingStartTime - executionStartTime;

        DataToLog dataToLog = getDataToLogForEndpoint(handler);
        if (DataToLog.REQUEST_AND_RESPONSE.equals(dataToLog) || DataToLog.RESPONSE.equals(dataToLog)) {
            log.info("Sending response with status " + response.getStatus());
        }

        log.info(MONITORING + " Request execution time was " + executionDuration + " (ms)");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
        if (request.getAttribute(RENDERING_START_TIME) != null) {
            long renderingStartTime = (Long) request.getAttribute(RENDERING_START_TIME);
            long renderingEndTime = System.currentTimeMillis();
            long renderingDuration = renderingEndTime - renderingStartTime;
            log.info(MONITORING + " Request rendering time was " + renderingDuration + " (ms)");
        }

        ThreadContext.pop();
        ThreadContext.clearMap();
    }

    /**
     * Persist the NDC in the Thread Context. Additionally, if the NDC was generated now, store it in a cookie so that
     * future requests will use the same NDC. This is useful for tracking flows across multiple requests done on the same session
     *
     * @param request  The HTTP Servlet Request
     * @param response The HTTP Servlet Response
     */
    private void persistNdc(HttpServletRequest request, HttpServletResponse response) {
        Cookie ndcCookie = CookieUtil.getCookie(request, NDC_COOKIE_NAME);
        String ndc;
        if (ndcCookie == null || StringUtils.isBlank(ndcCookie.getValue())) {
            ndc = generateNdc();
            response.addCookie(CookieUtil.buildSecureCookie(NDC_COOKIE_NAME, ndc));
        } else {
            ndc = ndcCookie.getValue();
        }
        ThreadContext.push(ndc);
        ThreadContext.put("NDC", ndc);
    }

    /**
     * Method that generates a new NDC to be placed on the request. Change this method to use a different format for the NDC
     *
     * @return The generated NDC
     */
    private String generateNdc() {
        return UUID.randomUUID().toString();
    }

    /**
     * This method logs the request received, if the appropriate annotation was provided for the handling controller method
     * For GET logs the URL parameters
     * For POST/PUT it logs the request body
     *
     * @param request The Http Servlet Request
     * @param handler The handler for the request that is mapped to a method in the controller
     */
    private void logRequestData(HttpServletRequest request, Object handler) {
        DataToLog dataToLog = getDataToLogForEndpoint(handler);
        try {
            if (DataToLog.REQUEST_AND_RESPONSE.equals(dataToLog) || DataToLog.REQUEST.equals(dataToLog)) {
                String requestString = getBodyOrParamsAsString(request);
                log.info("Received request: " + requestString);
            }
        } catch (IOException exception) {
            log.error("Error while logging request body", exception);
        }
    }

    /**
     * THis method returns the Request parameters from the URL for GET requests or the body for other request
     *
     * @param request The HTTP Servlet Request
     * @return The body or the request parameters
     * @throws IOException
     */
    private String getBodyOrParamsAsString(HttpServletRequest request) throws IOException {
        if (request.getMethod().equals(HttpMethod.GET.toString())) {
            Map<String, String[]> paramsMap = request.getParameterMap();
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String[]> param : paramsMap.entrySet()) {
                StringJoiner stringJoiner = new StringJoiner(",");
                for (String value : param.getValue()) {
                    stringJoiner.add(value);
                }
                builder.append(param.getKey()).append("=").append(stringJoiner).append(";");
            }

            return builder.toString();
        } else {
            return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Retrieve the information type that needs to be logged for the Handler.
     * This is done by checking if the handler is for a controller method and if that method or class has the
     * {@link LogRequestData} annotation
     *
     * @param handler The handler
     * @return What data information to log
     */
    private DataToLog getDataToLogForEndpoint(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return DataToLog.NONE;
        }

        Method method = ((HandlerMethod) handler).getMethod();
        Class<?> clazz = method.getDeclaringClass();

        // First, let's see if the method has the annotation
        DataToLog whatDataToLog = requestsToLog.get(method);
        LogRequestData logRequestDataAnnotation = null;
        if (whatDataToLog == null) {
            logRequestDataAnnotation = method.getAnnotation(LogRequestData.class);
            whatDataToLog = dataToLogFromAnnotation(logRequestDataAnnotation);
        }

        // If the method does NOT have, let's see if the class has
        if (logRequestDataAnnotation == null) {
            whatDataToLog = requestsToLog.get(clazz);
            if (whatDataToLog == null) {
                // See if the class has the annotation
                Annotation logRequestData = clazz.getAnnotation(LogRequestData.class);
                whatDataToLog = dataToLogFromAnnotation(logRequestData);
            }
        }

        // Store it in our map so that in the future, we do not need to recalculate this.
        requestsToLog.put(method, whatDataToLog);
        return whatDataToLog;
    }

    /**
     * Method that converts the {@link LogRequestData} annotation to an enum which dictates what information to log
     *
     * @param logRequestDataAnnotation The annotation that is present on the controller method
     * @return The enum which dictates what to log
     */
    private DataToLog dataToLogFromAnnotation(Annotation logRequestDataAnnotation) {
        if (!(logRequestDataAnnotation instanceof LogRequestData)) {
            return DataToLog.NONE;
        }

        LogRequestData logRequestData = (LogRequestData) logRequestDataAnnotation;
        boolean logRequest = logRequestData.logRequest();
        boolean logResponse = logRequestData.logResponse();

        if (logRequest && logResponse) {
            return DataToLog.REQUEST_AND_RESPONSE;
        } else if (logRequest) {
            return DataToLog.REQUEST;
        } else {
            return DataToLog.RESPONSE;
        }
    }

    private enum DataToLog {
        REQUEST,
        RESPONSE,
        REQUEST_AND_RESPONSE,
        NONE;
    }
}