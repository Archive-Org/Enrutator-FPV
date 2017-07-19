package pacheco.alejandro.com.enrrutadorfpv;

import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    final int CAMERA_CAPTURE = 1;
    ArrayList<String> addressesList = new ArrayList<>();
    ArrayAdapter<String> adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                addressesList);
        ListView lv = (ListView)findViewById(R.id.list_view);
        lv.setAdapter(adapter);

    }

    public void capture(View v) {
        // use standard intent to capture an image
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // we will handle the returned data in onActivityResult
        startActivityForResult(captureIntent, CAMERA_CAPTURE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_CAPTURE) {
                // get the Uri for the captured image
                Uri picUri = data.getData();
                CropImage.activity(picUri)
                        .start(this);
            }
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);

                String filepath = result.getUri().getPath();

                File imagefile = new File(filepath);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(imagefile);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Bitmap res = BitmapFactory.decodeStream(fis);


                String imgString = Base64.encodeToString(getBytesFromBitmap(res),
                        Base64.NO_WRAP);

                try {
                    analyze(imgString);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }

    void analyze(String data) throws IOException {

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n  \"requests\":[\n    {\n      \"image\":{\n        \"content\":\""+ data +"\"\n      },\n      \"features\":[\n        {\n          \"type\":\"TEXT_DETECTION\",\n          \"maxResults\":1\n        }\n      ]\n    }\n  ]\n}");

        Request request = new Request.Builder()
                .url("https://vision.googleapis.com/v1/images:annotate?key=AIzaSyAGO2lv4k0hvP-_BXA32AXq7rWhA1ngJnU")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();
        Toast.makeText(getApplicationContext(), "Analizando...", Toast.LENGTH_SHORT).show();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), "Error de coneccion", Toast.LENGTH_SHORT).show();
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String myResponse = response.body().string();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject obj = new JSONObject(myResponse);
                            if (obj.getJSONArray("responses").getJSONObject(0).length() < 1) {
                                Toast.makeText(getApplicationContext(),
                                        "Error, sin texto",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                String description =
                                        obj.getJSONArray("responses")
                                                .getJSONObject(0)
                                                .getJSONArray("textAnnotations")
                                                .getJSONObject(0)
                                                .get("description")
                                                .toString();
                                System.out.println("Found: " + description);
                                validate(description);
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }


                    }
                });

            }
        });
    }

    void validate(final String address) throws IOException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(address , "UTF-8") + "&key=AIzaSyAGO2lv4k0hvP-_BXA32AXq7rWhA1ngJnU")
                .get()
                .addHeader("content-type", "application/json")
                .addHeader("cache-control", "no-cache")
                .build();

        Toast.makeText(getApplicationContext(), "Validando...", Toast.LENGTH_SHORT).show();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(myResponse);
                        try {
                            JSONObject obj = new JSONObject(myResponse);

                            String status = obj.getString("status");
                            if (status.equals("OK")) {
                                String formatted_address = obj.getJSONArray("results").getJSONObject(0).get("formatted_address").toString();
                                addressesList.add(formatted_address);
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getApplicationContext(), "Invalida", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                });

            }
        });
    }



    public void optimizeRoutes(View v) throws IOException {
        OkHttpClient client = new OkHttpClient();
        final String origin = "Sevilla, Spain";
        final String separator = "%7C";

        // TODO implement as String constructor
        String baseUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                URLEncoder.encode(origin , "UTF-8") +
                "&destination=" + URLEncoder.encode(origin , "UTF-8") + "&waypoints=optimize%3Atrue%7C";

        // Append addresses as waypoints
        for (String address : addressesList) {
            baseUrl += URLEncoder.encode(address , "UTF-8") + separator;
        }
        System.out.println(baseUrl);
        // Remove last separator
        baseUrl = baseUrl.substring(0, baseUrl.length() - separator.length());

        // Add key
        baseUrl += "&key=AIzaSyAGO2lv4k0hvP-_BXA32AXq7rWhA1ngJnU";


        Request request = new Request.Builder()
                .url(baseUrl)
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(getApplicationContext(), "Error de conecion", Toast.LENGTH_SHORT).show();
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String myResponse = response.body().string();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //System.out.println(myResponse);

                        try {
                            JSONObject obj = new JSONObject(myResponse);
                            // Check routes status OK
                            if (!obj.getString("status").equals("OK")) {
                                Toast.makeText(getApplicationContext(), "Ruta fallo al optimizarse", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            JSONArray waypoints = obj.getJSONArray("geocoded_waypoints");
                            for (int i = 0; i < waypoints.length(); i++) {
                                if (!waypoints.getJSONObject(i).getString("geocoder_status").equals("OK")) {
                                    Toast.makeText(getApplicationContext(), "Ruta fallo al optimizarse", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }

                            JSONArray order = obj.getJSONArray("routes").getJSONObject(0).getJSONArray("waypoint_order");

                            ArrayList<Integer> orderList = new ArrayList<Integer>();
                            for (int i=0;i<order.length();i++){
                                orderList.add(order.getInt(i));
                            }

                            System.out.println(orderList);

                            ArrayList<String> orderedAddresses = new ArrayList<String>();
                            for (int i = 0; i < orderList.size(); i++) {
                                orderedAddresses.add(addressesList.get(orderList.indexOf(i)));
                            }

                            System.out.println(orderedAddresses);

                            Intent intent = new Intent(getBaseContext(), Ranking.class);
                            intent.putExtra("list", orderedAddresses);
                            startActivity(intent);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void deleteLast(View view) {
        if (addressesList.size() > 1) {
            addressesList.remove(addressesList.size()-1);
            adapter.notifyDataSetChanged();
        }
    }
}
