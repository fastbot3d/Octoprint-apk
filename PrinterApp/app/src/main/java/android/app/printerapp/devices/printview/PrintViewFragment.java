package android.app.printerapp.devices.printview;


import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.printerapp.Log;
import android.app.printerapp.MainActivity;
import android.app.printerapp.R;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.FinishDialog;
import android.app.printerapp.devices.camera.CameraHandler;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.DiscoveryController;
import android.app.printerapp.library.LibraryController;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.model.ModelProfile;
import android.app.printerapp.octoprint.HttpUtils;
import android.app.printerapp.octoprint.OctoprintConnection;
import android.app.printerapp.octoprint.OctoprintControl;
import android.app.printerapp.octoprint.OctoprintFiles;
import android.app.printerapp.octoprint.StateUtils;
import android.app.printerapp.util.ui.DisableFrameLayout;
import android.app.printerapp.util.ui.DisableLinearLayout;
import android.app.printerapp.viewer.DataStorage;
import android.app.printerapp.viewer.GcodeFile;
import android.app.printerapp.viewer.ViewerMainFragment;
import android.app.printerapp.viewer.ViewerSurfaceView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.material.widget.PaperButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.os.Handler;

/**
 * This class will show the PrintView detailed view for every printer
 * Should be able to control printer commands and show video feed.
 *
 * @author alberto-baeza
 */
public class PrintViewFragment extends Fragment {

    private static final String TAG = "PrintView";

    //Current Printer and status
    private static ModelPrinter mPrinter;
    private CameraHandler mCamera;
    private boolean isPrinting = false;
    private boolean isGcodeLoaded = false;

    //View references
    private TextView tv_printer;
    private TextView tv_file;
    private TextView tv_temp_ext1, tv_temp_ext2, tv_temp_ext3;
    private TextView tv_temp_bed;
    private TextView tv_prog;
    private TextView tv_profile;
    private TextView text_xy_speed, text_z_speed, text_e_speed, text_e_distance;

    private ProgressBar pb_prog;
    private SeekBar sb_head;

    private PaperButton button_pause;
    private PaperButton button_stop;
    private ImageView icon_pause;

    //File references
    private static DataStorage mDataGcode;
    private static ViewerSurfaceView mSurface;
    private SurfaceView mVideoSurface;
    private static FrameLayout mLayout;
    private static FrameLayout mLayoutVideo;

    private PaperButton mFanSpeedButton;
    private Spinner mFanIDSpinner, mExtruderSpinner;
    private SeekBar mFanSpeedSeekBar, mXYSpeedSeekBar, mZSpeedSeekBar, mESpeedSeekBar, mEDistanceSeekBar;
    private int mFanSpeed = 0, mXYSpeed = 0, mZSpeed = 0, mESpeed = 0, mEDistance;
    private int mFanID = 0;
    private LinearLayout extruder2_linerLayout, extruder3_linerLayout;
    DisableFrameLayout mDisableFrameLayoutMovement;
    DisableLinearLayout mDisableLinearLayoutExtruder;
    DisableLinearLayout mDisableLinearLayoutFilamentChange;

    private View mRootView;
    TabHost mTabHost;

    //Context needed for file loading
    private static Context mContext;
    private static int mActualProgress = 0;

    private Dialog mDownloadDialog;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retain instance to keep the Fragment from destroying itself
        setRetainInstance(true);

