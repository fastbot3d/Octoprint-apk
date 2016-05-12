package android.app.printerapp.model;

import android.app.printerapp.Log;
import android.app.printerapp.R;
import android.content.Context;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class will define the profile type that will be used by the printers / quality types
 * Created by alberto-baeza on 12/4/14.
 */
public class ModelProfile {

    //Printer profiles
    public static final String WITBOX_PROFILE = "bq_witbox";
    public static final String PRUSA_PROFILE = "bq_hephestos";
    public static final String DEFAULT_PROFILE = "CUSTOM";

    public static final String TYPE_P = ".profile";
    public static final String TYPE_Q = ".quality";
    public static final String TYPE_SLICE_PROFILE = "slice_type";

    //Quality profiles
    public static final String LOW_PROFILE = "low_bq";
    public static final String MEDIUM_PROFILE = "medium_bq";
    public static final String HIGH_PROFILE = "high_bq";

    private static final String[] PRINTER_TYPE = {"Witbox", "Hephestos"};
    private static final String[] PROFILE_OPTIONS = {HIGH_PROFILE, MEDIUM_PROFILE, LOW_PROFILE};

    private String name = null;
    private String id = null;

    private static ArrayList<String> mProfileList;
    private static ArrayList<String> mQualityList;

    public ModelProfile(String id, String name){
        this.name = name;
        this.id = id;
    }

    public String getId(){ return this.id; };
    public String getName(){ return this.name; };


