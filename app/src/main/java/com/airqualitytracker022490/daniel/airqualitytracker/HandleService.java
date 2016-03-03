package com.airqualitytracker022490.daniel.airqualitytracker;

/**
 * Created by Daniel on 3/3/2016.
 */
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import java.net.URI;



/**
 * Created by Daniel on 2/7/2016.
 */
public class HandleService {
    HttpResponse response;
    String responseString;
    URI uri;
    public String getAQ(String location, Boolean usingGps, Double latitude, Double longitude) {
        try{
            if(usingGps){
                uri = new URI("http://api.breezometer.com/baqi/?lat="+latitude+"&lon="+longitude+"&key=156744bdb0b44019b4e4a4d3e022bcca");
            }else{
                uri = new URI("http://api.breezometer.com/baqi/?location="+location+"&key=156744bdb0b44019b4e4a4d3e022bcca");
            }
            HttpGet request = new HttpGet(uri);
            HttpClient client = new DefaultHttpClient();
            response = client.execute(request);
            HttpEntity httpEntity = response.getEntity();
            responseString = EntityUtils.toString(httpEntity);
        }catch (Exception e){
            e.printStackTrace();
        }

        return responseString;

    }
}
