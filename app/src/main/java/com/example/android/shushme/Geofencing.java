package com.example.android.shushme;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

// DONE (1) Create a Geofencing class with a Context and GoogleApiClient constructor that
// initializes a private member ArrayList of Geofences called mGeofenceList

public class Geofencing implements ResultCallback {

    // Constants

    public static final String LOG_TAG = Geofencing.class.getSimpleName();

    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours

    private static final float GEOFENCE_RADIUS = 50; // 50 meters

    // Class attributes

    private Context mContext;

    private GoogleApiClient mGoogleApiClient;

    private List<Geofence> mGeofences;

    private PendingIntent mGeofencePendingIntent;

    public Geofencing(Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;
        mGeofences = new ArrayList<>();
        mGeofencePendingIntent = null;
    }

    // DONE (6) Inside Geofencing, implement a public method called registerAllGeofences that
    // registers the GeofencingRequest by calling LocationServices.GeofencingApi.addGeofences
    // using the helper functions getGeofencingRequest() and getGeofencePendingIntent()

    /***
     * Registers the list of Geofences specified in mGeofenceList with Google Place Services
     * Uses {@code #mGoogleApiClient} to connect to Google Place Services
     * Uses {@link #getGeofencingRequest} to get the list of Geofences to be registered
     * Uses {@link #getGeofencePendingIntent} to get the pending intent to launch the IntentService
     * when the Geofence is triggered
     * Triggers {@link #onResult} when the geofences have been registered successfully
     */
    public void registerAllGeofences() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            try {
                // TODO review why this API has been deprecated
                LocationServices.GeofencingApi.addGeofences(
                        mGoogleApiClient,
                        getGeofencingRequest(),
                        getGeofencePendingIntent()
                ).setResultCallback(this);
            } catch (SecurityException securityException) {
                // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                Log.e(LOG_TAG, "Security exception: " + securityException.getMessage(), securityException);
            }
        }
    }

    // DONE (7) Inside Geofencing, implement a public method called unRegisterAllGeofences that
    // unregisters all geofences by calling LocationServices.GeofencingApi.removeGeofences
    // using the helper function getGeofencePendingIntent()

    /***
     * Unregisters all the Geofences created by this app from Google Place Services
     * Uses {@code #mGoogleApiClient} to connect to Google Place Services
     * Uses {@link #getGeofencePendingIntent} to get the pending intent passed when
     * registering the Geofences in the first place
     * Triggers {@link #onResult} when the geofences have been unregistered successfully
     */
    public void unRegisterAllGeofences() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            try {
                // TODO review why this API has been deprecated
                LocationServices.GeofencingApi.removeGeofences(
                        mGoogleApiClient,
                        // This is the same pending intent that was used in registerGeofences
                        getGeofencePendingIntent()
                ).setResultCallback(this);
            } catch (SecurityException securityException) {
                // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                Log.e(LOG_TAG, "Security exception: " + securityException.getMessage(), securityException);
            }
        }
    }

    // DONE (2) Inside Geofencing, implement a public method called updateGeofencesList that
    // given a PlaceBuffer will create a Geofence object for each Place using Geofence.Builder
    // and add that Geofence to mGeofenceList

    /***
     * Updates the local ArrayList of Geofences using data from the passed in list
     * Uses the Place ID defined by the API as the Geofence object Id
     *
     * @param placeBuffer the PlaceBuffer result of the getPlaceById call
     */
    public void updateGeofencesList(PlaceBuffer placeBuffer) {
        mGeofences = new ArrayList<>();

        if (placeBuffer != null && placeBuffer.getCount() > 0) {
            for (Place place : placeBuffer) {
                // Read the place information from the DB cursor
                String id = place.getId();
                LatLng latLng = place.getLatLng();

                // Build a Geofence object
                Geofence geofence = new Geofence.Builder()
                        .setRequestId(id)
                        // Geofence expiration
                        // NEVER EXPIRE - geofence stays there even when the app gets uninstalled
                        // For production app, should use JobScheduler and register geofence every single day !
                        .setExpirationDuration(GEOFENCE_TIMEOUT) // 24hrs in this case
                        .setCircularRegion(latLng.latitude, latLng.longitude, GEOFENCE_RADIUS) // 50 meters in this case
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT) // When the geofence gets activated
                        .build();

                // Add the geofence to the list
                mGeofences.add(geofence);
            }
        }
    }

    // DONE (3) Inside Geofencing, implement a private helper method called getGeofencingRequest that
    // uses GeofencingRequest.Builder to return a GeofencingRequest object from the Geofence list

    /***
     * Creates a GeofencingRequest object using the mGeofenceList ArrayList of Geofences
     * Used by {@code #registerGeofences}
     *
     * @return the GeofencingRequest object
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // If device is inside geofence when the geofence is created, trigger the enter event immediately
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofences);
        return builder.build();
    }

    // DONE (5) Inside Geofencing, implement a private helper method called getGeofencePendingIntent that
    // returns a PendingIntent for the GeofenceBroadcastReceiver class

    /***
     * Creates a PendingIntent object using the GeofenceTransitionsIntentService class
     * Used by {@code #registerGeofences}
     *
     * @return the PendingIntent object
     */
    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent == null) {
            Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
            mGeofencePendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(LOG_TAG, String.format("Error adding/removing geofence : %s", result.getStatus().toString()));
    }
}
