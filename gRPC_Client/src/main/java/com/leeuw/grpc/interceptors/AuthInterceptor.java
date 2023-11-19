package com.leeuw.grpc.interceptors;

import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;

@GrpcGlobalClientInterceptor
public class AuthInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor,
            CallOptions callOptions, Channel channel) {
        System.out.println("client interceptor " + methodDescriptor.getFullMethodName());
        return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(Metadata.Key.of("grpc-api-key", Metadata.ASCII_STRING_MARSHALLER), "ay0ub");
                super.start(responseListener, headers);
            }
        };
    }
}