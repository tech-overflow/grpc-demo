package io.techoverflow.blocking;

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
public class BlockingClientDriver {
    private static final Logger logger = Logger.getLogger(BlockingClientDriver.class.getName());

    PublicApisGrpc.PublicApisBlockingStub blockingStub;
    ManagedChannel channel;
    public BlockingClientDriver() {
        String target = "localhost:50051";
        channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        blockingStub = PublicApisGrpc.newBlockingStub(channel);
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
    public void getApiList() {
        PublicApisRequest request = PublicApisRequest.newBuilder().build();
        try {
            PublicApisResponse response = blockingStub.getApiList(request);
            logger.log(Level.INFO, String.valueOf(response.getSerializedSize()));
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
        }
    }

    public static void main(String[] args) throws Exception {
        BlockingClientDriver client = new BlockingClientDriver();
        try {

            Options opt = new OptionsBuilder()
                    .include(BlockingClientDriver.class.getSimpleName())
                    .build();
            new Runner(opt).run();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