        Log.d("lkj printView framement", "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Reference to View
        mRootView = null;

        //If is not new
        if (savedInstanceState == null) {

            //Necessary for gcode tracking
            mContext = getActivity();

            //Show custom option menu
            setHasOptionsMenu(true);

            //Get the printer from the list
         //   Bundle args = getArguments();
        //    mPrinter = DevicesListController.getPrinter(args.getLong("id"));
          mPrinter = DevicesListController.mPrinter;

            //getActivity().getActionBar().setTitle(mPrinter.getAddress().replace("/", ""));

            if (mPrinter==null) {
                getActivity().onBackPressed();
            }else {

                try { //TODO CRASH
                    //Check printing status
                    if ((mPrinter.getStatus() == StateUtils.STATE_PRINTING)||
                            (mPrinter.getStatus() == StateUtils.STATE_PAUSED)) isPrinting = true;
                    else {
                        mActualProgress = 100;
                        isPrinting = false;
                    }
                } catch (NullPointerException e){
                    getActivity().onBackPressed();
                }

                //Update the actionbar to show the up carat/affordance
                if (DatabaseController.count()>1){
                    ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }

                //Inflate the fragment
                mRootView = inflater.inflate(R.layout.printview_layout, container, false);

                /************************************************************************/

                //Get video
                mLayoutVideo = (FrameLayout) mRootView.findViewById(R.id.printview_video);

                mDisableFrameLayoutMovement = (DisableFrameLayout) mRootView.findViewById(R.id.view_gcode);
                mDisableLinearLayoutExtruder = (DisableLinearLayout) mRootView.findViewById(R.id.disableLinearLayout_extruder);
                mDisableLinearLayoutFilamentChange = (DisableLinearLayout) mRootView.findViewById(R.id.disableLinearLayout_Filament_change);


                //TODO CAMERA DISABLED
                mCamera = new CameraHandler(mContext, mPrinter.getWebcamAddress(), mLayoutVideo);

                mVideoSurface = mCamera.getView();

                //lkj  mLayoutVideo.addView(mVideoSurface);

                mCamera.startVideo();

                //Get tabHost from the xml
                mTabHost = (TabHost) mRootView.findViewById(R.id.printviews_tabhost);
                mTabHost.setup();

                //Create 3D RENDER tab
                TabHost.TabSpec featuresTab = mTabHost.newTabSpec("3D Render");
           //lkj     featuresTab.setIndicator(getTabIndicator(mContext.getResources().getString(R.string.printview_3d_text), R.drawable.visual_normal_24dp));
                featuresTab.setIndicator(mContext.getResources().getString(R.string.printview_3d_text));
                featuresTab.setContent(R.id.view_gcode);
                mTabHost.addTab(featuresTab);

                //Create VIDEO tab
                TabHost.TabSpec settingsTab = mTabHost.newTabSpec("Video");
                //lkj settingsTab.setIndicator(getTabIndicator(mContext.getResources().getString(R.string.printview_video_text), R.drawable.ic_videocam));
                settingsTab.setIndicator(mContext.getResources().getString(R.string.printview_video_text));
                settingsTab.setContent(R.id.printview_video);
                mTabHost.addTab(settingsTab);



                TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
                tv.setTextSize(16);
                tv.setTextColor(getResources().getColor(R.color.black));

                tv = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title);
                tv.setTextSize(16);
                tv.setTextColor(getResources().getColor(R.color.black));

                mTabHost.getTabWidget().setDividerDrawable(new ColorDrawable(getResources().getColor(R.color.black)));


                mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                    @Override
                    public void onTabChanged(String s) {


                        if (s.equals("Video")) {
                            //lkj if (mSurface!=null)
                            //lkj  mLayout.removeAllViews();

                            mCamera.startVideo();

                            mVideoSurface.invalidate();
                            mLayoutVideo.invalidate();
                        } else {    //Redraw the gcode
                            Log.d("lkj printers-onTabChanged", "old gcode viewer pannel");
                            /*lkj
                            if (!isGcodeLoaded){

                                //Show gcode tracking if there's a current path in the printer/preferences
                                if (mPrinter.getJob().getFilename()!=null){
                                    retrieveGcode();
                                }
                            } else {
                                if (mSurface!=null){
                                    drawPrintView();
                                }
                            }
                            */
                            mCamera.stopVideo();
                            // mCamera.getView().setVisibility(View.INVISIBLE);
                          //  mCamera.getView().stopPlayback();
                        }


                        //TODO CAMERA DISABLED
                        // mLayoutVideo.invalidate();



                    }
                });

                /***************************************************************************/


                initUiElements();
                refreshData();

