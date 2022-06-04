import com.google.gson.Gson;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.techoverflow.publicapis.PublicApisGrpc;
import io.techoverflow.publicapis.PublicApisRequest;
import io.techoverflow.publicapis.PublicApisResponse;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientDriver {
    private static final Logger logger = Logger.getLogger(ClientDriver.class.getName());

    PublicApisGrpc.PublicApisBlockingStub blockingStub;

    public ClientDriver(Channel channel) {
        blockingStub = PublicApisGrpc.newBlockingStub(channel);
    }

    public void getApiList() {
        PublicApisRequest request = PublicApisRequest.newBuilder().build();
        try {
            PublicApisResponse response = blockingStub.getApiList(request);
            logger.log(Level.WARNING, new Gson().toJson(response));
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
    }
    public static void main(String[] args) throws Exception {
        String target = "localhost:50051";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();

        try {
            ClientDriver client = new ClientDriver(channel);
            client.getApiList();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
