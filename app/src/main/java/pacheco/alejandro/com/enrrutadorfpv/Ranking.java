package pacheco.alejandro.com.enrrutadorfpv;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Ranking extends AppCompatActivity {
    ArrayList<String> orderedAddresses;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        setTitle("Rutas Optimizadas");

        orderedAddresses = getIntent().getStringArrayListExtra("list");
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, orderedAddresses);
        final ListView lv = (ListView)findViewById(R.id.rank_listview);
        lv.setAdapter(adapter);
        lv.setClickable(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                Object o = lv.getItemAtPosition(position);
                System.out.println(o);
                launchMap(o.toString());
            }
        });

    }


    void launchMap(String address) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
}