    //Retrieve a profile in JSON format
    public static JSONObject retrieveProfile00(Context context, String resource, String type){

        int id = 0;

        //Select a predefined profile
        if (resource.equals(WITBOX_PROFILE)) id = R.raw.witbox;
        if (resource.equals(PRUSA_PROFILE)) id = R.raw.prusa;
        if (resource.equals(DEFAULT_PROFILE)) id = R.raw.defaultprinter;
        if (resource.equals(LOW_PROFILE)) id = R.raw.low;
        if (resource.equals(MEDIUM_PROFILE)) id = R.raw.medium;
        if (resource.equals(HIGH_PROFILE)) id = R.raw.high;

        InputStream fis = null;


        id = R.raw.low;  //lkj fix ?

        if (id != 0)  fis = context.getResources().openRawResource(id);
        else { //Custom profile

            try {
                Log.i("PROFILE", "Looking for " + resource);
                fis = context.openFileInput(resource + type);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
       //fis = context.getResources().openRawResource(id);
        JSONObject json = null;
        if (fis!=null){

            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line = null;



            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                reader.close();


                json = new JSONObject(sb.toString());

                Log.i("lkj-json", json.toString());


            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



        return json;

    }


    //Retrieve a profile in JSON format
    public static JSONObject retrieveProfile(Context context, String resource, String type){

        int id = 0;
        InputStream fis = null;

        File printerProfileFolder = null;
        File profileResult = null;
        String dirPath;
        if (type.contains(TYPE_P)){
            dirPath = Environment.getExternalStorageDirectory().toString() + "/Octoprint/printerProfiles/";
            printerProfileFolder = new File(dirPath);
        } else if (type.contains(TYPE_SLICE_PROFILE)){
            dirPath = Environment.getExternalStorageDirectory().toString() + "/Octoprint/slicingProfiles/cura/";
            printerProfileFolder = new File(dirPath);
        }

        for (File file : printerProfileFolder.listFiles()) {
                int pos = file.getName().lastIndexOf(".");
                String name = pos > 0 ? file.getName().substring(0, pos) : file.getName();
                if(name.equals(resource)){
                    profileResult = file;
                    Log.d("lkj retrieveProfile", "find it profile name:" + resource);
                    break;
                }

        }
        if (profileResult == null){
            Log.d("lkj retrieveProfile", "cann't find profile name:" + resource);
            return null;
        }

        try {
            fis = new FileInputStream(profileResult.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject json = null;
        if (fis!=null){
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                reader.close();
               // json = new JSONObject(sb.toString());
                json = convertToJson(sb.toString());

                Log.i("lkj-json", json.toString());
           // } catch (JSONException e) {
             //   e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    private static JSONObject convertToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);

        JSONObject jsonObject = new JSONObject(map);
        return jsonObject;
    }

    //Save a new custom profile
    public static boolean saveProfile(Context context, String name, JSONObject json, String type){

        int id = 0;
        FileOutputStream outputStream = null;
        File printerProfileFolder = null;

        String dirPath;
        if (type.contains(TYPE_P)){
            dirPath = Environment.getExternalStorageDirectory().toString() + "/Octoprint/printerProfiles/";
            printerProfileFolder = new File(dirPath + name + ".profile");
        } else if (type.contains(TYPE_SLICE_PROFILE)){
            dirPath = Environment.getExternalStorageDirectory().toString() + "/Octoprint/slicingProfiles/cura/";
            printerProfileFolder = new File(dirPath + name + ".profile");
        }

        try {
            outputStream = new FileOutputStream(printerProfileFolder.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (outputStream != null) {
            try {
                outputStream.write(json.toString().getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;

    }

    //Delete profile file from internal storage
    public static boolean deleteProfile(Context context, String name, String type) {

        File file = new File(context.getFilesDir(), name + type);
        if (file.delete()) return true;

        return false;

    }

    public static void reloadQualityList(Context context){

        //Add default types plus custom types from internal storage
        mQualityList = new ArrayList<String>();
        mQualityList.clear();
      //  for (String s : PROFILE_OPTIONS) {
      //      mQualityList.add(s);
      //  }
        Log.d("lkj reloadQualityList", "add slice profile");

        String sdOctoprint = Environment.getExternalStorageDirectory().toString() + "/Octoprint/slicingProfiles/cura/";
        File printerProfileFolder = new File(sdOctoprint);

        if(!printerProfileFolder.exists()){
            return;
        }

        //Add internal storage types
       // for (File file : context.getApplicationContext().getFilesDir().listFiles()) {
          for (File file : printerProfileFolder.listFiles()) {
            //Only files with the .profile extension
            if (file.getAbsolutePath().contains(TYPE_P)) {

                int pos = file.getName().lastIndexOf(".");
                String name = pos > 0 ? file.getName().substring(0, pos) : file.getName();

                Log.d("lkj reloadQualityList", "add slice profile name:" + name);
                //Add only the name
                mQualityList.add(name);
            }
        }
    }

    public static void reloadList(Context context, ArrayList<String> profileIDs){

        //Add default types plus custom types from internal storage
        mProfileList = new ArrayList<String>();
        mProfileList.clear();

        String[] array =new String[profileIDs.size()];
        profileIDs.toArray(array);

       for (String s : array) {
            mProfileList.add(s);
        }

        //Add internal storage types
        for (File file : context.getApplicationContext().getFilesDir().listFiles()) {
            //Only files with the .profile extension
            if (file.getAbsolutePath().contains(TYPE_P)) {

                int pos = file.getName().lastIndexOf(".");
                String name = pos > 0 ? file.getName().substring(0, pos) : file.getName();

                //Add only the name
                mProfileList.add(name);
            }

        }

    }

    public static void reloadList(Context context){

        //Add default types plus custom types from internal storage
        mProfileList = new ArrayList<String>();
        mProfileList.clear();
      /*lkj  for (String s : PRINTER_TYPE) {

            mProfileList.add(s);
        }
        */
        mProfileList.add("default");


        String sdOctoprint = Environment.getExternalStorageDirectory().toString() + "/Octoprint/printerProfiles/";
        Log.d("lkj reloadList", "add profile name: default, search sdOctoprint proifle:" + sdOctoprint);
        File printerProfileFolder = new File(sdOctoprint);

        if(printerProfileFolder == null){
            return;
        }

        //Add internal storage types
       // for (File file : context.getApplicationContext().getFilesDir().listFiles()) {
        for (File file : printerProfileFolder.listFiles()) {

            //Only files with the .profile extension
            if (file.getAbsolutePath().contains(TYPE_P)) {

                int pos = file.getName().lastIndexOf(".");
                String name = pos > 0 ? file.getName().substring(0, pos) : file.getName();

                Log.d("lkj reloadList", "add profile name:" + name );
                //Add only the name
                mProfileList.add(name);
            }
        }

    }

    public static ArrayList<String> getProfileList(){
        return mProfileList;
    }
    public static ArrayList<String> getQualityList(){
        return mQualityList;
    }

}
