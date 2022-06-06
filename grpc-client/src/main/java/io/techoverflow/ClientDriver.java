package io.techoverflow;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.techoverflow.publicapis.PublicApisGrpc;
import io.techoverflow.publicapis.PublicApisRequest;
import io.techoverflow.publicapis.PublicApisResponse;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@State(Scope.Benchmark)
public class ClientDriver {
    private static final Logger logger = Logger.getLogger(ClientDriver.class.getName());

    PublicApisGrpc.PublicApisBlockingStub blockingStub;
    ManagedChannel channel;
    public ClientDriver() {
        String target = "localhost:50051";
        channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        blockingStub = PublicApisGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
    public void getApiList() {
        PublicApisRequest request = PublicApisRequest.newBuilder().build();
        try {
            PublicApisResponse response = blockingStub.getApiList(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
    }

    public static void main(String[] args) throws Exception {
        ClientDriver client = new ClientDriver();
        try {

            Options opt = new OptionsBuilder()
                    .include(ClientDriver.class.getSimpleName())
                    .forks(2)
                    .build();
            new Runner(opt).run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
