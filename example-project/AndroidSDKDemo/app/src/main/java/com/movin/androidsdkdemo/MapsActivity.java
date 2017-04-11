package com.movin.androidsdkdemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.movin.caching.MovinCacheProtocol;
import com.movin.geojson.GeoLatLng;
import com.movin.maps.FloorPosition;
import com.movin.maps.GetDataListener;
import com.movin.maps.MovinEntity;
import com.movin.maps.MovinMap;
import com.movin.maps.MovinTileManifest;
import com.movin.maps.MovinTileProvider;
import com.movin.maps.SuccessListener;
import com.movin.movinsdk_googlemaps.MovinSupportMapFragment;
import com.movin.scanner.MovinBeaconScanner;
import com.movin.scanner.MovinBeaconScannerListener;
import com.movin.scanner.MovinRangedBeacon;
import com.movin.sdk.MovinSDK;
import com.movin.sdk.MovinSDKCallback;

import java.util.List;
import java.util.Locale;

import static com.movin.androidsdkdemo.R.id.map;
import static com.movin.sdk.MovinSDK.getMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, MovinSDKCallback, ActivityCompat.OnRequestPermissionsResultCallback, MovinBeaconScannerListener {

    private static final String TAG = "MovinSDK Demo";

    /**
     * The customer, required to initialize the MovinSDK
     */
    private String customer = // TODO set customer;
    /**
     * The apikey, required to initialize the MovinSDK
     */
    private String apikey = // TODO set apikey;

    /**
     * The mapId of the map we want to load, can also be retrieved from the movin portal
     */
    private String mapId = // TODO set mapId;

    /**
     * The mapStyle of the map we want to load. By default it is Default, but when custom map styles are designed in the portal we can change this.
     */
    private String mapStyle = "Default";

    /**
     * The google maps object on which we can show the map and highlight rooms.
     */
    private GoogleMap mMap;

    /**
     * The MovinBeaconScanner, responsible for scanning for beacons
     */
    private MovinBeaconScanner beaconScanner;

    /**
     * The polygon that indicates the room in which the closest beacon is, when it is triggered
     */
    private Polygon roomPolygon;

    /**
     * The mapFragment used to control the MovinMap with
     */
    private MovinSupportMapFragment mapFragment;

    /**
     * A view in which the floorSwitcher will be created
     */
    private TextView floorSwitcherView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (MovinSupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        // The drawing order of the map and POI's / Text Entities can be changed:
        mapFragment.setMovinTileLayerZIndex(0); // The Z index of the tile layer, defaults to 0
        mapFragment.setMovinDefaultLabelZIndex(4); // The Z index of Labels, defaults to 4
        mapFragment.setMovinDefaultMarkerZIndex(5); // The Z index of POI's, defaults to 5

        // Create the floorswitcher
        createFloorSwitcher();

        // Disable caching beacon data, so we can easily test new beacon positions
        // Parameters from left to right:
        //      allowCaching, whether or not the data can be cached or not, in this case we do false, so we will redownload on each app launch
        //      allowSyncing, whether or not the data can be synced after the first time download.
        //      refreshRate, after how many seconds the data should be refreshed. The refreshRate only allows refreshing over WiFi.
        //      forcedRefreshRate, after how many seconds the data should be refreshed, whether or not WiFi is available.
        //          A 0 value indicates that the data is never synced over anything other than WiFi
        //      timeoutWithCacheAvailable, after how many seconds the HTTP call is killed if there is a cached version available
        //      timeout, after how many seconds the HTTP call is killed if there is not a cached version available
        MovinCacheProtocol beaconCacheProtocol = new MovinCacheProtocol(false, true, 0, 0, 10, 60);
        MovinSDK.setCacheProtocol(MovinSDK.MOVIN_CACHEABLE_DATA_ALL, beaconCacheProtocol);

        // Start the initialization of the SDK
        MovinSDK.initialize(customer, apikey, this, this);
    }

    @Override
    protected void onStop() {
        // Stop the beacon scanner if it has been started
        if(beaconScanner != null) {
            beaconScanner.stop(this);
        }
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public synchronized void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Comment this to show out to hide the outside map (or set it to MAP_TYPE_NONE)
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // If the MovinSDK is already initialized, load the hardcoded active map
        if (MovinSDK.isInitialized()) {
            loadMap();
        }
    }

    /**
     * Called when the MovinSDK has finished initializing. Once this method is called and the
     * SDK has been successfully initialized you can start using the MovinSDK.
     * In this case we'll decide if the map can be shown already (GoogleMaps should be loaded for that
     * as well), and we'll create the beacon scanner.
     * @param success Whether or not the MovinSDK
     * @param exception If the initialization failed, the exception shows what went wrong.
     */
    @Override
    public synchronized void initialized(final boolean success, final Exception exception) {
        if(!success) {
            Log.e(TAG, "Error initializing the MovinSDK: " + exception.getLocalizedMessage());
            exception.printStackTrace();
            return;
        }
        // If the map is already loaded, load the hardcoded active map
        if(mMap != null) {
            loadMap();
        }

        // Start creating the beacon scanner
        createBeaconScanner();
    }

    /**
     * Loads the map so it is shown on GoogleMaps.
     */
    private void loadMap() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Find the requested MovinMap from the SDK
                final MovinMap map = getMap(mapId);

                // OPTIONAL: Start with initiating the map data download, so it'll be immediately available later on
                map.downloadMapData(new SuccessListener() {
                    @Override
                    public void onResult(boolean success, Exception exception) {
                        Log.i(TAG, "Downloaded map data: " + (success ? "true." : "false, " + exception.getLocalizedMessage()));
                    }
                });
                map.downloadBeaconData(new SuccessListener() {
                    @Override
                    public void onResult(boolean success, Exception exception) {
                        Log.i(TAG, "Downloaded beacon data: " + (success ? "true." : "false, " + exception.getLocalizedMessage()));
                    }
                });
                map.downloadTileManifest(new SuccessListener() {
                    @Override
                    public void onResult(boolean success, Exception exception) {
                        Log.i(TAG, "Downloaded tile manifest: " + (success ? "true." : "false, " + exception.getLocalizedMessage()));
                    }
                });

                // Get the TileManifest so we can show the map with the desired style
                map.getTileManifest(new GetDataListener<MovinTileManifest>() {
                    @Override
                    public void onGetData(MovinTileManifest manifest, Exception exception) {
                        if(exception != null) {
                            Log.i(TAG, "Could not get tile manifest, cannot show the map with the desired style");
                            return;
                        }

                        // Find the desired style
                        if(manifest.getStyles().containsKey(mapStyle)) {
                            mapFragment.setMovinMapStyle(manifest.getStyles().get(mapStyle));
                        } else {
                            Log.i(TAG, "Could not find style with name " + mapStyle + ", using default style");
                            mapFragment.setMovinMapStyle(manifest.getDefaultStyle());
                        }

                        // Set the map so it will become visible with the given style
                        mapFragment.setMovinMap(map);

                        // Make the map visible
                        mapFragment.fitToMap();

                        // Invalidate the options menu, since that is our temporary floor switcher
                        updateFloorSwitcher();
                    }
                });
            }
        });
    }

    /**
     * Tries to create a beacon scanner, but before a beacon scanner can be created we'll first
     * need to request the permissions required for iBeacon scanning, in case we are on Android version
     * 6.0 or higher.
     */
    private void createBeaconScanner() {
        // Check for permission request
        // First check whether or not the permission for fine location has been granted (only for android 6.0 and higher)
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // The permission was not given, request it from the user. This causes the Android system to show a request dialog.
            // The result of this request dialog comes in the onRequestPermissionsResult callback method.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            return;
        }

        // If the authorization has been granted, get the MovinBeaconScanner from the MovinSDK and start it
        try {
            beaconScanner = MovinSDK.getBeaconScanner();

            // Add the map, so we can scan for the beacons in this map
            beaconScanner.addMap(getMap(mapId), null);

            // Start the beaconScanner, so we can handle the beacon responses
            beaconScanner.start(this);

            // TODO: if a positioning engine wants to be created, that can be done as following (uncomment first):
//            // Get the positioning engine
//            final MovinPositioningEngine positioningEngine = MovinSDK.getMap(mapId).getPositioningEngine();
//            // Add a positioning listener to the engine
//            positioningEngine.addPositioningListener(new MovinPositioningListener() {
//                @Override
//                public void initializedPositioningEngine(boolean success, Exception exception) {
//                    if(success) {
//                        Log.i(TAG, "Successfully initialized positioning engine");
//                    } else {
//                        Log.e(TAG, "Error initializing positioning engine: " + exception.getLocalizedMessage());
//                        exception.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void updatedPosition(FloorPosition floorPosition) {
//                    // Update a marker or something with the new position.
//                    // Once the device got a position, this will be called 30 times a second to
//                    // enable a smooth experience.
//                    double lat = floorPosition.position.lat;
//                    double lng = floorPosition.position.lng;
//                    double floor = floorPosition.floor;
//                }
//
//                @Override
//                public void unknownLocation() {
//                    // The system no longer knows where you are, probably because you are no longer
//                    // in the area where the fingerprints were made.
//                }
//            });
//            // We can either start the positioning engine right away, which will take care of initializing
//            // the engine for us, or we can first initialize it, and then later start it.
//            positioningEngine.start();

        } catch(Exception ex) {
            Log.e(TAG, "Could not create a beacon scanner: " + ex.getLocalizedMessage());
        }
    }


    /**
     * Called when the user deiced whether or not he wants to allow location services.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // The user granted permissions for location
                // Continue creating the BeaconScanner
                createBeaconScanner();

            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED){
                // The user denied the permission.
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an alert to explain why the app needs location services
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MapsActivity.this)
                                    .setTitle("Location services")
                                    .setMessage("The app needs you to enable location services to help you in your visit.")
                                    .setPositiveButton("Okay, ask again", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Retry to initialize the SDK again, this will cause the system to request the
                                            // location services again
                                            createBeaconScanner();
                                        }
                                    })
                                    .setNegativeButton("I don't want it", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            // Do nothing
                                        }
                                    })
                                    .show();
                        }
                    });

                } else {
                    // Never ask again selected, or device policy prohibits the app from having that permission.
                    // Don't create the beaconScanner and don't use it.
                }
            }
        }
    }

    /**
     * This method is called by the MovinBeaconScanner to indicate a new beacon became the nearest beacon,
     * or, in case no more valid beacons are nearby it will provide a null value, so you know you are
     * no longer near your beacon infrastructure.
     * @param movinBeaconScanner
     * @param nearestBeacon
     */
    @Override
    public void didChangeNearestBeacon(MovinBeaconScanner movinBeaconScanner, MovinRangedBeacon nearestBeacon) {
        // If we already have a room highlighted, stop that highlight first
        if(roomPolygon != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    roomPolygon.remove();
                    roomPolygon = null;
                }
            });
        }

        // In case a nearest beacon is found, we will find the room it is placed in, to highlight it.
        if(nearestBeacon != null) {
            // Extract the map of the beacon
            // We can do this without null checks, since the valid beacon check already ensured
            // that getBeacon does not equal null, and the group and map of a MovinBeacon can never be null.
            MovinMap map = nearestBeacon.getBeacon().getGroup().getMap();

            // Extract the beacon position
            FloorPosition beaconPosition = nearestBeacon.getBeacon().getPosition();

            // Show the floor on which this beacon is
            setFloor(beaconPosition.floor);

            // Query the map to find the entity in which this beacon is placed in
            map.getEntitiesInShape(beaconPosition.position, beaconPosition.floor, new GetDataListener<List<MovinEntity>>() {
                @Override
                public void onGetData(List<MovinEntity> movinEntities, Exception e) {
                    // Find the first room or hall entity. Ignore all other entities. Other entities
                    // include the building, a text entity (in the future) et cetera.
                    // Halls and rooms usually don't collide, so selecting the first one is okay
                    MovinEntity entity = null;
                    for(MovinEntity ent : movinEntities) {
                        String type = ent.getSubType().getBaseType();
                        if(type.equalsIgnoreCase("Room") || type.equalsIgnoreCase("Hall")) {
                            entity = ent;
                            break;
                        }
                    }

                    // If the MovinEntity is still null, it has not been placed in a room or hallway
                    // ignore this situation for now, since that should not happen
                    if(entity == null) {
                        return;
                    }

                    // NOTE: It cannot happen that one beacon is triggered twice in a row, but
                    // since there are more beacons in one room, it can result in a single room
                    // being selected multiple times in a row.

                    // Highlight the entity
                    highlightEntity(entity);
                }
            });
        } else {
            // Make a simple toast to notify that we are no longer near any valid beacons.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "No longer near any valid beacons...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Highlights the given entity by creating and adding the roomPolygon to the GoogleMap
     * @param entity
     */
    private void highlightEntity(final MovinEntity entity) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Create a new polygon from the geometry data of the given MovinEntity to highlight
                PolygonOptions options = new PolygonOptions();
                for(GeoLatLng coord : entity.getGeometry().getPointsForIntersect()) {
                    options.add(new LatLng(coord.lat, coord.lng));
                }
                options.fillColor(Color.argb(100,0,0,255));
                options.strokeWidth(0);
                options.zIndex(10);
                roomPolygon = mMap.addPolygon(options);

                // Also show the room name in a toast.
                String name = entity.getName();
                if(name == null) {
                    name = "unnamed";
                }

                Toast.makeText(getApplicationContext(), "Highlighting entity with code: " + name, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows the tiles of the selected floor.
     * @param floor
     */
    private void setFloor(float floor) {
        if(mapFragment != null && mapFragment.getTileProvider() != null) {
            mapFragment.getTileProvider().setFloor(floor);
        }
    }



    /**
     * This method is called by the MovinBeaconScanner so you can decide if a certain beacon can
     * compete for being the nearest beacon. This allows you to filter any random beacons. In this
     * case we only allow beacons which is also stored in the SDK, by checking if the underlying
     * MovinBeacon (movinRangedBeacon.getBeacon()) is not null.
     * @param movinBeaconScanner
     * @param movinRangedBeacon
     * @return Whether or not this beacon should compete for being a nearest beacon.
     */
    @Override
    public boolean isValidNearestBeacon(MovinBeaconScanner movinBeaconScanner, MovinRangedBeacon movinRangedBeacon) {
        // Due to the structure I implemented this sample app, there is a slight possibility that the
        // beacon scanner has selected a nearest beacon before GoogleMaps has been initialized.
        // To avoid random null pointer exceptions, lets ignore beacons until GoogleMaps has been
        // initialized.
        if(mMap == null) {
            return false;
        }

        // Otherwise just check if the scanned beacon is known (thus we can find in which room it is placed).
        return movinRangedBeacon.getBeacon() != null && movinRangedBeacon.getBeacon().getPosition() != null;
        // NOTE: a MovinRangedBeacon is a beacon it has actually seen in its vicinity. Only if the system
        // actually recognizes this beacon as one of our own (so it can be matched with the beacons stored
        // in the Movin portal), the actual MovinBeacon will be set, and obtainable through getBeacon().
        // This MovinBeacon also contains the position of the beacon that was scanned.
    }

    /**
     * Called each time the MovinBeaconScanner has ranged beacons.
     * @param movinBeaconScanner
     * @param beacons
     */
    @Override
    public void didRangeBeacons(MovinBeaconScanner movinBeaconScanner, List<MovinRangedBeacon> beacons) {
        // In this sample app we can ignore this, we won't be using it.
        // However what you can expect in this method is once every second a list of all the beacons that have
        // been ranged, sorted from close to far.
    }


    //region Floor switcher
    /**
     * Creates a floor switcher button in the bottom right of the screen. Later this will be further developed
     * and added to the SDK itself.
     */
    private void createFloorSwitcher() {
        // Convert the required dip values to pixels
        Resources r = getResources();
        int px48 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
        int px12 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, r.getDisplayMetrics());

        // Create a parent view, required to push the floor switcher to the bottom right.
        RelativeLayout parent = new RelativeLayout(this);
        RelativeLayout.LayoutParams parentParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parent.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        parent.setPadding(0,0,px12,px12);
        parent.setLayoutParams(parentParams);

        // Create the floor switcher view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, px12, px12);
        floorSwitcherView = new TextView(this);
        floorSwitcherView.setText("?");
        floorSwitcherView.setTextColor(Color.WHITE);
        floorSwitcherView.setWidth(px48);
        floorSwitcherView.setHeight(px48);
        floorSwitcherView.setGravity(Gravity.CENTER);
        floorSwitcherView.setLayoutParams(params);
        floorSwitcherView.setBackgroundColor(Color.rgb(0, 140, 161));

        parent.addView(floorSwitcherView);
        ViewGroup parentView = (ViewGroup) mapFragment.getView();
        parentView.addView(parent);
    }

    /**
     * Called when the floor switcher can be updated, because a new map is visible.
     */
    private void updateFloorSwitcher() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(mapFragment != null) {
                    // Update the current shown floor
                    MovinTileProvider tileProvider = mapFragment.getTileProvider();
                    floorSwitcherView.setText(String.format(Locale.ENGLISH, "%d", (int)tileProvider.getFloor()));

                    // Update what happens on the onClick
                    floorSwitcherView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            PopupMenu popup = new PopupMenu(MapsActivity.this, view);
                            Menu menu = popup.getMenu();
                            MovinMap selectedMap = mapFragment.getMovinMap();
                            if(selectedMap != null) {
                                for(int i = (int)selectedMap.getLowestFloorNumber(); i <= (int)selectedMap.getHighestFloorNumber(); i++) {
                                    menu.add(Menu.NONE, i, i - (int)selectedMap.getLowestFloorNumber(), String.format(Locale.ENGLISH, "%d", i));
                                }
                            }
                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    mapFragment.setFloor((double)item.getItemId());
                                    floorSwitcherView.setText(String.format(Locale.ENGLISH, "%d", item.getItemId()));
                                    return true;
                                }
                            });
                            popup.show();
                        }
                    });
                }

            }
        });
    }
    //endregion
}
