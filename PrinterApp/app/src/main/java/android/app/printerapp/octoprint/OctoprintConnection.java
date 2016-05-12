package android.app.printerapp.octoprint;

import android.app.Dialog;
import android.app.printerapp.ListContent;
import android.app.printerapp.Log;
import android.app.printerapp.MainActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.database.DeviceInfo;
import android.app.printerapp.devices.discovery.DiscoveryController;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.library.LibraryController;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.model.ModelProfile;
import android.app.printerapp.settings.EditPrinterDialog;
import android.app.printerapp.viewer.ViewerMainFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;





import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
//import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;

/**
 * Class for Connection handling with Octoprint's API. Since the API is still on developement
 * we need more than one method, and they need interaction between them, this may change.
 * @author alberto-baeza
 *
 */
public class OctoprintConnection {

    private static final int SOCKET_TIMEOUT = 10000;
    public static final String DEFAULT_PORT = "/dev/ttyUSB0";
    private static final String DEFAULT_PROFILE = "_default";
    private static final String API_DISABLED_MSG = "API disabled";
    private static final String API_INVALID_MSG = "Invalid API key";

	/**
	 * 
	 * Post parameters to handle connection. JSON for the new API is made 
	 * but never used.
	 * 
	 */
	public static void startConnection(String url, final Context context, String port, String profile, final ConnectionCallBack callback){
					
		JSONObject object = new JSONObject();
		StringEntity entity = null;
		try {
			object.put("command","connect");
            object.put("port",port);
            object.put("baudrate",115200);
            object.put("printerProfile", profile);
            object.put("save", true);
			object.put("autoconnect","false");
			entity = new StringEntity(object.toString(), "UTF-8");
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

        Log.i("lkj-startConnection", "Start connection on profile:" + profile);
		
		HttpClientHandler.post(context, url + HttpUtils.URL_CONNECTION,
                entity, "application/json", new JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {

                    }

                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        callback.onSuccess(statusCode, headers, response);
                        super.onSuccess(statusCode, headers, response);
                    }

                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        callback.onFailure(statusCode, headers, throwable.toString());
                    }

                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        callback.onFailure(statusCode, headers, responseString);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                        Log.i("lkj-startConnection", "Failure ");
                        callback.onFailure(statusCode, headers, "startConnection failed");
                    }

                });
		
		
		
	}
    public static void disconnect(Context context, String url){

        JSONObject object = new JSONObject();
        StringEntity entity = null;
        try {
            object.put("command","disconnect");
            entity = new StringEntity(object.toString(), "UTF-8");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpClientHandler.post(context, url + HttpUtils.URL_CONNECTION,
                entity, "application/json", new JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {
                    }
                });
    }

    public static void getLinkedConnection(final Context context, final ModelPrinter p){

        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_CONNECTION, null, new JsonHttpResponseHandler() {

            @Override
            public void onProgress(int bytesWritten, int totalSize) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                JSONObject options = null;
                JSONObject current = null;
                try {


                    options = response.getJSONObject("options");
                    current = response.getJSONObject("current");
                    p.setPort(current.getString("port"));

                    String status = current.getString("state");
                    if (!(status.contains("Closed") || status.contains("error"))) {
                        p.setProfile(current.getString("printerProfile"));
                    }
                    p.setStatus(StateUtils.OctoprintStatusToInt(status));

                    JSONArray printerProfiles = options.getJSONArray("printerProfiles");
                    for (int i = 0; i < printerProfiles.length(); i++) {
                        String id = printerProfiles.getJSONObject(i).getString("id");
                        String name = printerProfiles.getJSONObject(i).getString("name");
                        Log.i("lkj-getPrinterStatus", "add printer profile id:" + id + " name:" + name);
                        ModelProfile profile = new ModelProfile(id, name);
                        p.addProfile(id, profile);
                    }

                    //lkj convertType(p, current.getString("printerProfile"));

                    //retrieve settings
                    //getUpdatedSettings(p,current.getString("printerProfile"));
                    getSettings(p);



                    ModelProfile.reloadList(context);
                    ModelProfile.reloadQualityList(context);

                   // Intent intent = new Intent("notify");
                   // intent.putExtra("message", "Devices");
                  //  LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    Handler handler = DevicesListController.mPrinter.mUpdateUIHandler;
                    if (handler != null){
                        Bundle bundle = new Bundle();
                        bundle.putString("message", "Devices");
                        Message msg = handler.obtainMessage(1000);
                        msg.setData(bundle);
                        msg.sendToTarget();
                    }

                    Log.i("lkj-getLinkedConnection", "Printer already connected to " + p.getPort());
                    //p.startUpdate(context);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js){
                if (statusCode == 401 && js.toString().equals(API_DISABLED_MSG)) {
                    Log.i("lkj-getLinkedConnection", js.toString());
                } else {
                    OctoprintAuthentication.getAuth(context, p, false);
                }


            }
        });

    }
	
	/**
	 * Obtains the current state of the machine and issues new connection commands
	 * @param p printer
	 */
	public static void getNewConnection(final Context context, final ModelPrinter p){

        //Get progress dialog UI
        View configurePrinterDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_progress_content_horizontal, null);
        ((TextView) configurePrinterDialogView.findViewById (R.id.progress_dialog_text)).setText(R.string.devices_discovery_connect);


