package io.techoverflow;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.techoverflow.publicapis.PublicApisGrpc;
import io.techoverflow.publicapis.PublicApisRequest;
import io.techoverflow.publicapis.PublicApisResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerDriver {
    private static final Logger logger = Logger.getLogger(ServerDriver.class.getName());
    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new PublicApisGrpcImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    ServerDriver.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static class PublicApisGrpcImpl extends PublicApisGrpc.PublicApisImplBase {

        @Override
        public void getApiList(PublicApisRequest req, StreamObserver<PublicApisResponse> responseObserver) {
            PublicApisBlockingCaller publicApisBlockingCaller = new PublicApisBlockingCaller();
            try {
                responseObserver.onNext(publicApisBlockingCaller.getPublicApis().build());
                logger.log(Level.ALL, "Emitting response");
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
                responseObserver.onNext(PublicApisResponse.newBuilder().build());
            }
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerDriver server = new ServerDriver();
        server.start();
        server.blockUntilShutdown();
    }
}
