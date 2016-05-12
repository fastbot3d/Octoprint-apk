package android.app.printerapp.octoprint;

import android.app.printerapp.Log;
import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.model.ModelPrinter;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Addresses and static fields for the OctoPrint API connection
 *
 * @author alberto-baeza
 */

public class HttpUtils {

    public static final String CUSTOM_PORT = ":5000"; //Octoprint server listening port

    /**
     * OctoPrint URLs *
     */

    public static final String URL_FILES = "/api/files"; //File operations
    public static final String URL_CONTROL = "/api/job"; //Job operations
    public static final String URL_SOCKET = "/sockjs/websocket"; //Socket handling
    public static final String URL_CONNECTION = "/api/connection"; //Connection handling
    public static final String URL_PRINTHEAD = "/api/printer/printhead"; //Send print head commands
    public static final String URL_TOOL = "/api/printer/tool"; //Send tool commands
    public static final String URL_BED = "/api/printer/bed"; //Send bed commands
    public static final String URL_NETWORK = "/api/plugin/netconnectd"; //Network config
    public static final String URL_SLICING = "/api/slicing/cura/profiles";
    public static final String URL_DOWNLOAD_FILES = "/downloads/files/local/";
    public static final String URL_SETTINGS = "/api/settings";
    public static final String URL_AUTHENTICATION = "/apps/auth";
    public static final String URL_PROFILES = "/api/printerprofiles";
    public static final String URL_ACCESS_SETUP = "/api/setup";
    public static final String URL_SYSTEM_CMD = "/api/system";
    public static final String URL_GCODE_COMMAND = "/api/printer/command"; //lkj






    /**
     * External links *
     */

    public static final String URL_THINGIVERSE = "http://www.thingiverse.com/newest";
    public static final String URL_YOUMAGINE = "https://www.youmagine.com/designs";

    //Retrieve current API Key from database
    public static String getApiKey(String url) {

        // http://127.0.0.1/api/connection
        String parsedUrl = url.substring(0, url.indexOf("/", 1));


        Log.d("lkj-getApiKey", "getApiKey url=" + url + " parsedUrl=" + parsedUrl);

        String id = "tbot";

        /*
        for (ModelPrinter p : DevicesListController.getList()) {

            Log.d("lkj-getApiKey", "getApiKey printer address=" + p.getAddress());
            switch (p.getStatus()){

                case StateUtils.STATE_ADHOC:

                    if (p.getName().equals(PrintNetworkManager.getCurrentNetwork().replace("\"","")))
                        id = PrintNetworkManager.getNetworkId(p.getName());

                    break;

                default:

                    if (p.getAddress().equals(parsedUrl)){
                        id = PrintNetworkManager.getNetworkId(p.getName());

                        if (!DatabaseController.isPreference(DatabaseController.TAG_KEYS, id))
                            id = PrintNetworkManager.getNetworkId(p.getAddress());
                    }

                    break;
            }


        }*/


        if (DatabaseController.isPreference(DatabaseController.TAG_KEYS, id)) {
            return DatabaseController.getPreference(DatabaseController.TAG_KEYS, id);
        } else {
            Log.i("-getApiKey", "id:" + id + " not found");
            return "";
        }

    }


    //private static final String OCTOPRINT_CONFIG_PATH = "/data/.octoprint/config.yaml";
    private static final String OCTOPRINT_CONFIG_PATH = "/system/driver/config.yaml";
    public static String getLocalAPIKey() {
        File file = new File(OCTOPRINT_CONFIG_PATH);
        String apiKey = null;
        Boolean a = file.exists();

        Log.d("lkj-getLocalAPIKey", "file exist:" + a + " name:" + file.getPath());
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while ((line = reader.readLine()) != null) {
                    if (line.contains("api:") ){
                        line = reader.readLine();
                        if (line.contains("key:")){
                            apiKey = line.substring(line.indexOf(":", 0) + 1, line.length());
                            apiKey = apiKey.replace(" ", "");

                            Log.d("lkj-getLocalAPIKey", "get apkey=" + apiKey);
                        }
                        break;
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return apiKey;
    }

    public static String getFirstRun() {
        File file = new File(OCTOPRINT_CONFIG_PATH);
        String isFirstRun = "true";
        Boolean a = file.exists();

        Log.d("lkj-getFirstRun", "file exist:" + a + " name:" + file.getPath());
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while ((line = reader.readLine()) != null) {
                    if (line.contains("firstRun:") ){
                        isFirstRun = line.substring(line.indexOf(":", 0) + 1, line.length());
                        isFirstRun = isFirstRun.replace(" ", "");

                        Log.d("lkj-getFirstRun", "isFirstRun=" + isFirstRun);
                        break;
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return isFirstRun;
    }


    public void saveIPToFile(String dhcp, String ip, String mask, String gateway) {
        File file = new File("/data/.ip.sh");
        Boolean a = file.exists();

        Log.d("lkj-getFirstRun", "file exist:" + a + " name:" + file.getPath());

        file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("#!/system/bin/sh");writer.newLine();
                writer.write("dhcp=" + dhcp);writer.newLine();
                writer.write("ip=" + ip);writer.newLine();
                writer.write("mask=" + mask);writer.newLine();
                writer.write("gateway=" + gateway);writer.newLine();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    static Yaml mYaml= null;
    public static String getKeyValue(String key) {
        String key_value = null;

        if (mYaml == null){
            mYaml=new Yaml();

            File file = new File(OCTOPRINT_CONFIG_PATH);
            Boolean a = file.exists();

            Log.d("lkj-getKeyValue", "file exist:" + a + " name:" + file.getPath());
            if (file.exists()) {
                try {
                    FileInputStream fi = new FileInputStream(file.getAbsolutePath());
                    HashMap data = (HashMap)mYaml.load(fi);
                    key_value = data.get(key).toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return key_value;
    }

    public static String do_exec(String cmd) {
        String s = "/n";

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                s += line + "/n";
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return s;
    }

}
