package com.srviswan.basketpricing.config;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new io.grpc.ServerInterceptor() {
            @Override
            public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                    io.grpc.ServerCall<ReqT, RespT> call,
                    io.grpc.Metadata headers,
                    io.grpc.ServerCallHandler<ReqT, RespT> next) {
                
                String methodName = call.getMethodDescriptor().getFullMethodName();
                long startTime = System.currentTimeMillis();
                
                return next.startCall(new io.grpc.ServerCall<ReqT, RespT>() {
                    @Override
                    public void request(int numMessages) {
                        call.request(numMessages);
                    }

                    @Override
                    public void sendHeaders(io.grpc.Metadata headers) {
                        call.sendHeaders(headers);
                    }

                    @Override
                    public void sendMessage(RespT message) {
                        call.sendMessage(message);
                    }

                    @Override
                    public void close(io.grpc.Status status, io.grpc.Metadata trailers) {
                        long duration = System.currentTimeMillis() - startTime;
                        System.out.println("gRPC call " + methodName + " completed in " + duration + "ms with status: " + status.getCode());
                        call.close(status, trailers);
                    }

                    @Override
                    public boolean isCancelled() {
                        return call.isCancelled();
                    }

                    @Override
                    public io.grpc.MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
                        return call.getMethodDescriptor();
                    }
                }, headers);
            }
        };
    }
}
