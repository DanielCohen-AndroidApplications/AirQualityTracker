package com.airqualitytracker022490.daniel.airqualitytracker;

/**
 * Created by Daniel on 3/3/2016.
 */
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by Daniel on 2/7/2016.
 */
public class MainActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback{

    //Google map frag automatically drops a pin at last know location; the user can input text to get a new location.
    // The controls below allow them to get air quality info at the selected location.
    private GoogleMap map;
    SupportMapFragment fm;

    //Doubles for lat and long; either retrieved by GPS or text input
    Double latitude,longitude;

    //EditText to input a location as text (city, country, address, zip, etc.)
    EditText editText;

    //buttons to respectively: get location from text input, get location from gps, get air quality index, get air quality description,
    //get main pollutant, get recommendations for children, athletes, health, indoors, outdoors, effects of the main pollutant, and causes.
    Button locationBtn, gpsBtn, aqiBtn,descriptionBtn,polBtn,childBtn,sportBtn,healthBtn,indoorBtn,outdoorBtn,effectsBtn,causesBtn;

    //textViews which respectively display the info retrieved by the buttons above.
    TextView aqiTextView,descriptionTextView,polTextView,childTextView,
            sportTextView, healthTextView,indoorTextView, outdoorTextView,effectsTextView, causesTextView;

    //web service for accessing air quality API
    HandleService service;

    //An HTTP response from the air quality API
    HttpResponse response;

    //JSONObjects representing info retrieved from the API by clicking the above buttons
    JSONObject aqi,description,pol,recommendations,jsonChild,jsonsport,jsonhealth,
            jsonindoors,jsonoutdoors,jsoneffects, jsoncauses, locationInfo;

    //Booleans which indicate whether a piece of data is being retrieved...
    Boolean gettingAqi, gettingDescription,gettingPol, gettingChild, gettingSport,
            gettingHealth,gettingIndoors, gettingOutdoors, gettingEffects, gettingCauses,

    //and if so whether or not an exception has occurred
    aqiException,descriptionException, pollutantException,childException,sportException,healthException,
            indoorsException, outdoorsException,effectsException, causesException, usingGps;

    //a stringBuilder which accumulates data retrieved by the API, for insertion into a snippet in the map marker.
    StringBuilder mySnippet;

    //string representations of retrieved data which are displayed in their appropriate textviews.
    String responseString,location,strAdd,selection,result,breezometerAqi,aqDescription, pollutant, child, sport,health, indoors, outdoors,effects,causes, emoticon;
    Tracker tracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar();
        //Google Analytics
        try {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(MainActivity.this);
            analytics.setLocalDispatchPeriod(1800);
            tracker = analytics.newTracker("UA-74634780-1");
            tracker.enableExceptionReporting(true);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableAutoActivityTracking(true);
        } catch (Exception e){
            Log.v("_dan", e.getMessage());
        }
        //default initial values, in case gps is not available
        latitude=0.0;
        longitude=0.0;

