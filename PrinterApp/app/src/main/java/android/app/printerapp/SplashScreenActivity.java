package android.app.printerapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.library.LibraryController;
import android.app.printerapp.model.ModelPrinter;
import android.app.printerapp.octoprint.HttpUtils;
import android.app.printerapp.octoprint.OctoprintConnection;
import android.app.printerapp.octoprint.StateUtils;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.ThemeSingleton;
import com.afollestad.materialdialogs.internal.MDTintHelper;

import android.widget.CheckBox;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.printerapp.devices.DevicesListController;

/**
 * Splash screen activity that shows the logo of the app during a time interval
 * of 3 seconds. Then, the main activity is charged and showed.
 *
 * @author sara-perez
 */
public class SplashScreenActivity extends Activity {

    private static final String TAG = "SplashScreenActivity";

    //Set the duration of the splash screen
    private static final long SPLASH_SCREEN_DELAY = 100;

    Context mContext;
  //  ModelPrinter mPrinter;
    MyHandler myHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash_screen);

        mContext = this;


        //Initialize db and lists
        new DatabaseController(this);

        //Initialize default settings
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        myHandler = new MyHandler();

        DevicesListController.mPrinter = new ModelPrinter("Fastbot", "127.0.0.1:80", StateUtils.STATE_NEW);
       // mPrinter = new ModelPrinter("Fastbot", "192.168.1.113", StateUtils.STATE_NEW);

        DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, DevicesListController.mPrinter.getName(), "/sdcard/Octoprint/uploads/", false);
        DatabaseController.handlePreference(DatabaseController.TAG_REFERENCES, DevicesListController.mPrinter.getName(), "/sdcard/Octoprint/uploads/", true);

        DevicesListController.mPrinter.setType(StateUtils.TYPE_CUSTOM);



        myHandler.sendEmptyMessage(MSG_CHECK_API_KEY);
    }

    TimerTask splashDelay = new TimerTask() {
        @Override
        public void run() {
            Intent mainIntent = new Intent().setClass(
                    SplashScreenActivity.this, MainActivity.class);
            startActivity(mainIntent);

            //Close the activity so the user won't able to go back this
            //activity pressing Back button
            finish();
        }
    };


    public final static int MSG_CHECK_API_KEY = 1000;
    public final static int MSG_CHECK_CONNECTION = 1001;
    public final static int MSG_GET_PRINTER_STATUS = 1002;
    public final static int MSG_GO_TO_MAIN_ACTIVITY = 1003;
    public final static int MSG_CONNECT_PRINTER = 1004;

    public final static int MSG_ERROR= 1999;

    class MyHandler extends Handler {
        public MyHandler() {
        }

        public MyHandler(Looper L) {
            super(L);
        }

        // 子类必须重写此方法，接受数据
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.d("MyHandler", "handleMessage msg.what=" + msg.what);
            super.handleMessage(msg);

            switch (msg.what){
                case MSG_CHECK_API_KEY:
                        String api_key = HttpUtils.getLocalAPIKey();
                        if ( api_key == null || api_key.isEmpty() ) {
                            Log.d("lkj-Splash-main", "api is null");
                            Toast.makeText(mContext, "Please make sure Octoprint is running", Toast.LENGTH_LONG);
                            finish();
                            break;
                        }

                        String api_key_local = DatabaseController.getPreference(DatabaseController.TAG_KEYS, PrintNetworkManager.getNetworkId(DevicesListController.mPrinter.getName()));
                        if ( api_key == null ||  !api_key.equals(api_key_local) ) {
                            DatabaseController.handlePreference(DatabaseController.TAG_KEYS, PrintNetworkManager.getNetworkId(DevicesListController.mPrinter.getName()), api_key, true);
                            if (!DatabaseController.checkExisting(DevicesListController.mPrinter)) {
                                DatabaseController.writeDb(DevicesListController.mPrinter.getName(), DevicesListController.mPrinter.getAddress(), String.valueOf(DevicesListController.mPrinter.getPosition()), String.valueOf(DevicesListController.mPrinter.getType()), "Local");
                            }
                        }
                        String a = DatabaseController.getPreference(DatabaseController.TAG_KEYS, PrintNetworkManager.getNetworkId(DevicesListController.mPrinter.getName()));
                        Log.d("LKJ", "add fastbot printer api:" + a);
                        myHandler.sendEmptyMessage(MSG_CHECK_CONNECTION);
                    break;

                case MSG_CHECK_CONNECTION:
                        String isFirstRun = HttpUtils.getFirstRun();
                        if ( isFirstRun == null || isFirstRun.equals("true") ) {
                            first_run(DevicesListController.mPrinter);
                        } else {
                            myHandler.sendEmptyMessage(MSG_GET_PRINTER_STATUS);
                        }
                    break;

                case MSG_GET_PRINTER_STATUS:
                        get_printer_status(DevicesListController.mPrinter);
                        break;

                case MSG_CONNECT_PRINTER:
                         OctoprintConnection.ConnectionCallBack callBack = new OctoprintConnection.ConnectionCallBack(){
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                                myHandler.sendEmptyMessage(MSG_GO_TO_MAIN_ACTIVITY);
                            }

                            public void onFailure(int statusCode, Header[] headers, String responseString){
                                myHandler.sendEmptyMessage(MSG_ERROR);
                            }
                        };
                        OctoprintConnection.connect_printer(mContext, DevicesListController.mPrinter, callBack);
                    break;

                case MSG_GO_TO_MAIN_ACTIVITY:
                        DevicesListController.loadList(mContext);

                        LibraryController.initializeHistoryList();

                        if (isTaskRoot()) {
                            //Simulate a long loading process on application startup
                            Timer timer = new Timer();
                            timer.schedule(splashDelay, SPLASH_SCREEN_DELAY);
                        }  else
                            finish();
                    break;
                case MSG_ERROR:
                        Log.d("lkj-MyHandler", "err message, exit app");
                        finish();
                    break;

                default:
                        Log.d("lkj-MyHandler", "unkonwn message");
                    break;

            }
            //Bundle b = msg.getData();
            //String color = b.getString("color");
            //MyHandlerActivity.this.button.append(color);

        }
    }



    View first_run_positiveAction;
    EditText first_run_name_input;
    EditText first_run_password_input;
    private void first_run(final ModelPrinter p){

        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.first_tile)
                .customView(R.layout.first_run_dialog, true)
                .positiveText(android.R.string.ok)
                .autoDismiss(false)
                .cancelable(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(final MaterialDialog dialog2) {
                        final OctoprintConnection.ConnectionCallBack callBack = new OctoprintConnection.ConnectionCallBack() {
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                myHandler.sendEmptyMessage(MSG_GET_PRINTER_STATUS);
                                dialog2.dismiss();
                            }

                            public void onFailure(int statusCode, Header[] headers, String responseString) {
                                myHandler.sendEmptyMessage(MSG_ERROR);
                                dialog2.dismiss();
                            }
                        };

                        String user_name = first_run_name_input.getText().toString();
                        String user_passwd = first_run_password_input.getText().toString();
                        Log.d("lkj-first_run", "user:" + user_name + " passwd:" + user_passwd);
                        OctoprintConnection.firstSetup(mContext, p, user_name, user_passwd, callBack);
                    }
                }).build();

                first_run_positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
                first_run_name_input = (EditText) dialog.getCustomView().findViewById(R.id.username_edit);
                first_run_password_input = (EditText) dialog.getCustomView().findViewById(R.id.password_edit);
                first_run_password_input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        first_run_positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                // Toggling the show password CheckBox will mask or unmask the password input EditText
                CheckBox checkbox = (CheckBox) dialog.getCustomView().findViewById(R.id.showPassword);
                checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        first_run_password_input.setInputType(!isChecked ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT);
                        first_run_password_input.setTransformationMethod(!isChecked ? PasswordTransformationMethod.getInstance() : null);
                    }
                });

                //Workaround for CheckBox theming  on API 10 until AppCompat fix it
                int widgetColor = ThemeSingleton.get().widgetColor;
                MDTintHelper.setCheckBoxTint(checkbox,
                        widgetColor == 0 ? getResources().getColor(R.color.material_pink_500) : widgetColor);

                MDTintHelper.setEditTextTint(first_run_password_input,
                        widgetColor == 0 ? getResources().getColor(R.color.material_pink_500) : widgetColor);

        dialog.show();
        first_run_positiveAction.setEnabled(false); // disabled by default
    }


    private void get_printer_status(final ModelPrinter p){
        final OctoprintConnection.ConnectionCallBack callBack_getPrinterStatus = new OctoprintConnection.ConnectionCallBack(){
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                myHandler.sendEmptyMessage(MSG_CONNECT_PRINTER);
            }

            public void onFailure(int statusCode, Header[] headers, String responseString){
                myHandler.sendEmptyMessage(MSG_ERROR);
            }
        };
        OctoprintConnection.getPrinterStatus(mContext, p, callBack_getPrinterStatus);
    }


    View connect_printer_positiveAction;
    String select_profile_name = null;
    private void connect_printer(final ModelPrinter p) {
        final OctoprintConnection.ConnectionCallBack callBack = new OctoprintConnection.ConnectionCallBack(){
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                myHandler.sendEmptyMessage(MSG_GO_TO_MAIN_ACTIVITY);
            }

            public void onFailure(int statusCode, Header[] headers, String responseString){
                myHandler.sendEmptyMessage(MSG_ERROR);
            }
        };

        ArrayList<String> profilesID = p.getProfileIDArray();

        if (StateUtils.octoprintIsClosed(p.getStatus())) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.connect_printer_title)
                    .items(profilesID.toArray(new CharSequence[profilesID.size()]))
                    .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                select_profile_name = text.toString();
                                Log.d("lkj-connect_printer", "select:" + select_profile_name);
                                return true; // allow selection
                            }
                        })
                    .alwaysCallSingleChoiceCallback()
                    .positiveText(R.string.connect_printer_button)
                    .show();

            connect_printer_positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
            connect_printer_positiveAction.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            p.setProfile(select_profile_name);
                            OctoprintConnection.startConnection(p.getAddress(), mContext, "AUTO", select_profile_name, callBack);
                            Log.d("lkj-connect_printer", "2 connect with id:" + select_profile_name);
                        }
                    }
            );
            //dialog.show();
            Log.d("lkj-connect_printer", "is closed");
        } else if (StateUtils.octoprintIsOffline(p.getStatus())){
            Log.d("lkj-connect_printer", "is offline");
            myHandler.sendEmptyMessage(MSG_ERROR);
        } else {
            Log.d("lkj-connect_printer", "is online");
            myHandler.sendEmptyMessage(MSG_GO_TO_MAIN_ACTIVITY);

        }
    }

}
