package android.app.printerapp.devices.camera;

import android.app.printerapp.Log;
import android.app.printerapp.R;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.URI;

/**
 * This class will handle the Camera connection.
 * The device camera will stream on MJPEG.
 * @author alberto-baeza
 *
 */
public class CameraHandler {

   private static final String STREAM_PORT = ":80/?action=stream";

   //UI reference to the video view
	private MjpegView mv = null;
    private Context mContext;
    private FrameLayout mRootView;
	
	//Boolean to check if the stream was already started
	public boolean isRunning = false;
	
   //sample public cam
   private String URL;
	
	public CameraHandler(final Context context, String address, FrameLayout rootView){

		mContext = context;
        mRootView = rootView;

		mv = (MjpegView) mRootView.findViewById(R.id.mv);

        //mv = new MjpegView(mContext);


        /**
         * This method handles the stream connection / reconnection.
         * We need to check if the stream alredy started to reconnect in case
         * of a bad initial conection (when we upgrade a service).
         * If it was running, we only need to restart the stream by setting
         * the source again.
         */
       /**/ mv.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d("lkj-CAMERA onClick", "clcik start, ioisRight=" + mv.getIOIsRight() + ",standby=" + mv.getStandby());
				if (!mv.getIOIsRight()){
					new DoRead().execute(URL);
				}

				if (mv.getStandby()){
					mv.setStandby(false);
				}
				Log.d("lkj-CAMERA onClick", "clcik end");
			}
		});

        //Create URL
//        URL = "http:/" + address.substring(0,address.lastIndexOf(':')) + STREAM_PORT;
		URL = "http://127.0.0.1:8080/?action=stream" ;  //lkj
      //  URL = address;

       //Read stream
        Log.i("CAMERA", "Executing " + URL);
	   
	}

    public void startVideo(){
		Log.d("lkj-CAMERA startVideo", "ioisRight=" + mv.getIOIsRight() + ",standby=" + mv.getStandby());
		if (!mv.getIOIsRight()){
			new DoRead().execute(URL);
		}

		if (mv.getStandby()) {
			mv.setStandby(false);
		}
		mv.setVisibility(View.VISIBLE);
    }

	public void stopVideo(){
		Log.d("lkj-CAMERA stopVideo", "ioisRight=" + mv.getIOIsRight() + ",standby=" + mv.getStandby());
		mv.setStandby(false);
		mv.setVisibility(View.INVISIBLE);
	}

	/**
	 * This class will send a http get request to the server's stream
	 * @author alberto-baeza
	 *
	 */
	    class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
	    	
	        protected MjpegInputStream doInBackground(String... url) {

	            HttpResponse res = null;
	            DefaultHttpClient httpclient = new DefaultHttpClient();
				HttpParams httpParams = httpclient.getParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, 1 * 700);
				httpclient.setParams(httpParams);
	            try {      	
	                res = httpclient.execute(new HttpGet(URI.create(url[0])));
	                
	                if(res.getStatusLine().getStatusCode()==401){
	                    return null;
	                }
					Log.d("lkj-CAMERA", "DoRead");
	                return new MjpegInputStream(res.getEntity().getContent());

	            } catch (Exception e) {
	                //Error connecting to camera
                  //lkj  e.printStackTrace();
					Log.d("lkj-CAMERA", "doInBackground execption " + e);
					isRunning = false;
	            }
				isRunning = false;
	            return null;
	        }

	        protected void onPostExecute(MjpegInputStream result) {

                if (result != null){
                    //Returns an input stream
                    mv.setSource(result);
                    //Display options
                    mv.setDisplayMode(MjpegView.SIZE_FULLSCREEN);
                    mv.showFps(false); //lkj

					mv.setIOIsRight(true);
					Log.d("lkj-CAMERA", "onPostExecute");
					//if (mRootView!=null)
					//	mRootView.findViewById(R.id.videocam_off_layout).setVisibility(View.GONE);
                } else {
					mv.setIOIsRight(false);
					isRunning = false;
                    if (mRootView!=null) {
						//mRootView.findViewById(R.id.videocam_off_layout).setVisibility(View.VISIBLE);
						//mRootView.findViewById(R.id.videocam_off_layout).bringToFront();
					}
                }
	        }
	    }
	    
	    public MjpegView getView(){
	    	return mv;
	    }
	}