        //Google map fragment
        fm = (SupportMapFragment)  getSupportFragmentManager().findFragmentById(R.id.map); map = fm.getMap(); map.setMapType(GoogleMap.MAP_TYPE_NORMAL); map.setMyLocationEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true); map.getUiSettings().setCompassEnabled(true); map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true); map.getUiSettings().setRotateGesturesEnabled(true);


        //assignments for textViews, buttons, and the one editText
        locationBtn=(Button) findViewById(R.id.locationBtn); editText= (EditText) findViewById(R.id.editText);
        aqiTextView=(TextView) findViewById(R.id.aqiTextView); aqiBtn=(Button) findViewById(R.id.aqiBtn);
        descriptionTextView=(TextView)findViewById(R.id.descriptionTextView); descriptionBtn=(Button) findViewById(R.id.descriptionBtn);
        polTextView=(TextView) findViewById(R.id.polTextView); polBtn=(Button) findViewById(R.id.polBtn);
        childTextView=(TextView) findViewById(R.id.childTextView); childBtn=(Button) findViewById(R.id.childBtn);
        sportTextView=(TextView) findViewById(R.id.sportTextView); sportBtn=(Button) findViewById(R.id.sportBtn);
        healthTextView=(TextView) findViewById(R.id.healthTextView); healthBtn=(Button) findViewById(R.id.healthBtn);
        indoorTextView=(TextView) findViewById(R.id.indoorTextView); indoorBtn=(Button) findViewById(R.id.indoorBtn);
        outdoorTextView=(TextView) findViewById(R.id.outdoorTextView);outdoorBtn=(Button) findViewById(R.id.outdoorBtn);
        effectsTextView=(TextView) findViewById(R.id.effectsTextView); effectsBtn=(Button) findViewById(R.id.effectsBtn);
        causesTextView=(TextView) findViewById(R.id.causesTextView); causesBtn=(Button) findViewById(R.id.causesBtn);
        gpsBtn = (Button) findViewById(R.id.gpsBtn);

        //API webservice
        service = new HandleService();

        //The app automatically tries to retrieve current location
        getCurrentLocation();


        //Set listeners for the buttons
        locationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!editText.getText().toString().equals("")) {
                    map.clear();
                    usingGps = false;
                    aqiBtn.setVisibility(View.VISIBLE); aqiTextView.setVisibility(View.VISIBLE); aqiTextView.setText("");gettingAqi = false;aqiException = false;
                    descriptionBtn.setVisibility(View.VISIBLE); descriptionTextView.setVisibility(View.VISIBLE); descriptionTextView.setText("");gettingDescription = false; descriptionException = false;
                    polBtn.setVisibility(View.VISIBLE); polTextView.setVisibility(View.VISIBLE); polTextView.setText("");gettingPol = false; pollutantException = false;
                    childBtn.setVisibility(View.VISIBLE);childTextView.setVisibility(View.VISIBLE); childTextView.setText("");gettingChild = false; childException = false;
                    sportBtn.setVisibility(View.VISIBLE); sportTextView.setVisibility(View.VISIBLE); sportTextView.setText("");gettingSport = false; sportException = false;
                    healthBtn.setVisibility(View.VISIBLE);healthTextView.setVisibility(View.VISIBLE);healthTextView.setText(""); gettingHealth = false; healthException = false;
                    indoorBtn.setVisibility(View.VISIBLE);indoorTextView.setVisibility(View.VISIBLE);indoorTextView.setText("");gettingIndoors=false; indoorsException = false;
                    outdoorTextView.setText(""); outdoorTextView.setVisibility(View.VISIBLE);outdoorTextView.setText(""); gettingOutdoors = false; outdoorsException = false;
                    effectsTextView.setText(""); effectsTextView.setVisibility(View.VISIBLE);effectsTextView.setText(""); gettingEffects = false; effectsException = false;
                    causesTextView.setText(""); causesTextView.setVisibility(View.VISIBLE);causesTextView.setText(""); gettingCauses = false; causesException = false;
                    mySnippet = new StringBuilder("");
                    location = editText.getText().toString().replace(",", "").replace(" ", "+").replace("\n","");
                    clearExceptions();
                    new LatLongTask().execute();
                }
            }
        });
        gpsBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                editText.setText(getCurrentLocation()[2]);
            }
        });
        aqiBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                map.clear();
                selection="aqi";
                gettingAqi=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        descriptionBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                map.clear();
                selection="description";
                gettingDescription=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        polBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                map.clear();
                selection="pol";
                gettingPol=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        childBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "child";
                gettingChild=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        sportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "sport";
                gettingSport=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        healthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "health";
                gettingHealth= true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        indoorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "indoors";
                gettingIndoors=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        outdoorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "outdoors";
                gettingOutdoors=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        effectsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "effects";
                gettingEffects=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });

        causesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.clear();
                selection = "causes";
                gettingCauses=true;
                clearExceptions();
                new MyTask().execute(location);
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
        super.onStop();
    }

    //This method gets the current location by best available provider; it is called onCreate and which the gpsBtn is clicked
    //The method sets the map and all location-relevant parameters to the retrieved location,

    public String[] getCurrentLocation(){
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location l = null;

        for(int i = 0; i < providers.size(); i++) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) {
                latitude = l.getLatitude();
                longitude = l.getLongitude();
                usingGps=true;
                strAdd = getCompleteAddressString(latitude, longitude);
                location=strAdd.replace(",","").replace(" ", "+");
                if(!(strAdd.equals(""))) {
                    locationBtn.setText("Get new location");
                    editText.setText(strAdd);
                }
                break;
            }
        }

        if(map != null){
            aqiBtn.setVisibility(View.VISIBLE);
            aqiTextView.setVisibility(View.VISIBLE);
            descriptionBtn.setVisibility(View.VISIBLE);
            descriptionTextView.setVisibility(View.VISIBLE);
            polBtn.setVisibility(View.VISIBLE);
            polTextView.setVisibility(View.VISIBLE);
            childBtn.setVisibility(View.VISIBLE);
            childTextView.setVisibility(View.VISIBLE);
            sportBtn.setVisibility(View.VISIBLE);
            sportTextView.setVisibility(View.VISIBLE);
            healthBtn.setVisibility(View.VISIBLE);
            healthTextView.setVisibility(View.VISIBLE);
            indoorBtn.setVisibility(View.VISIBLE);
            indoorTextView.setVisibility(View.VISIBLE);
            outdoorBtn.setVisibility(View.VISIBLE);
            outdoorTextView.setVisibility(View.VISIBLE);
            effectsBtn.setVisibility(View.VISIBLE);
            effectsTextView.setVisibility(View.VISIBLE);
            causesBtn.setVisibility(View.VISIBLE);
            causesTextView.setVisibility(View.VISIBLE);

            aqiTextView.setText("");
            descriptionTextView.setText("");
            polTextView.setText("");
            childTextView.setText("");
            sportTextView.setText("");
            healthTextView.setText("");
            indoorTextView.setText("");
            outdoorTextView.setText("");
            effectsTextView.setText("");
            causesTextView.setText("");

            mySnippet = new StringBuilder("");
            gettingAqi = false;
            gettingDescription = false;
            gettingPol = false;
            gettingChild = false;
            gettingSport = false;
            gettingHealth = false;
            gettingIndoors = false;
            gettingOutdoors = false;
            gettingEffects = false;
            gettingCauses = false;

            aqiException = false;
            childException = false;
            descriptionException = false;
            pollutantException = false;
            sportException = false;
            healthException = false;
            indoorsException = false;
            outdoorsException = false;
            effectsException = false;
            causesException = false;

            onMapReady(map);
        }
        // The method returns a String[] with three elements: lat, lng, and strAdd (string representions of the latitude, longitude, and the address)
        return new String[]{latitude+"",longitude+"",strAdd};
    }

    //Gets an address string approximating a latitude and longitude
    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<android.location.Address> addresses = geocoder
                    .getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                android.location.Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress
                            .append(returnedAddress.getAddressLine(i)).append(
                            "\n");
                }
                strAdd = strReturnedAddress.toString();

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return strAdd;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    //Asynchronous task for getting data from the air quality API and putting the data into the UI post execute
    class MyTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            result=service.getAQ(params[0], usingGps, latitude,longitude);
            if(selection.equals("aqi")) {
                try {
                    aqi = new JSONObject(result);
                    breezometerAqi=aqi.optString("breezometer_aqi");
                    int aqiInt=Integer.parseInt(breezometerAqi);
                    if(aqiInt>80){
                        emoticon="\uD83D\uDE00";
                    }else if(aqiInt>70){
                        emoticon="\uD83D\uDE0A";
                    }else if(aqiInt>60){
                        emoticon="\uD83D\uDE0C";
                    } else if(aqiInt>50){
                        emoticon="\uD83D\uDE10";
                    }else if(aqiInt>30){
                        emoticon="\uD83D\uDE1F";
                    }else{
                        emoticon="\uD83D\uDE31";
                    }
                    breezometerAqi=breezometerAqi+emoticon;
                    gettingAqi=true;
                } catch (Exception e) {
                    aqiException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("description")){
                try {
                    description = new JSONObject(result);
                    aqDescription=description.optString("breezometer_description");
                    gettingDescription=true;
                } catch (Exception e) {
                    descriptionException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("pol")){
                try {
                    pol = new JSONObject(result);
                    pollutant=pol.optString("dominant_pollutant_description");

                } catch (Exception e) {
                    pollutantException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("child")){
                try {
                    jsonChild = new JSONObject(result);
                    recommendations=jsonChild.optJSONObject("random_recommendations");
                    child=recommendations.optString("children");
                } catch (Exception e) {
                    childException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("sport")){
                try {
                    jsonsport = new JSONObject(result);
                    recommendations=jsonsport.optJSONObject("random_recommendations");
                    sport=recommendations.optString("sport");
                } catch (Exception e) {
                    sportException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("health")){
                try {
                    jsonhealth = new JSONObject(result);
                    recommendations=jsonhealth.optJSONObject("random_recommendations");
                    health=recommendations.optString("health");
                } catch (Exception e) {
                    healthException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("indoors")){
                try {
                    jsonindoors = new JSONObject(result);
                    recommendations=jsonindoors.optJSONObject("random_recommendations");
                    indoors=recommendations.optString("inside");
                } catch (Exception e) {
                    indoorsException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("outdoors")){
                try {
                    jsonoutdoors = new JSONObject(result);
                    recommendations=jsonoutdoors.optJSONObject("random_recommendations");
                    outdoors=recommendations.optString("outside");
                } catch (Exception e) {
                    outdoorsException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("effects")){
                try {
                    jsoneffects = new JSONObject(result);
                    recommendations=jsoneffects.optJSONObject("dominant_pollutant_text");
                    effects=recommendations.optString("effects");
                } catch (Exception e) {
                    sportException=true;
                    e.printStackTrace();
                }
            }else if(selection.equals("causes")){
                try {
                    jsoncauses = new JSONObject(result);
                    recommendations=jsoncauses.optJSONObject("dominant_pollutant_text");
                    causes=recommendations.optString("causes");
                } catch (Exception e) {
                    causesException=true;
                    e.printStackTrace();
                }
            }
            return result;

        }
        @Override
        protected void onPostExecute(String result) {
            if(gettingAqi) {
                try {
                    if(!mySnippet.toString().contains(breezometerAqi)) {
                        mySnippet.append(System.getProperty("line.separator") + "Air Quality Index = " + breezometerAqi);
                    }
                    aqiTextView.setTextColor(Color.parseColor(aqi.optString("breezometer_color")));
                    aqiTextView.setText(breezometerAqi);
                }catch(Exception e){
                    aqiException=true;
                }
            }
            if(gettingDescription) {
                try {
                    if(!mySnippet.toString().contains(aqDescription)) {
                        mySnippet.append(System.getProperty("line.separator") + aqDescription);
                    }
                    descriptionTextView.setTextColor(Color.parseColor(description.optString("breezometer_color")));
                    descriptionTextView.setText(description.optString("breezometer_description"));
                }catch(Exception e){
                    descriptionException=true;
                }
            }
            if(gettingPol) {
                try {
                    if(!mySnippet.toString().contains(pollutant)) {
                        mySnippet.append(System.getProperty("line.separator") + "Main pollutant = "+pollutant);
                    }
                    polTextView.setTextColor(Color.parseColor(pol.optString("breezometer_color")));
                    polTextView.setText(pol.optString("dominant_pollutant_description"));
                }catch (Exception e){
                    pollutantException=true;
                }
            }
            if(gettingChild) {
                try {
                    childTextView.setTextColor(Color.parseColor(jsonChild.optString("breezometer_color")));
                    childTextView.setText(child);
                }catch (Exception e){
                    childException=true;
                }
            }
            if(gettingSport) {
                try {

                    sportTextView.setTextColor(Color.parseColor(jsonsport.optString("breezometer_color")));
                    sportTextView.setText(sport);
                }catch (Exception e){
                    sportException=true;
                }
            }
            if(gettingHealth) {
                try {

                    healthTextView.setTextColor(Color.parseColor(jsonhealth.optString("breezometer_color")));
                    healthTextView.setText(health);
                }catch (Exception e){
                    healthException=true;
                }
            }
            if(gettingIndoors) {
                try {
                    indoorTextView.setTextColor(Color.parseColor(jsonindoors.optString("breezometer_color")));
                    indoorTextView.setText(indoors);
                }catch (Exception e){
                    indoorsException=true;
                }
            }
            if(gettingOutdoors) {
                try {
                    outdoorTextView.setTextColor(Color.parseColor(jsonoutdoors.optString("breezometer_color")));
                    outdoorTextView.setText(outdoors);
                }catch (Exception e){
                    outdoorsException=true;
                }
            }
            if(gettingEffects) {
                try {
                    if(!mySnippet.toString().contains(effects)) {
                        mySnippet.append(System.getProperty("line.separator") +"Health effects: " + System.getProperty("line.separator")+effects);
                    }
                    effectsTextView.setTextColor(Color.parseColor(jsoneffects.optString("breezometer_color")));
                    effectsTextView.setText(effects);
                }catch (Exception e){
                    effectsException=true;
                }
            }if(gettingCauses) {
                try {
                    causesTextView.setTextColor(Color.parseColor(jsoncauses.optString("breezometer_color")));
                    causesTextView.setText(causes);
                } catch (Exception e) {
                    causesException = true;
                }
            }
            onMapReady(map);

            if(aqiException){
                aqiTextView.setText("Info not available");
            }
            if(descriptionException){
                descriptionTextView.setText("Info not available");
            }
            if(pollutantException){
                polTextView.setText("Info not available");
            }
            if(childException){
                childTextView.setText("Info not available");
            }
            if(sportException){
                sportTextView.setText("Info not available");
            }
            if(healthException){
                healthTextView.setText("Info not available");
            }
            if(indoorsException){
                indoorTextView.setText("Info not available");
            }
            if(outdoorsException){
                outdoorTextView.setText("Info not available");
            }
            if(effectsException){
                effectsTextView.setText("Info not available");
            }
            if(causesException){
                causesTextView.setText("Info not available");
            }

        }
        @Override
        protected void onPreExecute() {
            if(gettingAqi) {
                aqiTextView.setText("...one sec");
            }
            if(gettingDescription) {
                descriptionTextView.setText("...one sec");
            }
            if(gettingPol) {
                polTextView.setText("...one sec");
            }
            if(gettingChild) {
                childTextView.setText("...one sec");
            }
            if(gettingSport) {
                sportTextView.setText("...one sec");
            }
            if(gettingHealth) {
                healthTextView.setText("...one sec");
            }
            if(gettingIndoors) {
                indoorTextView.setText("...one sec");
            }
            if(gettingOutdoors) {
                outdoorTextView.setText("...one sec");
            }
            if(gettingEffects) {
                effectsTextView.setText("...one sec");
            }
            if(gettingCauses) {
                causesTextView.setText("...one sec");
            }
        }
        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    //Asynchronous task for retrieving latitude and longitude from Google Maps API based on a string representation of a location input by the user
    class LatLongTask extends AsyncTask<String,Integer,JSONObject>{
        @Override
        protected JSONObject doInBackground(String...params) {
            JSONObject latLong=new JSONObject();
            try {
                latLong=getLatLong();
                JSONArray resultJSONArray = latLong.optJSONArray("results");
                JSONObject locationJSONObj = resultJSONArray.getJSONObject(0).optJSONObject("geometry").optJSONObject("location");
                latitude=locationJSONObj.optDouble("lat");
                longitude=locationJSONObj.optDouble("lng");
            }catch(Exception e){
                e.printStackTrace();
            }
            return latLong;
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            onMapReady(map);
        }
        @Override
        protected void onPreExecute() {

        }
        @Override
        protected void onProgressUpdate(Integer... values) {

        }
    }

    //JSON object containing location information retrieved from the Google Maps API
    public JSONObject getLatLong() {

        try {
            URI uri = new URI("https://maps.googleapis.com/maps/api/geocode/json?address=" +location+"&key=AIzaSyAOolIF3JIZfb-1PyotIkVYIV0LXNFW7fs");
            HttpGet request = new HttpGet(uri);
            HttpClient client = new DefaultHttpClient();
            response = client.execute(request);
            HttpEntity httpEntity = response.getEntity();
            responseString = EntityUtils.toString(httpEntity);
            locationInfo = new JSONObject(responseString);

        }catch(Exception e){
            Log.v("_dan,", "Get LatLong issue");
        }
        return locationInfo;

    }

    //This method focuses the camera on the appropriate map location (either from gps or text input), with a marker contianing air quality info
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng pos = new LatLng(latitude,longitude);
        googleMap.addMarker(new MarkerOptions().position(pos).title(location.replace("+", " ")).snippet(mySnippet.toString()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getApplication());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getBaseContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }
    public void clearExceptions(){
        aqiException = false;
        childException = false;
        descriptionException = false;
        pollutantException = false;
        sportException = false;
        healthException = false;
        indoorsException = false;
        outdoorsException = false;
        effectsException = false;
        causesException = false;
    }
}