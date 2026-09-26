package com.backend.orbitflow.global.advice;

import com.backend.orbitflow.global.dto.response.CommonResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Instant;
import java.util.Optional;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalResponseAdvice implements ResponseBodyAdvice<CommonResponse<?>> {

    private final Tracer tracer;

    @Value("${app.version:v1.0}") // 기본값(default value) : v1.0
    private String version;

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }

        ResolvableType resolvableType = ResolvableType.forMethodParameter(returnType);

        return CommonResponse.class.isAssignableFrom(returnType.getParameterType()) ||
                CommonResponse.class.isAssignableFrom(resolvableType.getGeneric(0).toClass());
    }

    @Override
    public CommonResponse<?> beforeBodyWrite(
            CommonResponse<?> body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response
    ) {
        if (body == null) {
            return null;
        }

        String traceId = Optional.ofNullable(tracer.currentSpan())
                .map(Span::context)
                .map(TraceContext::traceId)
                .orElse(null);

        CommonResponse.Meta meta = new CommonResponse.Meta(
                Instant.now(),
                traceId,
                version
        );

        return new CommonResponse<>(
                body.success(),
                body.status(),
                body.data(),
                body.error(),
                body.message(),
                meta
        );
    }
}
