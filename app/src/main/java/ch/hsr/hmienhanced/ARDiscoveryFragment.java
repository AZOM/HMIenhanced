package ch.hsr.hmienhanced;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ListFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.hsr.hmienhanced.ardrone.DroneDiscoverer;

/**
 * Responsible class to discover and list the available AR Drones.
 */

public class ARDiscoveryFragment extends ListFragment {

    private static final String TAG = ARDiscoveryFragment.class.getSimpleName();

    /**
     * List of runtime permission we need.
     */
    private static final String[] PERMISSIONS_NEEDED = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };

    /**
     * Code for permission request result handling.
     */
    private static final int REQUEST_CODE_PERMISSIONS_REQUEST = 1;

    // this block loads the native libraries - it is mandatory
    static {
        ARSDK.loadSDKLibs();
    }

    private final List<ARDiscoveryDeviceService> mDronesList = new ArrayList<>();
    private final BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mDronesList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDronesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                rowView = inflater.inflate(android.R.layout.simple_list_item_1, null);

                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.text = (TextView) rowView.findViewById(android.R.id.text1);
                rowView.setTag(viewHolder);
            }

            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) getItem(position);
            holder.text.setText(service.getName());

            return rowView;
        }
    };

    private final DroneDiscoverer.Listener mDiscovererListener = new DroneDiscoverer.Listener() {

        @Override
        public void onDronesListUpdated(List<ARDiscoveryDeviceService> dronesList) {
            mDronesList.clear();
            mDronesList.addAll(dronesList);

            Log.d(TAG, "onDronesListUpdated() -> drones: " + dronesList.toString());
            mAdapter.notifyDataSetChanged();
        }
    };

    private DroneDiscoverer mDroneDiscoverer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_ardiscovery, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        checkPermissions();

        initListView();

        mDroneDiscoverer = new DroneDiscoverer(getActivity());
    }

    private void checkPermissions() {
        Set<String> permissionsToRequest = new HashSet<>();
        for (String permission : PERMISSIONS_NEEDED) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
                    Toast.makeText(getActivity(), "Please allow permission " + permission, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                } else {
                    permissionsToRequest.add(permission);
                }
            }
        }

        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(getActivity(),
                    permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                    REQUEST_CODE_PERMISSIONS_REQUEST);
        }
    }

    private void initListView() {
        setListAdapter(mAdapter);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ARDiscoveryDeviceService service = (ARDiscoveryDeviceService) mAdapter.getItem(position);
                ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());

                switch (product) {
                    case ARDISCOVERY_PRODUCT_ARDRONE:
                    case ARDISCOVERY_PRODUCT_BEBOP_2:
                        Log.i(TAG, "onItemClick() -> Init MainFragment with selected AR drone");
                        final MainFragment fragment = MainFragment.newInstance(service);

                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_fragment_container, fragment)
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .commit();
                        break;

                    default:
                        Log.e(TAG, "The type " + product + " is not supported yet.");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup the drone discoverer and register as listener
        mDroneDiscoverer.setup();
        mDroneDiscoverer.addListener(mDiscovererListener);

        // start discovering
        mDroneDiscoverer.startDiscovering();
    }

    @Override
    public void onPause() {
        super.onPause();

        // clean the drone discoverer object
        mDroneDiscoverer.stopDiscovering();
        mDroneDiscoverer.cleanup();
        mDroneDiscoverer.removeListener(mDiscovererListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean denied = false;
        if (permissions.length == 0) {
            // canceled, finish
            denied = true;
        } else {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    denied = true;
                }
            }
        }

        if (denied) {
            Toast.makeText(getActivity(), "At least one permission is missing.", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    public static class ViewHolder {
        TextView text;
    }
}
