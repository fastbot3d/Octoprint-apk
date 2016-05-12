package android.app.printerapp.model;

import android.app.printerapp.Log;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.camera.CameraHandler;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.database.DeviceInfo;
import android.app.printerapp.octoprint.OctoprintConnection;
import android.app.printerapp.octoprint.StateUtils;
import android.content.Context;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ModelPrinter {

    //Id for database interaction
    private long mId;

    private int mPrinterType;
    private String mPrinterProfileId = null; //mAllModeProfile id

	//Service info
	private String mName;
	private String mDisplayName;
    private int mDisplayColor = 0;
	private String mAddress;
	private int mStatus = StateUtils.STATE_NONE;
    private String mPort;
    private String mNetwork;
    private String mWebcamAddress;
	
	//TODO hardcoded string
	private String mMessage = "Offline";
	//lkj private String mTemperature;
	//lkj private String mTempTarget;
	private ArrayList<String> mTemperature;
	private ArrayList<String> mTempTarget;
    private String mBedTemperature;
    private String mBedTempTarget;
	
	private ArrayList<File> mFileList;

	private HashMap<String, ModelProfile> mAllModeProfile;
	private String mAPIKey ;
	
	//Pending job
	private ModelJob mJob;
	private boolean mJobLoaded;
	
	//Job path in case it's a local file, else null
	private String mJobPath;
	
	//Camera
	private CameraHandler mCam;
	public Handler mUpdateUIHandler;
	
	//Position in grid
	private int mPosition;

    //TODO temporary profile handling
    private ArrayList<JSONObject> mSliceProfiles;
	
	public ModelPrinter(String name, String address, int type){
		
		mName = name;
		mDisplayName = name;
		mAddress = address;
		mJob = new ModelJob();
		mFileList = new ArrayList<File>();
        mSliceProfiles = new ArrayList<JSONObject>();
		mJobLoaded = true;

		mAllModeProfile = new HashMap<String, ModelProfile>();
		mAllModeProfile.clear();
		mAPIKey = null;
		
		//TODO: Load with db
		mJobPath = null;
		
		//Set new position according to the position in the DB, or the first available
		//if ((Integer.valueOf(position)==null)) mPosition = DevicesListController.searchAvailablePosition();
		//else mPosition = position;

        mPosition = DevicesListController.searchAvailablePosition();

        //TODO predefine network types

        switch (type){

            case StateUtils.STATE_ADHOC:
            case StateUtils.STATE_NEW: mStatus = type; break;

            default: mStatus = StateUtils.STATE_NONE; break;

        }

        mPrinterType = type;

		mTemperature = new ArrayList<String>(3);
		mTempTarget = new ArrayList<String>(3);

	}
	
	/*********
	 * Gets
	 *********/

	public ModelProfile getModeProfileFromName(String profileNameId) {
		if ( !mAllModeProfile.isEmpty() ){
			return mAllModeProfile.get(profileNameId);
		}
		return null;
	}

	public ArrayList<String> getProfileIDArray() {
		if ( !mAllModeProfile.isEmpty() ){
			ArrayList<String> ret = new ArrayList<String>();
			for (ModelProfile p : mAllModeProfile.values()) {
				Log.d("lkj-getProfileIDArray", "id:" + p.getId());
				ret.add(p.getId());
			}
			return ret;
		}
		return null;
	}

	public void addProfile(String profileNameId, ModelProfile profile) {
		if ( mAllModeProfile.get(profileNameId) == null )
			mAllModeProfile.put(profileNameId, profile);
	}
	public String getAPIKey(){	return mAPIKey; }
	public void   setAPIKey(String key){
		mAPIKey = key;
	}

	public void setStatus(int status){
		mStatus = status;
	}


	public String getName(){
		return mName;
	}
	
	public ModelJob getJob(){
		return mJob;
	}
	
	public String getAddress(){
		return mAddress;
	}

    public String getWebcamAddress() { return mWebcamAddress;    }
	
	public int getStatus(){
		return mStatus;
	}
	
	public String getMessage(){
		return mMessage;
	}
	
	public ArrayList<String> getTemperature(){
		return mTemperature;
	}
	
	public ArrayList<String> getTempTarget(){
		return mTempTarget;
	}

    public String getBedTemperature() {
        return mBedTemperature;
    }

    public String getBedTempTarget() {
        return mBedTempTarget;
    }
	
	public ArrayList<File> getFiles(){
		return mFileList;
	}

	public int getPosition(){
		return mPosition;
	}
	
	public String getDisplayName(){
		return mDisplayName;
	}
    public int getDisplayColor() { return mDisplayColor; }
	
	public boolean getLoaded(){
		return mJobLoaded;
	}

	public String getJobPath(){
		return mJobPath;
	}

    public ArrayList<JSONObject> getSliceProfiles() { return mSliceProfiles; }
	public void addSliceProfiles(JSONObject sliceProfile)
	{
		mSliceProfiles.add(sliceProfile);
	}

    public long getId() { return mId; }

    public String getPort() { return mPort; }

    public int getType() { return mPrinterType; }
    public String getProfile() { return mPrinterProfileId; }  //lkj get current connection profile
    public String getNetwork() { return mNetwork; }

	/**********
	 *  Sets
	 **********/
	
	public void updatePrinter(String message, int stateCode, JSONObject status){
						
		mStatus = stateCode;
		mMessage = message;
		
		
		if (status!=null){

			mJob.updateJob(status);
			
			try {
				//Avoid having empty temperatures
				JSONArray temperature = status.getJSONArray("temps");
				if (temperature.length()>0) {
					JSONObject  temp= temperature.getJSONObject(0);
					float actual_tool0_temp = Float.parseFloat(temp.getJSONObject("tool0").getString("actual"));
					mTemperature.add(0, String.format("%.2f", actual_tool0_temp));
					mTempTarget.add(0, temp.getJSONObject("tool0").getString("target"));

					if(temp.has("tool1")) {
						float actual_tool1_temp = Float.parseFloat(temp.getJSONObject("tool1").getString("actual"));
						mTemperature.add(1,String.format("%.2f", actual_tool1_temp));
						mTempTarget.add(1, temp.getJSONObject("tool1").getString("target"));
					} else {
						mTemperature.add(1,"off");
						mTempTarget.add(1, "off");
					}

					if(temp.has("tool2")) {
						float actual_tool2_temp = Float.parseFloat(temp.getJSONObject("tool2").getString("actual"));
						mTemperature.add(2,String.format("%.2f", actual_tool2_temp));
						mTempTarget.add(2, temp.getJSONObject("tool2").getString("target"));
					} else {
						mTemperature.add(2,"off");
						mTempTarget.add(2, "off");
					}

					if(temp.has("bed")) {
						float actual_bed_temp = Float.parseFloat(temp.getJSONObject("bed").getString("actual"));
						mBedTemperature = String.format("%.2f", actual_bed_temp);
						mBedTempTarget = temperature.getJSONObject(0).getJSONObject("bed").getString("target");
					} else {
						mBedTemperature = "0";
						mBedTempTarget = "0";
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void updateFiles(File m){
		mFileList.add(m);
	}
	
	public void startUpdate(final Context context){
		//Initialize web socket connection
		//OctoprintConnection.getNewConnection(context, this, false);
		Log.d("lkj-startUpdate", "open a new thread for connect");
		mStatus = StateUtils.STATE_NONE;
		final ModelPrinter printer = this;
	//	new Thread() {
	//		public void run() {
				OctoprintConnection.openSocket(printer, context);
	//		}}.start();


	}
	
	public void setConnecting(){
		mStatus = StateUtils.STATE_NONE;
	}
	
	/*public void setNotConfigured(){
		mStatus = StateUtils.STATE_ADHOC;
		mMessage = "Not configured";
	}*/
	
	/*public void setNotLinked(){
		mStatus = StateUtils.STATE_NEW;
		mMessage = "New";
	}*/
	
	/*public void setLinked(Context context){
		//mStatus = StateUtils.STATE_NONE;
		//mMessage = "";
		startUpdate(context);
		mCam = new CameraHandler(context,mAddress);
		
	}*/
	
	//Set video stream from the camera
/*	public void setVideoStream(Context context){
		mCam = new CameraHandler(context,mAddress);
	}*/
	
	//change position
	public void setPosition(int pos){

        if ((Integer.valueOf(pos)==null)) mPosition = DevicesListController.searchAvailablePosition();
		else mPosition = pos;

        DatabaseController.updateDB(DeviceInfo.FeedEntry.DEVICES_POSITION, getId(), String.valueOf(mPosition));
	}
	
	public void setDisplayName(String name){
		mDisplayName = name;
	}
    public void setDisplayColor(int color) { mDisplayColor = color; }
	
	public void setLoaded(boolean load){
		mJobLoaded = load;
	}
	
	public void setJobPath(String path){
		mJobPath = path;
	}

    public void setId(long id) { mId = id; }

    public void setPort(String port) { mPort = port; }

    public void setNetwork (String network) { mNetwork = network; }

    public void setTypeAndProfile(int type, String profile) { //lkj no use
        mPrinterType = type;
        mPrinterProfileId = profile;
    }

	public void setType(int type) {
		mPrinterType = type;
	}

	public void setProfile(String profile) {
		mPrinterProfileId = profile;
	}

    public void setWebcamAddress(String address){
        mWebcamAddress = address;
    }

}
