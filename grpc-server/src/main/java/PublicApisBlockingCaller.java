import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import io.techoverflow.publicapis.PublicApisResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PublicApisBlockingCaller {

    public PublicApisResponse.Builder getPublicApis() throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://api.publicapis.org/entries");
        HttpResponse response = client.execute(httpGet);
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        StringBuilder output = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            output.append(line);
        }
        PublicApisResponse.Builder publicApisResponse = PublicApisResponse.newBuilder();
        JsonFormat.parser().merge(output.toString(), publicApisResponse);
        return publicApisResponse;
    }
}
