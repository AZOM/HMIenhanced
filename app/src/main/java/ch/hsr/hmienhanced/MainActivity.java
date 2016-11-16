package ch.hsr.hmienhanced;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Main activity that is responsible for showing the fragments in need.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideActionBar();

        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate() -> Loading initial fragment");
        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment_container, new ARDiscoveryFragment())
                .commit();
    }

    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            Log.d(TAG, "hideActionBar() -> Hiding action bar");
            actionBar.hide();
        }
    }

}
