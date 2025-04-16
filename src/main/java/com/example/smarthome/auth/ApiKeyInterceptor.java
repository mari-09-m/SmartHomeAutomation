package com.example.smarthome.auth;

import io.grpc.*;

public class ApiKeyInterceptor implements ServerInterceptor {

    private static final String VALID_API_KEY = "my-secure-key-123";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        Metadata.Key<String> apiKeyHeader =
                Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

        String clientApiKey = headers.get(apiKeyHeader);

        if (!VALID_API_KEY.equals(clientApiKey)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid API Key"), headers);
            return new ServerCall.Listener<>() {}; 
        }

        return next.startCall(call, headers); 
    }
}
