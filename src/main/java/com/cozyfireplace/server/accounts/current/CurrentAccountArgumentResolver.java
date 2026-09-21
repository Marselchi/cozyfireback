package com.cozyfireplace.server.accounts.current;

import com.cozyfireplace.server.accounts.Account;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CurrentAccountArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentAccountResolver accountResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentAccount.class) != null
                && Account.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    @Nullable
    public Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {
        HttpServletRequest request = getNativeRequest(webRequest);
        Long roomId = extractRoomIdFromPath(request);

        return accountResolver.resolve(roomId);
    }

    @NonNull
    private HttpServletRequest getNativeRequest(@NonNull NativeWebRequest webRequest) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("Current request is not an HTTP request");
        }
        return request;
    }

    @NonNull
    private Long extractRoomIdFromPath(@NonNull HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!(attribute instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("URI template variables attribute is not a Map");
        }

        if (rawMap.isEmpty() || !rawMap.containsKey("roomId")) {
            throw new IllegalArgumentException("Missing path variable 'roomId' in request URL");
        }

        Object roomIdObj = rawMap.get("roomId");
        if (!(roomIdObj instanceof String roomIdParam)) {
            throw new IllegalArgumentException("roomId parameter is not a String");
        }

        try {
            return Long.parseLong(roomIdParam);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid roomId format: " + roomIdParam, ex);
        }
    }
}