                //Register receiver
            //lkj    mContext.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

    }


    //Refresh printers when the fragmetn is shown
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);


        if(!hidden){
            String curTab = mTabHost.getCurrentTabTag();
            Log.d("lkj printViewFragment", "onHiddenChanged curTab=" + curTab);

            if (curTab.contains("Video")){

                mCamera.startVideo();
                mVideoSurface.invalidate();
                mLayoutVideo.invalidate();
            }
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.printview_menu, menu);
    }

    //Switch menu options if it's printing/paused
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    //Option menu
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if (DatabaseController.count()>1) getActivity().onBackPressed();
                return true;

            case R.id.printview_add:
                new DiscoveryController(getActivity()).scanDelayDialog();
                return true;

            case R.id.printview_open_camera:  //lkj
                //getActivity().onBackPressed();
                OctoprintConnection.systemCmd(mContext,mPrinter,"streamon");
                return true;
            case R.id.printview_shutdown:  //lkj
                final MaterialDialog dialog =  new MaterialDialog.Builder(mContext)
                        .title("Warning")
                        .content("Are you sure you want to shut down!")
                        .negativeText("Cancel")
                        .positiveText("OK")
                        .show();
                dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                OctoprintConnection.systemCmd(mContext, mPrinter, "shutdown");
                                Log.d("lkj-shutdown", "shutdown");
                            }
                        }
                );

                return true;
            case R.id.printview_settings:
                //getActivity().onBackPressed();
                MainActivity.showExtraFragment(0, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Initialize all UI elements
    private void initUiElements(){

        //UI references
        tv_printer = (TextView) mRootView.findViewById(R.id.printview_printer_tag);
        tv_file = (TextView) mRootView.findViewById(R.id.printview_printer_file);
        tv_temp_ext1 = (TextView) mRootView.findViewById(R.id.printview_extruder1_temp);
        tv_temp_ext2 = (TextView) mRootView.findViewById(R.id.printview_extruder2_temp);
        tv_temp_ext3 = (TextView) mRootView.findViewById(R.id.printview_extruder3_temp);
        tv_temp_bed = (TextView) mRootView.findViewById(R.id.printview_bed_temp);
        tv_prog = (TextView) mRootView.findViewById(R.id.printview_printer_progress);
        tv_profile = (TextView) mRootView.findViewById(R.id.printview_text_profile_text);
        pb_prog = (ProgressBar) mRootView.findViewById(R.id.printview_progress_bar);

        extruder2_linerLayout = (LinearLayout) mRootView.findViewById(R.id.ll_extruder2);
        extruder3_linerLayout = (LinearLayout) mRootView.findViewById(R.id.ll_extruder3);

        button_pause = (PaperButton) mRootView.findViewById(R.id.printview_pause_button);
        icon_pause = (ImageView) mRootView.findViewById(R.id.printview_pause_image);


        text_xy_speed = (TextView) mRootView.findViewById(R.id.text_xy_speed);
        text_z_speed = (TextView) mRootView.findViewById(R.id.text_z_speed);

        text_e_speed= (TextView) mRootView.findViewById(R.id.txtview_extruder_speed);
        text_e_distance= (TextView) mRootView.findViewById(R.id.txtview_extruder_distance);

        button_stop = (PaperButton) mRootView.findViewById(R.id.printview_stop_button);

        mFanSpeedButton = (PaperButton) mRootView.findViewById(R.id.btn_fan_speed);
        mFanSpeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "fastbot", "FanID", mFanID, mFanSpeed);
            }
        });

        mFanIDSpinner = (Spinner) mRootView.findViewById(R.id.fan_spinner);
        ArrayList<String> fanIdArray = new ArrayList<String>(6);
        fanIdArray.add("1");fanIdArray.add("2");fanIdArray.add("3");fanIdArray.add("4");
        fanIdArray.add("5");fanIdArray.add("6");
        ArrayAdapter fanIDAdapter = new ArrayAdapter<String>(mContext,
                R.layout.print_panel_spinner_item, fanIdArray);
        fanIDAdapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item);
        mFanIDSpinner.setAdapter(fanIDAdapter);
        if (fanIDAdapter!=null){
            fanIDAdapter.notifyDataSetChanged();
            mFanIDSpinner.postInvalidate();
        }
        mFanIDSpinner.setSelection(0);

        mFanIDSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String id = mFanIDSpinner.getSelectedItem().toString();
                mFanID = Integer.parseInt(id);
                Log.d("lkj fan id", "id=" + mFanID + ",select id=" + id + ",i=" + i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        mFanSpeedSeekBar=(SeekBar)mRootView.findViewById(R.id.seekbar_fan_speed);
        mFanSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                                        @Override
                                                        public void onStopTrackingTouch(SeekBar seekBar) {
                                                        }

                                                        @Override
                                                        public void onStartTrackingTouch(SeekBar seekBar) {
                                                        }

                                                        @Override
                                                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                                            Log.d("lkj fan onProgressChanged", "progress=" + progress);
                                                            mFanSpeed = progress;
                                                            mFanSpeedButton.setText(String.format(getResources().getString(R.string.printview_fan_button), progress));
                                                        }
                                                    }
        );
        mFanSpeedSeekBar.setProgress(2);


        mXYSpeedSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_xy_speed);

        mXYSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("lkj mXYSpeedSeekBar onProgressChanged", "progress=" + progress);
                mXYSpeed = progress;
                text_xy_speed.setText(String.format(getResources().getString(R.string.printview_xy_speed_text), progress));
            }
        });
        mXYSpeedSeekBar.setProgress(100);

        mZSpeedSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_z_speed);
        mZSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("lkj mXYSpeedSeekBar onProgressChanged", "progress=" + progress);
                mZSpeed = progress;
                text_z_speed.setText(String.format(getResources().getString(R.string.printview_z_speed_text), progress));
            }
        });
        mZSpeedSeekBar.setProgress(5);



        mExtruderSpinner = (Spinner) mRootView.findViewById(R.id.extruder_spinner);
        ArrayList<String> extIdArray = new ArrayList<String>(6);
        extIdArray.add("Extruder1");extIdArray.add("Extruder2");extIdArray.add("Extruder3");
        ArrayAdapter extAdapter = new ArrayAdapter<String>(mContext,
                R.layout.print_panel_spinner_item, extIdArray);
        extAdapter.setDropDownViewResource(R.layout.print_panel_spinner_dropdown_item);
        mExtruderSpinner.setAdapter(extAdapter);
        if (extAdapter!=null){
            extAdapter.notifyDataSetChanged();
            mExtruderSpinner.postInvalidate();
        }
        mExtruderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String id = mExtruderSpinner.getSelectedItem().toString();
                String toolNum = "tool0";
                if (id.contains("Extruder2")) {
                    toolNum = "tool1";
                } else if (id.contains("Extruder3")) {
                    toolNum = "tool2";
                }

                Log.d("lkj extruder id", "id=" + toolNum);
                OctoprintControl.sendSelectToolCommand(getActivity(), mPrinter.getAddress(), "select", toolNum);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        mExtruderSpinner.setSelection(0);

        mESpeedSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_extruder_speed);
        mESpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("lkj mESpeedSeekBar onProgressChanged", "progress=" + progress);
                mESpeed = progress;
                text_e_speed.setText(String.format(getResources().getString(R.string.printview_e_speed_text), progress));
            }
        });
        mESpeedSeekBar.setProgress(5);


        mEDistanceSeekBar = (SeekBar) mRootView.findViewById(R.id.seekbar_extruder_distance);
        mEDistanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("lkj mESpeedSeekBar onProgressChanged", "progress=" + progress);
                mEDistance = progress;
                text_e_distance.setText(String.format(getResources().getString(R.string.printview_distance_text), progress));
            }
        });
        mEDistanceSeekBar.setProgress(5);




        icon_pause.setColorFilter(mContext.getResources().getColor(R.color.body_text_2),PorterDuff.Mode.MULTIPLY);
            button_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View view){

                if (!isPrinting) {

                    if ((!mPrinter.getJob().getProgress().equals("null")) && (mPrinter.getJob().getFinished())) {

                        new FinishDialog(mContext, mPrinter);

                    } else {
                        OctoprintControl.sendCommand(getActivity(), mPrinter.getAddress(), "start");
                    }
                } else OctoprintControl.sendCommand(getActivity(), mPrinter.getAddress(), "pause");

            }
            }

            );

