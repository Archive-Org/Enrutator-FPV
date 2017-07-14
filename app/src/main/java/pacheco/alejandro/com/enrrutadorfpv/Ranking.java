package pacheco.alejandro.com.enrrutadorfpv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
        ListView lv = (ListView)findViewById(R.id.rank_listview);
        lv.setAdapter(adapter);

    }
}
