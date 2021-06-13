/*
 Copyright 2013 Sebastián Katzer

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package de.appplant.cordova.plugin.background;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONObject;

import de.appplant.cordova.plugin.background.UploadForegroundService.UploadForegroundBinder;

import static android.content.Context.BIND_AUTO_CREATE;
import static de.appplant.cordova.plugin.background.BackgroundModeExt.clearKeyguardFlags;

public class BackgroundMode extends CordovaPlugin {

    // Event types for callbacks
    private enum Event { ACTIVATE, DEACTIVATE, FAILURE }

    // Plugin namespace
    private static final String JS_NAMESPACE = "cordova.plugins.backgroundMode";

    // Flag indicates if the app is in background or foreground
    private boolean inBackground = false;

    // Flag indicates if the plugin is enabled or disabled
    private boolean isDisabled = true;

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Service that keeps the app awake
    private Service service;

    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected (ComponentName name, IBinder service)
        {
            if(cordova.getActivity().getLocalClassName().contains("Upload")){
                UploadForegroundBinder binder = (UploadForegroundBinder) service;
                BackgroundMode.this.service = binder.getService();
            }else if(cordova.getActivity().getLocalClassName().contains("Download")){
                DownloadForegroundService.DownloadForegroundBinder binder = (DownloadForegroundService.DownloadForegroundBinder) service;
                BackgroundMode.this.service = binder.getService();
            }
        }

        @Override
        public void onServiceDisconnected (ComponentName name)
        {
            fireEvent(Event.FAILURE, "'service disconnected'");
        }
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when
     *                 calling back into JavaScript.
     *
     * @return Returning false results in a "MethodNotFound" error.
     */


    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callback)
    {
        boolean validAction = true;

        switch (action)
        {
            case "configure":
                configure(args.optJSONObject(0), args.optBoolean(1));
                break;
            case "enable":
                enableMode();
                break;
            case "disable":
                disableMode();
                break;
            default:
                validAction = false;
        }

        if (validAction) {
            callback.success();
        } else {
            callback.error("Invalid action: " + action);
        }

        return validAction;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking)
    {
        try {
            //TODO temporary commented
//            inBackground = true;
//            startService();
        } finally {
            clearKeyguardFlags(cordova.getActivity());
        }
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop () {
        clearKeyguardFlags(cordova.getActivity());
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume (boolean multitasking)
    {
        //TODO temporary commented
//        inBackground = false;
//        stopService();
    }

    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy()
    {
        stopService();
//        android.os.Process.killProcess(android.os.Process.myPid());
        cordova.getActivity().finish();
    }

    /**
     * Enable the background mode.
     */
    private void enableMode()
    {
        //TODO temporary commented

        isDisabled = false;

//        if (inBackground) {
            startService();
//        }
    }

    /**
     * Disable the background mode.
     */
    private void disableMode()
    {
        stopService();
        isDisabled = true;
    }

    /**
     * Update the default settings and configure the notification.
     *
     * @param settings The settings
     * @param update A truthy value means to update the running service.
     */
    private void configure(JSONObject settings, boolean update)
    {
        if (update) {
            updateNotification(settings);
        } else {
            setDefaultSettings(settings);
        }
    }

    /**
     * Update the default settings for the notification.
     *
     * @param settings The new default settings
     */
    private void setDefaultSettings(JSONObject settings)
    {
        defaultSettings = settings;
    }

    /**
     * Returns the settings for the new/updated notification.
     */
    static JSONObject getSettings () {
        return defaultSettings;
    }

    /**
     * Update the notification.
     *
     * @param settings The config settings
     */
    private void updateNotification(JSONObject settings)
    {
        if (isBind) {
            if(isDownload())
                ((DownloadForegroundService)service).updateNotification(settings);
            else if(isUpload()){
                ((UploadForegroundService)service).updateNotification(settings);
            }
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService()
    {
        Activity context = cordova.getActivity();

        if (isDisabled || isBind)
            return;

        Intent intent;
        if(isUpload())
            intent = new Intent(context, UploadForegroundService.class);
        else if(isDownload()){
            intent = new Intent(context, DownloadForegroundService.class);
        }else{
            return;
        }

        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            fireEvent(Event.ACTIVATE, null);
            context.startService(intent);
        } catch (Exception e) {
            fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
        }

        isBind = true;
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService()
    {
        Activity context = cordova.getActivity();
        Intent intent;
        if(isUpload())
           intent = new Intent(context, UploadForegroundService.class);
        else if(isDownload()){
            intent = new Intent(context, DownloadForegroundService.class);
        }else{
            return;
        }

        if (!isBind) return;

        fireEvent(Event.DEACTIVATE, null);
        context.unbindService(connection);
        context.stopService(intent);

        isBind = false;
    }

    /**
     * Fire vent with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    private void fireEvent (Event event, String params)
    {
        String eventName = event.name().toLowerCase();
        Boolean active   = event == Event.ACTIVATE;

        String str = String.format("%s._setActive(%b)",
                JS_NAMESPACE, active);

        str = String.format("%s;%s.on('%s', %s)",
                str, JS_NAMESPACE, eventName, params);

        str = String.format("%s;%s.fireEvent('%s',%s);",
                str, JS_NAMESPACE, eventName, params);

        final String js = str;

        cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
    }

    private boolean isUpload(){
        return cordova.getActivity().getLocalClassName().contains("Upload");
    }

    private boolean isDownload(){
        return cordova.getActivity().getLocalClassName().contains("Download");
    }
}