//        ((ImageView) mRootView.findViewById(R.id.printview_stop_image)).
//                setColorFilter(mContext.getResources().getColor(android.R.color.holo_red_dark),
//                        PorterDuff.Mode.MULTIPLY);
            button_stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View view){
                OctoprintControl.sendCommand(getActivity(), mPrinter.getAddress(), "cancel");
            }
            }

            );


            sb_head=(SeekBar)mRootView.findViewById(R.id.seekbar_head_movement_amount);
            sb_head.setProgress(2);


            mRootView.findViewById(R.id.button_xy_down). setOnClickListener(new View.OnClickListener() {
                                                                                @Override
                                                                                public void onClick(View v) {
                     OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "y", -convertProgress(sb_head.getProgress()), mXYSpeed);
                                                                                }
                                                                            }
            );

            mRootView.findViewById(R.id.button_xy_up).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "y", convertProgress(sb_head.getProgress()), mXYSpeed);
                }
            });

            mRootView.findViewById(R.id.button_xy_left).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "x", -convertProgress(sb_head.getProgress()), mXYSpeed);
                                   }
                               }

            );

            mRootView.findViewById(R.id.button_xy_right).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "x", convertProgress(sb_head.getProgress()), mXYSpeed);
                                   }
                               }

            );

            mRootView.findViewById(R.id.button_z_down).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "z", -convertProgress(sb_head.getProgress()), mZSpeed);
                                   }
                               }

            );

            mRootView.findViewById(R.id.button_z_up).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "jog", "z", convertProgress(sb_head.getProgress()), mZSpeed);
                                   }
                               }

            );

            mRootView.findViewById(R.id.button_z_home).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "home", "z", 0, 0);
                                   }
                               }

            );

            mRootView.findViewById(R.id.button_xy_home).
            setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       OctoprintControl.sendHeadCommand(getActivity(), mPrinter.getAddress(), "home", "xy", 0, 0);
                                   }
                               }

            );

            /**
             * Temperatures
             */
        //extruder 1
        final SeekBar extruder1SeekBar = (SeekBar) mRootView.findViewById(R.id.printview_extruder1_temp_slider);
        final PaperButton extruder1Button = (PaperButton) mRootView.findViewById(R.id.printview_extruder1_temp_button);
        extruder1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "target", "tool0", extruder1SeekBar.getProgress(), 0);
            }
        });
        extruder1SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                extruder1Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        extruder1SeekBar.setProgress(0);
        extruder1Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), 0));

        //extruder 2
        final SeekBar extruder2SeekBar = (SeekBar) mRootView.findViewById(R.id.printview_extruder2_temp_slider);
        final PaperButton extruder2Button = (PaperButton) mRootView.findViewById(R.id.printview_extruder2_temp_button);
        extruder2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "target", "tool1", extruder2SeekBar.getProgress(), 0);
            }
        });
        extruder2SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                extruder2Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        extruder2SeekBar.setProgress(0);
        extruder2Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), 0));


        //extruder 3
        final SeekBar extruder3SeekBar = (SeekBar) mRootView.findViewById(R.id.printview_extruder3_temp_slider);
        final PaperButton extruder3Button = (PaperButton) mRootView.findViewById(R.id.printview_extruder3_temp_button);
        extruder3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "target", "tool2", extruder3SeekBar.getProgress(), 0);
            }
        });
        extruder3SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                extruder3Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        extruder3SeekBar.setProgress(0);
        extruder3Button.setText(String.format(getResources().getString(R.string.printview_change_temp_button), 0));



        final PaperButton motorONButton = (PaperButton) mRootView.findViewById(R.id.printview_motors_on);
        final PaperButton motorOFFButtn = (PaperButton) mRootView.findViewById(R.id.printview_motors_off);
        motorONButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v){
                OctoprintControl.sendGcodeCommand(getActivity(), mPrinter.getAddress(), "M80");
            }
        });
        motorOFFButtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OctoprintControl.sendGcodeCommand(getActivity(), mPrinter.getAddress(), "M18");
            }
        });

        final SeekBar bedSeekBar = (SeekBar) mRootView.findViewById(R.id.printview_bed_temp_slider);
            final PaperButton bedButton = (PaperButton) mRootView.findViewById(R.id.printview_bed_temp_button);
            bedButton.setText(String.format(getResources().getString(R.string.printview_change_temp_button), 0));

            bedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()  {
                @Override
                public void onProgressChanged (SeekBar seekBar,int i, boolean b){
                    bedButton.setText(String.format(getResources().getString(R.string.printview_change_temp_button), i));
                }

                @Override
                public void onStartTrackingTouch (SeekBar seekBar){
                }

                @Override
                public void onStopTrackingTouch (SeekBar seekBar){
                }
            });

            bedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick (View v){
                    OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "target", "bed", bedSeekBar.getProgress(), 0);
                }
            });

        /*  Extruder    */
          //lkj  final EditText et_am = (EditText) mRootView.findViewById(R.id.et_amount);

            mRootView.findViewById(R.id.printview_retract_button).setOnClickListener(new View.OnClickListener() {
                                                                                         @Override
                                                                                         public void onClick(View view) {
                    OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "extrude", null, -mEDistance, mESpeed);
                                                                                         }
                                                                                     }
            );

            mRootView.findViewById(R.id.printview_etrude_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    OctoprintControl.sendToolCommand(getActivity(), mPrinter.getAddress(), "extrude", null, mEDistance, mESpeed);
                }
            });

        mRootView.findViewById(R.id.printview_filament_unload_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OctoprintControl.sendGcodeCommand(getActivity(), mPrinter.getAddress(), "M602 S2");
            }
        });

        mRootView.findViewById(R.id.printview_filament_load_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OctoprintControl.sendGcodeCommand(getActivity(), mPrinter.getAddress(), "M602 S1");
            }
        });

        mRootView.findViewById(R.id.printview_filament_pause_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OctoprintControl.sendGcodeCommand(getActivity(), mPrinter.getAddress(), "M602 S3");
            }
        });
    }

    private double convertProgress(int amount){

        double finalAmount = 0.1 * Math.pow(10,Math.abs(amount));

        return finalAmount;

    }

    /**
     * Return the custom view of the print view tab
     *
     * @param title Title of the tab
     * @param icon  Icon of the tab
     * @return Custom view of a tab layout
     */
    private View getTabIndicator(String title, int icon) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.printview_tab_layout, null);
        ImageView iv = (ImageView) view.findViewById(R.id.tab_icon_imageview);
        iv.setImageResource(icon);
        iv.setColorFilter(mContext.getResources().getColor(R.color.body_text_1),
                PorterDuff.Mode.MULTIPLY);
        TextView tv = (TextView) view.findViewById(R.id.tab_title_textview);
        tv.setText(title);
        return view;
    }

    /**
     * Convert progress string to percentage
     *
     * @param p progress string
     * @return converted value
     */
    public String getProgress(String p) {

        double value = 0;

        try {
            value = Double.valueOf(p);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return String.valueOf((int) value);
    }

    /**
     * Dinamically update progress bar and text from the main activity
     */
    public void refreshData() {
        //Check around here if files were changed
        if (mPrinter == null) {
            return ;
        }
        if (!this.isAdded()){
            Log.d("lkj-refresh data", "fragement is not added ");
            return;
        }
     //   Log.d("lkj-refresh data", "getDisplayName=" + mPrinter.getDisplayName());
    //    Log.d("lkj-refresh data", "getMessage=" + mPrinter.getMessage());
   //     Log.d("lkj-refresh data", "getPort=" + mPrinter.getPort());
        //lkj tv_printer.setText(mPrinter.getDisplayName() + ": " + mPrinter.getMessage() + " [" + mPrinter.getPort() + "]");
        tv_printer.setText("BBP: " + mPrinter.getMessage() + " [" + mPrinter.getPort() + "]");

        String jobName = mPrinter.getJob().getFilename();
        Log.d("lkj-refresh data", "jobName=" + jobName);
        if (jobName != null && !jobName.startsWith("null")) {
            tv_file.setText(mPrinter.getJob().getFilename());
        } else {
            tv_file.setText("no file");
        }
        if (mPrinter.getTemperature().size()>0) {
            tv_temp_ext1.setText(mPrinter.getTemperature().get(0) + "ºC / " + mPrinter.getTempTarget().get(0) + "ºC");
            tv_temp_bed.setText(mPrinter.getBedTemperature() + "ºC / " + mPrinter.getBedTempTarget() + "ºC");

            String tmpExt2 = mPrinter.getTemperature().get(1);
            if (tmpExt2.contains("off")) {
                extruder2_linerLayout.setVisibility(View.GONE);
            } else {
                if (extruder2_linerLayout.getVisibility() == View.GONE) {
                    extruder2_linerLayout.setVisibility(View.VISIBLE);
                }
                tv_temp_ext2.setText(mPrinter.getTemperature().get(1) + "ºC / " + mPrinter.getTempTarget().get(1) + "ºC");
            }

            String tmpExt3 = mPrinter.getTemperature().get(2);
            if (tmpExt3.contains("off")) {
                extruder3_linerLayout.setVisibility(View.GONE);
            } else {
                if (extruder3_linerLayout.getVisibility() == View.GONE) {
                    extruder3_linerLayout.setVisibility(View.VISIBLE);
                }
                tv_temp_ext3.setText(mPrinter.getTemperature().get(2) + "ºC / " + mPrinter.getTempTarget().get(2) + "ºC");
            }
        }

        tv_profile.setText(" " + mPrinter.getProfile());

        CardView printer_select_layout = (CardView) mRootView.findViewById(R.id.printer_select_card_view);

        if ((mPrinter.getStatus() == StateUtils.STATE_PRINTING) ||
                (mPrinter.getStatus() == StateUtils.STATE_PAUSED)) {

            isPrinting = true;

            if (mPrinter.getStatus() == StateUtils.STATE_PRINTING){
                button_pause.setText(getString(R.string.printview_pause_button));
                icon_pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));

            } else {
                button_pause.setText(getString(R.string.printview_start_button));
                icon_pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                mDisableLinearLayoutFilamentChange.disable_touch(false);
            }


            tv_prog.setText(getProgress(mPrinter.getJob().getProgress()) + "% (" + OctoprintConnection.ConvertSecondToHHMMString(mPrinter.getJob().getPrintTimeLeft()) +
                    " left / " + OctoprintConnection.ConvertSecondToHHMMString(mPrinter.getJob().getPrintTime()) + " elapsed) - ");

            if (!mPrinter.getJob().getProgress().equals("null")) {
                Double n = Double.valueOf(mPrinter.getJob().getProgress());
                pb_prog.setProgress(n.intValue());
            }

            if (button_stop.getVisibility() == View.INVISIBLE ){
                button_stop.setVisibility(View.VISIBLE);
            }

            if (mDataGcode != null)
                changeProgress(Double.valueOf(mPrinter.getJob().getProgress()));

        } else {
            if (!mPrinter.getLoaded())
                tv_file.setText(R.string.devices_upload_waiting);



            if ((!mPrinter.getJob().getProgress().equals("null")) && (mPrinter.getJob().getFinished()))
            {
                pb_prog.setProgress(100);
                tv_file.setText(R.string.devices_text_completed);
                button_pause.setText(getString(R.string.printview_finish_button));
                icon_pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_done));

               //lkj mRootView.findViewById(R.id.stop_button_container).setVisibility(View.INVISIBLE);
                button_stop.setVisibility(View.INVISIBLE);
            } else {
                tv_prog.setText(mPrinter.getMessage() + " - ");
                button_pause.setText(getString(R.string.printview_start_button));
                icon_pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));

                //lkj mRootView.findViewById(R.id.stop_button_container).setVisibility(View.VISIBLE);
                button_stop.setVisibility(View.VISIBLE);

                pb_prog.setProgress(0);
            }

            isPrinting = false;
        }

       if ((mPrinter.getStatus() == StateUtils.STATE_NONE) ||(mPrinter.getStatus() == StateUtils.STATE_CLOSED)) {

            tv_file.setVisibility(View.INVISIBLE);
            tv_prog.setVisibility(View.INVISIBLE);
            tv_profile.setVisibility(View.INVISIBLE);
            sb_head.setProgress(2);
            mRootView.findViewById(R.id.printview_text_profile_tag).setVisibility(View.INVISIBLE);

           mDisableFrameLayoutMovement.disable_touch(true);
           mDisableLinearLayoutExtruder.disable_touch(true);
           mDisableLinearLayoutFilamentChange.disable_touch(true);
        //lkj    ViewHelper.disableEnableAllViews(false, printer_select_layout);

        } else {
            //mRootView.findViewById(R.id.disabled_gray_tint).setVisibility(View.VISIBLE);
            tv_file.setVisibility(View.VISIBLE);
            tv_prog.setVisibility(View.VISIBLE);
            tv_profile.setVisibility(View.VISIBLE);
            //lkj sb_head.setProgress(2);

            mRootView.findViewById(R.id.printview_text_profile_tag).setVisibility(View.VISIBLE);

           mDisableFrameLayoutMovement.disable_touch(false);
           mDisableLinearLayoutExtruder.disable_touch(false);
           mDisableLinearLayoutFilamentChange.disable_touch(false);
           //lkj ViewHelper.disableEnableAllViews(true, printer_select_layout);
        }

        if ((mPrinter.getStatus() == StateUtils.STATE_PRINTING)){
            mDisableFrameLayoutMovement.disable_touch(true);
            mDisableLinearLayoutExtruder.disable_touch(true);
            mDisableLinearLayoutFilamentChange.disable_touch(true);
        }

        Log.d("lkj-refresh data", "refresh data");
       //lkj getActivity().invalidateOptionsMenu();

    }

    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
         //   if (msg.what == 1) {
                // 动态更新UI界面
               // String str = msg.getData().getInt("num") + "";
                Log.d("lkj-handler", "refresh data");
            refreshData();
            //  }
        };
    };




    public void stopCameraPlayback() {
        //TODO CAMERA DEISABLE
     //   mCamera.getView().stopPlayback();
        mCamera.stopVideo();
       // mCamera.getView().setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {

        //mContext.unregisterReceiver(onComplete);
        super.onDestroy();
    }

    /**
     * ****************************************************************************************
     * <p/>
     * PRINT VIEW PROGRESS HANDLER
     *
     *****************************************************************************************
     */

    /**
     * Method to check if we own the gcode loaded in the printer to display it or we have to download it.
     */
    public void retrieveGcode() {


        //If we have a jobpath, we've uploaded the file ourselves
        if (mPrinter.getJobPath() != null) {

            Log.i(TAG, "PATH IS " + mPrinter.getJobPath());

            //Get filename
            File currentFile = new File(mPrinter.getJobPath());

            if (currentFile.exists())
                //if it's the same as the server or it's in process of being uploaded
                if ((mPrinter.getJob().getFilename().equals(currentFile.getName()))
                        || (!mPrinter.getLoaded())) {

                    Log.i(TAG, "Sigh, loading " + mPrinter.getJobPath());

                    if (LibraryController.hasExtension(1, currentFile.getName()))
                        openGcodePrintView(getActivity(), mPrinter.getJobPath(), mRootView, R.id.view_gcode);
                    else Log.i(TAG, "Das not gcode");

                    isGcodeLoaded = true;

                    //end process
                    return;

                    //Not the same file
                } else Log.i(TAG, "FAIL ;D " + mPrinter.getJobPath());

        }

        if (mPrinter.getLoaded())

            if (mPrinter.getJob().getFilename()!=null)
            //The server actually has a job
            if (!mPrinter.getJob().getFilename().equals("null")) {

                Log.i(TAG, "Either it's not the same or I don't have it, download: " + mPrinter.getJob().getFilename());

                String download = "";
                if (DatabaseController.getPreference(DatabaseController.TAG_REFERENCES,mPrinter.getName())!=null){

                    Log.i(TAG, "NOT NULLO");

                    download = DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, mPrinter.getName());

                }else {

                    Log.i(TAG, "Çyesp NULLO");
                    download = LibraryController.getParentFolder() + "/temp/" + mPrinter.getJob().getFilename();

                    //Add it to the reference list
                    DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, mPrinter.getName(),
                            LibraryController.getParentFolder() + "/temp/" + mPrinter.getJob().getFilename(), true);
                }



                //Check if we've downloaded the same file before
                //File downloadPath = new File(LibraryController.getParentFolder() + "/temp/", mPrinter.getJob().getFilename());
                File downloadPath = new File(download);

                if (downloadPath.exists()) {

                    Log.i(TAG, "Wait, I downloaded it once!");
                    openGcodePrintView(getActivity(), downloadPath.getAbsolutePath(), mRootView, R.id.view_gcode);

                    //File changed, remove jobpath
                    mPrinter.setJobPath(downloadPath.getAbsolutePath());

                    //We have to download it again
                } else {
                    //Remake temp folder if it's not available
                    if (!downloadPath.getParentFile().exists())
                        downloadPath.getParentFile().mkdirs();

                    Log.i(TAG, "Downloadinag " + downloadPath.getParentFile().getAbsolutePath() + " PLUS " + mPrinter.getJob().getFilename());

                    //Download file
                    OctoprintFiles.downloadFile(mContext, mPrinter.getAddress() + HttpUtils.URL_DOWNLOAD_FILES,
                            downloadPath.getParentFile().getAbsolutePath() + "/", mPrinter.getJob().getFilename());

                    Log.i(TAG, "Downloading and adding to preferences");

                    //Get progress dialog UI
                    View waitingForServiceDialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_progress_content_horizontal, null);
                    ((TextView) waitingForServiceDialogView.findViewById(R.id.progress_dialog_text)).setText(R.string.printview_download_dialog);

                    //Show progress dialog
                    MaterialDialog.Builder connectionDialogBuilder = new MaterialDialog.Builder(mContext);
                    connectionDialogBuilder.customView(waitingForServiceDialogView, true)
                            .autoDismiss(false);

                    //Progress dialog to notify command events
                    mDownloadDialog = new MaterialDialog.Builder(mContext)
                    .customView(waitingForServiceDialogView, true)
                    .autoDismiss(false)
                    .build();
                    mDownloadDialog.show();

                    //File changed, remove jobpath
                    mPrinter.setJobPath(null);
                }


            }

        isGcodeLoaded = true;
    }


    public void openGcodePrintView(Context context, String filePath, View rootView, int frameLayoutId) {
        //Context context = getActivity();
        mLayout = (FrameLayout) rootView.findViewById(frameLayoutId);
        File file = new File(filePath);

        DataStorage tempData = GcodeCache.retrieveGcodeFromCache(file.getAbsolutePath());

        if (tempData != null) {

            mDataGcode = tempData;
            drawPrintView();


        } else {

            mDataGcode = new DataStorage();
            GcodeFile.openGcodeFile(context, file, mDataGcode, ViewerMainFragment.PRINT_PREVIEW);

            GcodeCache.addGcodeToCache(mDataGcode);
        }


        mDataGcode.setActualLayer(mActualProgress);

    }

    public static boolean drawPrintView() {
        List<DataStorage> gcodeList = new ArrayList<DataStorage>();
        gcodeList.add(mDataGcode);

        mSurface = new ViewerSurfaceView(mContext, gcodeList, ViewerSurfaceView.LAYERS, ViewerMainFragment.PRINT_PREVIEW, null);

        mLayout.removeAllViews();
        mLayout.addView(mSurface, 0);

        changeProgress(mActualProgress);

        mSurface.setZOrderOnTop(true);

        JSONObject profile = ModelProfile.retrieveProfile(mContext, mPrinter.getProfile(), ModelProfile.TYPE_P);
        try {
            JSONObject volume = profile.getJSONObject("volume");

            mSurface.changePlate(new int[]{volume.getInt("width") / 2, volume.getInt("depth") / 2, volume.getInt("height")});
            mSurface.requestRender();

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return true;
    }

    public static void changeProgress(double percentage) {
        int maxLines = mDataGcode.getMaxLayer();
        int progress = (int) percentage * maxLines / 100;
        mDataGcode.setActualLayer(progress);
        if (mSurface != null) mSurface.requestRender();
    }

    @Override
    public void onDestroyView() {

        try {
            mContext.unregisterReceiver(onComplete);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }

        super.onDestroyView();
    }

    /**
     * Receives the "download complete" event asynchronously
     */
    public BroadcastReceiver onComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            DownloadManager manager = (DownloadManager) ctxt.getSystemService(Context.DOWNLOAD_SERVICE);

            String filename = null;

            //Get the downloaded file name
            Bundle extras = intent.getExtras();
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            Cursor c = manager.query(q);

            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                }
            }
            c.close();


            //If we have a stored path
            if (DatabaseController.isPreference(DatabaseController.TAG_REFERENCES, mPrinter.getName())) {

                String path = DatabaseController.getPreference(DatabaseController.TAG_REFERENCES, mPrinter.getName());

                File file = new File(path);


                if ((file.getName().equals(filename))){

                    //In case there was a previous cached file with the same path
                    GcodeCache.removeGcodeFromCache(path);



                    if (file.exists()) {

                        openGcodePrintView(mRootView.getContext(), path, mRootView, R.id.view_gcode);
                        mPrinter.setJobPath(path);

                        //Register receiver
                        mContext.unregisterReceiver(onComplete);

                        //DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, mPrinter.getName(), null, false);

                    } else {

                        Toast.makeText(getActivity(), R.string.printview_download_toast_error, Toast.LENGTH_LONG).show();

                        //Register receiver
                        mContext.unregisterReceiver(onComplete);
                    }
                } else {

                }
            }

            if (mDownloadDialog != null) mDownloadDialog.dismiss();
        }
    };
}