//        try{
            //Show progress dialog
            final MaterialDialog.Builder configurePrinterDialogBuilder = new MaterialDialog.Builder(context);
            configurePrinterDialogBuilder.title(R.string.devices_discovery_title)
                    .customView(configurePrinterDialogView, true)
                    .cancelable(true)
                    .negativeText(R.string.cancel)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            dialog.setOnDismissListener(null);
                            dialog.dismiss();


                        }
                    })
                    .autoDismiss(false);
            //Progress dialog to notify command events
            final Dialog progressDialog = configurePrinterDialogBuilder.build();
            progressDialog.show();
//        } catch (WindowManager.BadTokenException e){
//            e.printStackTrace();
//        }


        //Get connection status
        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_CONNECTION, null, new JsonHttpResponseHandler(){

			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONObject response) {
				super.onSuccess(statusCode, headers, response);


                //TODO Random crash
                try{
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                   else return;
                } catch (ArrayIndexOutOfBoundsException e){

                    e.printStackTrace();

                }catch (NullPointerException e){

                    e.printStackTrace();

                    return;

                }


                //Check for current status
                JSONObject current = null;

                try {
                    current = response.getJSONObject("current");

                        //if closed or error
                        if ((current.getString("state").contains("Closed"))
                                ||(current.getString("state").contains("Error"))
                                 || (current.getString("printerProfile").equals(DEFAULT_PROFILE))) {

                            //configure new printer
                            new EditPrinterDialog(context, p, response);

                        } else {


                            //already connected
                            if (p.getStatus() == StateUtils.STATE_NEW){
                                //load information
                                p.setPort(current.getString("port"));
                                //lkj convertType(p, current.getString("printerProfile"));
                                //getUpdatedSettings(p,current.getString("printerProfile"));
                                getSettings(p);
                                Log.i("getNewConnection", "Printer already connected to " + p.getPort());

                                String network = MainActivity.getCurrentNetwork(context);
                                p.setNetwork(network);

                                p.setId(DatabaseController.writeDb(p.getName(), p.getAddress(), String.valueOf(p.getPosition()), String.valueOf(p.getType()), network));

                                p.startUpdate(context);

                            }


                        }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
			}

			@Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js){
                Log.i("Connection", "Failure while connecting " + statusCode + " == " + js.toString());

                if (statusCode == 401 && js.toString().equals(API_DISABLED_MSG)){
                    showApiDisabledDialog(context);
                } else {
                    OctoprintAuthentication.getAuth(context, p, true);
                }
                progressDialog.dismiss();

			}

		});
		
		
		
	}

    public static void showApiDisabledDialog(final Context context){

        new MaterialDialog.Builder(context)
                .title(R.string.error)
                .content(R.string.connection_error_api_disabled)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        new DiscoveryController(context).optionAddPrinter();
                    }
                }).show();


    }

    private static void convertType(ModelPrinter p, String type){

        Log.i("Profile","Converting type " + type);

        if (type.equals(ModelProfile.WITBOX_PROFILE)) p.setTypeAndProfile(1, ModelProfile.WITBOX_PROFILE);
        else if (type.equals(ModelProfile.PRUSA_PROFILE)) p.setTypeAndProfile(2, ModelProfile.PRUSA_PROFILE);
        else if (p.getProfile() == null)  {

            Log.i("Profile","Setting type: " + type);

            p.setTypeAndProfile(3, type);
        } else if (!p.getProfile().equals("_default")){
            Log.i("Profile","Setting type default");

            p.setTypeAndProfile(3, type);

        } else  Log.i("Profile", "Basura " + p.getProfile());

        Log.i("Profile", "Get type " + p.getProfile());


    }

    /*************************************************
     * SETTINGS
     **************************************************/

    public static int convertColor(String color){

        if (color.equals("default")) return Color.TRANSPARENT;
        if (color.equals("red")) return Color.RED;
        if (color.equals("orange")) return Color.rgb(255,165,0);
        if (color.equals("yellow")) return Color.YELLOW;
        if (color.equals("green")) return Color.GREEN;
        if (color.equals("blue")) return Color.BLUE;
        if (color.equals("violet")) return Color.rgb(138,43,226);

        return Color.BLACK;

    }

    public static void getUpdatedSettings(final ModelPrinter p, String profile){

        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_PROFILES + "/" + profile, null, new JsonHttpResponseHandler() {


            @Override
            public void onProgress(int bytesWritten, int totalSize) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);


                Log.i("getUpdatedSettings", response.toString());

                try {
                    String name = response.getString("name");
                    String color = response.getString("color");

                    if (!name.equals("")) {

                        p.setDisplayName(name);
                        DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, p.getId(), name);
                    }

                    p.setDisplayColor(convertColor(color));


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
            }
        });



    }

    /**
     * Function to get the settings from the server
     * @param p
     */
    public static void getSettings(final ModelPrinter p){

        final String PREFIX = "http://";

        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_SETTINGS, null, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                try {
                    JSONObject appearance = response.getJSONObject("appearance");

                    Log.i("Connection", appearance.toString());

                    String newName = appearance.getString("name");
                    if (!newName.equals("")) {

                        p.setDisplayName(newName);
                        DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_DISPLAY, p.getId(), newName);
                    }

                    p.setDisplayColor(convertColor(appearance.getString("color")));

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                try {

                    JSONObject webcam = response.getJSONObject("webcam");
                    if (webcam.has("streamUrl")) {
                        if (webcam.getString("streamUrl").startsWith("/")) {
                            p.setWebcamAddress(PREFIX + p.getAddress() + webcam.getString("streamUrl"));
                        } else {
                            p.setWebcamAddress(webcam.getString("streamUrl"));
                        }
                    }

                    /*
                    JSONObject system = response.getJSONObject("system"); //lkj
                    JSONArray actions = system.getJSONArray("actions");
                    for (int i = 0; i < actions.length(); i++) {
                        String action = actions.getJSONObject(i).getString("action");
                        if (action.equals("streamon")){
                            String cmd = actions.getJSONObject(i).getString("command");
                            p.setWebcamCommand(cmd);
                            Log.i("lkj-getSettings", "webcam steam cmd:" + cmd);
                        }
                    }
                    */
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                Log.i("Connection", "Settings failure: " + js.toString());
                DatabaseController.handlePreference(DatabaseController.TAG_KEYS, PrintNetworkManager.getNetworkId(p.getAddress()), null, false);
                MainActivity.showDialog(js.toString());
            }
        });

    }

    /**
     * Function to set the settings to the server
     */
    public static void setSettings(final ModelPrinter p, String newName, final String newColor, Context context){

        JSONObject object = new JSONObject();
        JSONObject appearance = new JSONObject();
        StringEntity entity = null;
        try {
            appearance.put("name", newName);
            appearance.put("color", newColor);
            object.put("appearance",appearance);
            entity = new StringEntity(object.toString(), "UTF-8");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpClientHandler.post(context, p.getAddress() + HttpUtils.URL_SETTINGS,
                entity, "application/json", new JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);

                        //if (newColor!=null) p.setDisplayColor(convertColor(newColor));
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js){
                        Log.i("Connection", "Settings failure: " + js.toString());
                    }
                });

    }
	
	
	/**
	 * 
	 * Obtains the state of the machine that will be on the Connection API, that's why this is here.
	 * Works in conjunction with GET /api/connection on the NEW API.
	 * 
	 * New client implementation uses Websockets to receive status updates from the server
	 * so we only need to open a new connection and parse the payload.
	 */
	public static void openSocket(final ModelPrinter p, final Context context){
		
		p.setConnecting();

		Log.d("lkj-openSocket", "open 1");

		//Web socket URI
		final String wsuri = "ws://" + p.getAddress() + HttpUtils.URL_SOCKET;

		   try {
               Log.d("lkj-openSocket", "open wsuri:" + wsuri);

               if (DevicesListController.mConnection == null)
                    DevicesListController.mConnection = new WebSocketConnection();

               WebSocketOptions option = new WebSocketOptions();

               option.setMaxFramePayloadSize(548576); //0.5M
               option.setSocketConnectTimeout(8000000); //8s
               option.setSocketReceiveTimeout(8000000);
               option.setReconnectInterval(100);

               Log.d("lkj-openSocket", "open 3.1");

			   //mConnection is a new websocket connection
               DevicesListController.mConnection.connect(wsuri, new WebSocketConnectionHandler() {

                   //When the websocket opens
                   @Override
                   public void onOpen() {
                       //TODO unify this method
                       Log.d("lkj-openSocket", "Status: Connected to " + wsuri);
                       Log.d("lkj-openSocket", "Connection from: SOCKET");
                       doConnection(context, p);
                   }
/*
                   public void onRawTextMessage(byte[] var1) {
                       Log.d("lkj-openSocket", "onRawTextMessage:" + var1.toString());
                   }

                   public void onBinaryMessage(byte[] var1) {
                       Log.d("lkj-openSocket", "onBinaryMessage:" + var1.toString());
                   }*/

                   //On message received
                   @Override
                   public void onTextMessage(String payload) {

                       //Log.i("SOCK", "Got echo [" + p.getAddress() + "]: " + payload);
                       Handler handler = DevicesListController.mPrinter.mUpdateUIHandler;

                       try {
                           Log.d("lkj-onTextMessage", "payload:" + payload);

                           JSONObject object = new JSONObject(payload);

		            	//Get the json string for "current" status
		            	if (object.has("current")){

		            		JSONObject response = new JSONObject(payload).getJSONObject("current");

							//Update job with current status
			            	//We'll add every single parameter


                            //SEND NOTIFICATION
                            /*
                            Intent intent = new Intent("notify");
                            intent.putExtra("message", "Devices");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            */

                            if (handler != null){
                                Bundle bundle = new Bundle();
                                bundle.putString("message", "Devices");
                                Message msg = handler.obtainMessage(1000);
                                msg.setData(bundle);
                                if(! handler.hasMessages(1000)) {
                                    p.updatePrinter(response.getJSONObject("state").getString("text"), createStatus(response.getJSONObject("state").getJSONObject("flags")), response);
                                    handler.sendMessageDelayed(msg, 1500);
                                } else {
                                     //  handler.removeMessages(1000);
                                }
                            }


                            if (!response.getJSONObject("progress").getString("completion").equals("null")){

                                Double d = Double.parseDouble(response.getJSONObject("progress").getString("completion"));

                                if ((d>0) && (p.getStatus() == StateUtils.STATE_PRINTING)){

                                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                    if (sharedPref.getBoolean(context.getResources().getString(R.string.shared_preferences_print), true)){

                                        Intent intentN = new Intent();
                                        intentN.setAction("android.app.printerapp.NotificationReceiver");
                                        intentN.putExtra("printer", p.getId());
                                        intentN.putExtra("progress", d.intValue());
                                        intentN.putExtra("type","print");
                                        context.sendBroadcast(intentN);
                                    }
                                }
                            }
		            	}

                         //Check for events in the server
		            	if (object.has("event")){
                            JSONObject payloadType = null;
		            		JSONObject response = new JSONObject(payload).getJSONObject("event");
                            String eventPayload = response.getString("payload");
                            if (eventPayload != null && !eventPayload.equals("") && !eventPayload.equals("null") ){
                                payloadType = new JSONObject(eventPayload);
                            }
                            Log.d("lkj-onTextMessage", "response:" + response.toString());
                            Log.d("lkj-onTextMessage", "event type:" + response.getString("type"));

                            //Slicing finished should be handled in another method
		            		if (response.getString("type").equals("SlicingDone")){

		            			JSONObject slicingPayload = response.getJSONObject("payload");

                                sliceHandling(context, slicingPayload, p.getAddress());

                                ViewerMainFragment.showProgressBar(StateUtils.SLICER_HIDE, 0); //lkj
                            } else if (response.getString("type").equals("UpdatedFiles") ){// update file

                                if (payloadType != null && payloadType.getString("type").equals("gcode")) {
                                    OctoprintFiles.getFiles(context, p, null);
                                }
		            		} else if (response.getString("type").equals("Upload")){//A file was uploaded
                                if (payloadType != null ) {
                                    String fileName = payloadType.getString("file").toString();
                                    File m = new File("local/" + fileName);
                                    if (m != null)
                                        p.updateFiles(m);
                                }
		            			//p.setLoaded(true);
								/*lkj 
                                if (DatabaseController.getPreference(DatabaseController.TAG_SLICING, "Last")!=null)
                                    if ((DatabaseController.getPreference("Slicing","Last")).equals(response.getJSONObject("payload").getString("file"))){

                                        //Log.i("Slicer","LETS SLICE " + response.getJSONObject("payload").getString("file"));
                                        }
								*/
		            		} else if (response.getString("type").equals("PrintStarted")){
                                p.setLoaded(true);
                            } else if (response.getString("type").equals("Connected")){
                                p.setPort(response.getJSONObject("payload").getString("port"));
                                Log.i("lkj-onTextMessage", "UPDATED PORT " + p.getPort());
                            } else if (response.getString("type").equals("PrintDone")){
                                //SEND NOTIFICATION
                                Log.i("lkj-onTextMessage", "PRINT FINISHED! " + response.toString());

                                   if (p.getJobPath() != null)
                                       addToHistory(p, response.getJSONObject("payload"));

                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                if (sharedPref.getBoolean(context.getResources().getString(R.string.shared_preferences_print), true)) {

                                    Intent intentN = new Intent();
                                    intentN.setAction("android.app.printerapp.NotificationReceiver");
                                    intentN.putExtra("printer", p.getId());
                                    intentN.putExtra("progress", 100);
                                    intentN.putExtra("type", "finish");
                                    context.sendBroadcast(intentN);
                                }


                            } else if (response.getString("type").equals("SettingsUpdated")){
                                OctoprintSlicing.retrieveProfiles(context, p);
                                getLinkedConnection(context,p);
                               // Intent intent = new Intent("notify");
                               // intent.putExtra("message", "Profile");
                               // LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                                if (handler != null){
                                    Bundle bundle = new Bundle();
                                    bundle.putString("message", "Profile");
                                    Message msg = handler.obtainMessage(1000);
                                    msg.setData(bundle);
                                    msg.sendToTarget();
                                }
                            }
		            	}



                          //update slicing progress in the print panel fragment
                          if (object.has("slicingProgress")){

                              JSONObject response = new JSONObject(payload).getJSONObject("slicingProgress");

                              //TODO random crash because not yet created
                              try{
                                  //Check if it's our file
                                  //lkj if(!DatabaseController.getPreference(DatabaseController.TAG_SLICING,"Last").equals(null))
                                  //if (DatabaseController.getPreference(DatabaseController.TAG_SLICING,"Last").equals( response.getString("source_path"))){

                                      //Log.i("Slicer","Progress received for " + response.getString("source_path"));

                                      int progress = response.getInt("progress");


                                      //TODO
                                      ViewerMainFragment.showProgressBar(StateUtils.SLICER_SLICE, progress);

                                      //SEND NOTIFICATION

                                      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                      if (sharedPref.getBoolean(context.getResources().getString(R.string.shared_preferences_slice), true)) {

                                          Intent intent = new Intent();
                                          intent.setAction("android.app.printerapp.NotificationReceiver");
                                          intent.putExtra("printer", p.getId());
                                          intent.putExtra("progress", progress);
                                          intent.putExtra("type", "slice");
                                          context.sendBroadcast(intent);
                                      }
                                  //}
                              } catch (NullPointerException e){

                                  //e.printStackTrace();
                                  //Log.i("OUT","Null slicing");
                              }
                          }
		            } catch (JSONException e) {
						e.printStackTrace();
						Log.i("CONNECTION", "Invalid JSON");

					}
		         }

		         @Override
		         public void onClose(int code, String reason) {
		            Log.i("SOCK", "Connection lost at code: " + code + " because " + reason);

                     //  DevicesListController.mConnection.disconnect();

                       switch (code) {
                           case WebSocket.ConnectionHandler.CLOSE_SERVER_ERROR:
                           case WebSocket.ConnectionHandler.CLOSE_RECONNECT:
                           case WebSocket.ConnectionHandler.CLOSE_INTERNAL_ERROR:
                           case WebSocket.ConnectionHandler.CLOSE_PROTOCOL_ERROR:
                           case WebSocket.ConnectionHandler.CLOSE_CANNOT_CONNECT:
                           case WebSocket.ConnectionHandler.CLOSE_CONNECTION_LOST:
                               Log.i("lkj open SOCK", "old lost, will start new thread");
                              // new Thread(
                              //         new Runnable() {
                                      //     @Override
                                  //         public void run() {
                                        //       openSocket(p, context);
                                   //        }
                                //       }
                             //  ).start();
                               break;
                           case WebSocket.ConnectionHandler.CLOSE_NORMAL:
                               break;
                       }
                       //Timeout for reconnection
		            	/*lkj Handler handler = new Handler();
		            	handler.postDelayed(new Runnable() {

							@Override
							public void run() {
                                Log.i("OUT", "Timeout expired, reconnecting to " + p.getAddress());
								 p.startUpdate(context);
							}
						}, SOCKET_TIMEOUT);
                        */

		         }
		      }, option);
		   } catch (WebSocketException e) {
		      Log.i("WebSocketException:", e.toString());
		   } catch (Exception e) {
               Log.i("WebSocket  Exception:", e.toString());
           }

	}

    //TODO
    //Method to invoke connection handling
    public static void doConnection(Context context, ModelPrinter p){


        getLinkedConnection(context, p);

        //Get printer settings

        //getSettings(p);

        //Get a new set of files
        OctoprintFiles.getFiles(context, p, null);

        //Get a new set of profiles
        //OctoprintSlicing.retrieveProfiles(context,p); //Don't retrieve profiles yet
    }

    public static int createStatus(JSONObject flags){

				try {
					if (flags.getBoolean("paused")) return StateUtils.STATE_PAUSED;
					if (flags.getBoolean("printing")) return StateUtils.STATE_PRINTING;
					if (flags.getBoolean("operational")) return StateUtils.STATE_OPERATIONAL;
					if (flags.getBoolean("error")) return StateUtils.STATE_ERROR;
					if (flags.getBoolean("paused")) return StateUtils.STATE_PAUSED;
					if (flags.getBoolean("closedOrError")) return StateUtils.STATE_CLOSED;

				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				return StateUtils.STATE_NONE;
		
	}
	
	/**
	 * This method will create a dialog to handle the sliced file from the server.
	 * @param context
	 * @param payload sliced file data from the server
	 * @param url server address
	 */
	private static void sliceHandling(final Context context, final JSONObject payload, final String url){

		try {

            Log.i("Slicer", "Slice done received for " + payload.getString("stl"));

            //Search for files waiting for slice
            if (DatabaseController.getPreference(DatabaseController.TAG_SLICING,"Last")!=null){
             if (DatabaseController.getPreference(DatabaseController.TAG_SLICING,"Last").equals( payload.getString("stl"))) {
                 //lkj  {

                 //lkj Log.i("Slicer", "Changed PREFERENCE [Last]: " + payload.getString("gcode"));
                 //lkj DatabaseController.handlePreference(DatabaseController.TAG_SLICING,"Last",payload.getString("gcode"), true);

                 //lkj ViewerMainFragment.showProgressBar(StateUtils.SLICER_DOWNLOAD, 0);

                 //lkj  OctoprintSlicing.getMetadata(url, payload.getString("gcode"));
                 //lkj OctoprintFiles.downloadFile(context, url + HttpUtils.URL_DOWNLOAD_FILES,
                 //lkj			LibraryController.getParentFolder() + "/temp/", payload.getString("gcode"));
                 OctoprintFiles.deleteFile(context, url, payload.getString("stl"), "/local/");
             }
            }else {
             //   Log.i("Slicer", "Slicing NOPE for me!");
            }

		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

    public static void addToHistory(ModelPrinter p, JSONObject history){

        try {
            String name = history.getString("filename");
            String path = p.getJobPath();
            String time = ConvertSecondToHHMMString(history.getString("time"));
            String type = p.getProfile();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String date =  sdf.format(new Date());

            if (path!=null)
            if(!path.contains("/temp/")){

                LibraryController.addToHistory(new ListContent.DrawerListItem(type,name,time,date,path));
                DatabaseController.writeDBHistory(name, path, time, type, date);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }

    }

    //External method to convert seconds to HHmmss
    public static String ConvertSecondToHHMMString(String secondtTime) {
        String time = "--:--:--";

        if (secondtTime != null && !secondtTime.equals("null")) {

            int value = (int)Float.parseFloat(secondtTime);

            TimeZone tz = TimeZone.getTimeZone("UTC");
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.US);
            df.setTimeZone(tz);
            time = df.format(new Date(value * 1000L));
        }


        return time;

    }

    public interface ConnectionCallBack {
        public void onSuccess(int statusCode, Header[] headers, JSONObject response);
        public void onFailure(int statusCode, Header[] headers, String responseString);
    }


    public static void systemCmd(Context context, ModelPrinter p, String cmd){

        RequestParams setup_data = new RequestParams();
        setup_data.put("action", cmd);
        setup_data.put("async", true);
        setup_data.put("ignore", true);

        HttpClientHandler.post(p.getAddress() + HttpUtils.URL_SYSTEM_CMD,
                setup_data, new JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {

                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        Log.d("lkj-systemCmd", "cmd success");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                        Log.e("lkj-systemCmd", "cmd failed");
                    }
                });

    }

    /**
     * Function to set the settings to the server
     */
    public static void firstSetup(Context context, ModelPrinter p, String user, String passwd, final ConnectionCallBack callback){

        RequestParams setup_data = new RequestParams();
        setup_data.put("ac", true);
        setup_data.put("user", user);
        setup_data.put("pass1", passwd);
        setup_data.put("pass2", passwd);

         HttpClientHandler.post(p.getAddress() + HttpUtils.URL_ACCESS_SETUP,
                setup_data, new JsonHttpResponseHandler() {

                    //Override onProgress because it's faulty
                    @Override
                    public void onProgress(int bytesWritten, int totalSize) {

                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        callback.onSuccess(statusCode, headers, response);
                        Log.d("lkj-firstSetup", "set up user and passwd success");

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                        Log.e("lkj-firstSetup", "Settings failure: " + js.toString());
                    }
                });

    }

    public static void getPrinterStatus(final Context context, final ModelPrinter p, final ConnectionCallBack callback){
        Log.d("lkj-getPrinterStatus", "get status");


        HttpClientHandler.get(p.getAddress() + HttpUtils.URL_CONNECTION, null, new JsonHttpResponseHandler() {

            @Override
            public void onProgress(int bytesWritten, int totalSize) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

                JSONObject current = null;
                JSONObject options = null;
                try {
                    Log.i("lkj-getPrinterStatus", "response:" + response);
                    options = response.getJSONObject("options");
                    current = response.getJSONObject("current");
                    p.setPort(current.getString("port"));
                    String status = current.getString("state");
                    Log.i("lkj-getPrinterStatus", "status:" + status);
                    if (!(status.contains("Closed") || status.contains("error"))) {
                        p.setProfile(current.getString("printerProfile"));
                    }
                    p.setStatus(StateUtils.OctoprintStatusToInt(status));

                    JSONArray printerProfiles = options.getJSONArray("printerProfiles");
                    for (int i = 0; i < printerProfiles.length(); i++) {
                        String id = printerProfiles.getJSONObject(i).getString("id");
                        String name = printerProfiles.getJSONObject(i).getString("name");
                        Log.i("lkj-getPrinterStatus", "add printer profile id:" + id + " name:" + name);
                        ModelProfile profile = new ModelProfile(id, name);
                        p.addProfile(id, profile);
                    }

                    ModelProfile.reloadList(context);
                    ModelProfile.reloadQualityList(context);


                    Log.i("lkj-getPrinterStatus", "Printer status:" + p.getStatus());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                callback.onSuccess(statusCode, headers, response);
            }



            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                p.setStatus(StateUtils.STATE_NONE);
                callback.onFailure(statusCode, headers, throwable.toString());
            }

            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                p.setStatus(StateUtils.STATE_NONE);
                callback.onFailure(statusCode, headers, responseString);
            }


            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject js) {
                p.setStatus(StateUtils.STATE_NONE);
                callback.onFailure(statusCode, headers, throwable.toString());
            }
        });

    }






    public static void connect_printer(final Context context, final ModelPrinter p, final ConnectionCallBack callBack) {
        final ArrayList<String> profilesID = p.getProfileIDArray();

        if (StateUtils.octoprintIsClosed(p.getStatus())) {
            final MaterialDialog progressDialog =  new MaterialDialog.Builder(context)
                    .title(R.string.connect_progress_dialog_title)
                    .content(R.string.connect_progress_dialog_infomation)
                    .cancelable(false)
                    .progress(true, 0).build();

            final MaterialDialog dialog = new MaterialDialog.Builder(context)
                    .title(R.string.connect_printer_title)
                    .items(profilesID.toArray(new CharSequence[profilesID.size()]))
                    .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            int index = dialog.getSelectedIndex();
                            String select_profile_name = profilesID.get(index);
                            Log.d("lkj-connect_printer", "select:" + select_profile_name + " index:" + index);
                            return true; // allow selection
                        }
                    })
                    .alwaysCallSingleChoiceCallback()
                    .positiveText(R.string.connect_printer_button)
                    .autoDismiss(false)
                    .cancelable(false)
                    .show();

            final ConnectionCallBack local_callBack = new OctoprintConnection.ConnectionCallBack(){
                public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                    progressDialog.dismiss();
                    OctoprintSlicing.retrieveProfiles(context, p);
                    callBack.onSuccess(statusCode, headers, response);
                }

                public void onFailure(int statusCode, Header[] headers, String responseString){
                    progressDialog.dismiss();
                    Toast.makeText(context, "Fail to connect", Toast.LENGTH_LONG);
                    callBack.onFailure(statusCode,headers,responseString);
                }
            };

            View connect_printer_positiveAction;
            connect_printer_positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
            connect_printer_positiveAction.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int index = dialog.getSelectedIndex();
                            if(index <0 ){
                                return;
                            }
                            String select_profile_name = profilesID.get(index);
                            p.setProfile(select_profile_name);
                            OctoprintConnection.startConnection(p.getAddress(), context, "AUTO", select_profile_name, local_callBack);

                            progressDialog.show();
                            dialog.dismiss();
                            Log.d("lkj-connect_printer", "2 connect with id:" + select_profile_name);
                        }
                    }
            );

            Log.d("lkj-connect_printer", "is closed");
        } else if (StateUtils.octoprintIsOffline(p.getStatus())){
            Log.d("lkj-connect_printer", "is offline");
            callBack.onFailure(-1,null,null);
        } else {
            OctoprintSlicing.retrieveProfiles(context, p);
            callBack.onSuccess(0, null, null);
            Log.d("lkj-connect_printer", "is online");
        }
    }

}
