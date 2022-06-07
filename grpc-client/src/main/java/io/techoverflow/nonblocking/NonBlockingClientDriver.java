package io.techoverflow.nonblocking;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import io.techoverflow.publicapis.PublicApisGrpc;
import io.techoverflow.publicapis.PublicApisRequest;
import io.techoverflow.publicapis.PublicApisResponse;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@State(Scope.Benchmark)
public class NonBlockingClientDriver {
    private static final Logger logger = Logger.getLogger(NonBlockingClientDriver.class.getName());
    final Object lock = new Object();
    PublicApisGrpc.PublicApisStub nonBlockingStub;
    ManagedChannel channel;

    public NonBlockingClientDriver() {
        String target = "localhost:50051";
        channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        nonBlockingStub = PublicApisGrpc.newStub(channel);
    }

    public void shutdown() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Benchmark
    @Warmup(iterations=0)
    @Fork(value = 1)
    @Measurement(iterations=10)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    public void getApiList() throws InterruptedException {
        PublicApisRequest request = PublicApisRequest.newBuilder().build();

        try {
            PublicApisResponse response = nonBlockingStub.getApiList(request,
                    new ClientResponseObserver<PublicApisRequest, PublicApisResponse>() {
                    @Override
                    public void onNext(PublicApisResponse publicApisResponse) {
                        logger.log(Level.INFO, String.valueOf(publicApisResponse.getSerializedSize()));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }

                    @Override
                    public void beforeStart(ClientCallStreamObserver<PublicApisRequest> requestStream) {
                        requestStream.setOnReadyHandler(new Runnable() {
                            public void run() {
                                requestStream.onNext(request);
                                requestStream.onCompleted();
                            }
                        });
                    }
            });
            synchronized (lock) {
                lock.wait();
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }

    public static void main(String[] args) throws Exception {
        NonBlockingClientDriver client = new NonBlockingClientDriver();
        try {
            Options opt = new OptionsBuilder()
                    .include(NonBlockingClientDriver.class.getSimpleName())
                    .build();
            new Runner(opt).run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
