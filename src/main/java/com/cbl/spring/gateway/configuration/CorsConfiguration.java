package com.cbl.spring.gateway.configuration;

import com.cbl.spring.gateway.utils.XssStringValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Map;
import java.util.Optional;

@Configuration
public class CorsConfiguration {

    private static final String ALLOWED_HEADERS = "x-requested-with, authorization, Content-Type, Authorization, credential, X-XSRF-TOKEN";
    private static final String ALLOWED_METHODS = "GET, PUT, POST, DELETE, OPTIONS";
    private static final String ALLOWED_ORIGIN = "*";
    private static final String MAX_AGE = "3600";

    @Bean
    @Order(0)
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();
            if (CorsUtils.isCorsRequest(request)) {
                ServerHttpResponse response = ctx.getResponse();
                HttpHeaders headers = response.getHeaders();
                headers.add("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
                headers.add("Access-Control-Allow-Methods", ALLOWED_METHODS);
                headers.add("Access-Control-Max-Age", MAX_AGE);
                headers.add("Access-Control-Allow-Headers",ALLOWED_HEADERS);
                if (request.getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            return chain.filter(ctx);
        };


    }

    @Bean
    @Order(1)
    public WebFilter xssFilterBody() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            Flux<DataBuffer> dataBufferFlux = request.getBody();
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        try {
                            String bodyString = new String(bytes, "utf-8");
                            XssStringValidator xssStringValidator = new XssStringValidator();
                            if(!xssStringValidator.isGoodHtmlString(bodyString)){
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        DataBufferUtils.release(dataBuffer);
                        Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                            DataBuffer buffer = exchange.getResponse().bufferFactory()
                                    .wrap(bytes);
                            return Mono.just(buffer);
                        });

                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return cachedFlux;
                            }
                        };
                        return chain.filter(exchange.mutate().request(mutatedRequest)
                                .build());
                    });
        };


    }


    @Bean
    @Order(2)
    public WebFilter xssFilterRequestUrlQueryParam() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
            Map<String, String> querymap = queryParams.toSingleValueMap();
            Optional<Map.Entry<String, String>> mapValue = querymap.entrySet().
                    stream().
                    filter(value-> !XssStringValidator.isGoodHtmlString(value.getKey()) ||  !XssStringValidator.isGoodHtmlString(value.getValue())).
                    findFirst();
            if(mapValue.isPresent()){
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    @Order(3)
    public WebFilter xssFilterRequestUrl() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            RequestPath requestPath = exchange.getRequest().getPath();
            String path = requestPath.pathWithinApplication().value();
            if(!XssStringValidator.isGoodHtmlString(path)){
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
            }
            return chain.filter(exchange);
        };
    }
}
