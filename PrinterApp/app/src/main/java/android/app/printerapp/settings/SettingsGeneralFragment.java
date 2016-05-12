package android.app.printerapp.settings;

import android.app.Fragment;
import android.app.printerapp.Log;
import android.app.printerapp.R;
import android.app.printerapp.octoprint.HttpUtils;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Locale;

/**
 * Created by sara on 5/02/15.
 */
public class SettingsGeneralFragment extends Fragment {

    private SettingsListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retain instance to keep the Fragment from destroying itself
        setRetainInstance(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //Reference to View
        View rootView = null;

        //If is not new
        if (savedInstanceState==null){

            //Inflate the fragment
            rootView = inflater.inflate(R.layout.settings_general_fragment, container, false);

            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

			/* lkj
            CheckBox checkBox_slice = (CheckBox) rootView.findViewById(R.id.settings_slicing_checkbox);
            checkBox_slice.setChecked(sharedPref.getBoolean(getString(R.string.shared_preferences_slice), false));
            checkBox_slice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    sharedPref.edit().putBoolean(getString(R.string.shared_preferences_slice), b).apply();


                }
            });

            CheckBox checkBox_print = (CheckBox) rootView.findViewById(R.id.settings_printing_checkbox);
            checkBox_print.setChecked(sharedPref.getBoolean(getString(R.string.shared_preferences_print), false));
            checkBox_print.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    sharedPref.edit().putBoolean(getString(R.string.shared_preferences_print), b).apply();


                }
            });
	*/


			/* lkj
            CheckBox checkBox_save = (CheckBox) rootView.findViewById(R.id.settings_save_files_checkbox);
            checkBox_save.setChecked(sharedPref.getBoolean(getString(R.string.shared_preferences_save), false));
            checkBox_save.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    sharedPref.edit().putBoolean(getString(R.string.shared_preferences_save), b).apply();


                }
            });	*/

            /* lkj CheckBox checkBox_autoslice = (CheckBox) rootView.findViewById(R.id.settings_automatic_checkbox);
            checkBox_autoslice.setChecked(sharedPref.getBoolean(getString(R.string.shared_preferences_autoslice), false));
            checkBox_autoslice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    sharedPref.edit().putBoolean(getString(R.string.shared_preferences_autoslice), b).apply();


                }
            }); */



            /*********************************************************/

            getNetworkSsid(rootView);

        }
        return rootView;
    }


    public void notifyAdapter(){
        mAdapter.notifyDataSetChanged();
    }

    //Return network without quotes
    public void getNetworkSsid(View v){

        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        //TextView tv = (TextView) v.findViewById(R.id.network_name_textview);
        //tv.setText(wifiInfo.getSSID().replace("\"", ""));

        int ipAddress = wifiInfo.getIpAddress();
        String wifi_ip = null;

        wifi_ip =String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),  (ipAddress >> 16 & 0xff),
                                    (ipAddress >> 24 & 0xff));

        TextView wifi_address = (TextView) v.findViewById(R.id.wifi_address);
        if( wifi_ip != null && !wifi_ip.isEmpty())
            wifi_address.setText(wifi_ip);
        else
            wifi_address.setText("no connection");

        TextView ethernet_address = (TextView) v.findViewById(R.id.ethernet_address);
        //String eth_ip = HttpUtils.do_exec("/system/bin/busybox ifconfig eth0  | /system/bin/busybox grep 'inet addr'");
        String eth_info = HttpUtils.do_exec("/system/bin/busybox ifconfig eth0");
        String eth_ip = "no connection";
        if (eth_info != null && !eth_info.isEmpty()) {
			int bcast_index = eth_info.indexOf("Bcast:", 0);
			if (bcast_index > 0){
				int inet_index = eth_info.indexOf("inet addr:", 0) + 10;
				eth_ip = eth_info.substring(inet_index, bcast_index).trim();
			}
        }
        Log.d("lkj-getNetworkSSid", "ethernet address:" + eth_ip);
        ethernet_address.setText(eth_ip);

/* lkj
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                String name  = intf.getName();
                Log.d("lkj-getNetworkSSid", "NetworkInterface name :" + name);
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    if (!inetAddress.isLoopbackAddress())
                    {
                        String Ip= inetAddress.getHostAddress().toString();
                        Log.d("lkj-getNetworkSSid", "NetworkInterface ip:" + Ip);

                    }
                }
            }

        }
        catch (SocketException obj)
        {
            Log.e("Error occurred during IP fetching: ", obj.toString());
        }
        D/lkj-getNetworkSSid(13642): NetworkInterface name :lo
        D/lkj-getNetworkSSid(13642): NetworkInterface name :sit0
        D/lkj-getNetworkSSid(13642): NetworkInterface name :eth0
        D/lkj-getNetworkSSid(13642): NetworkInterface ip:fe80::6ac9:bff:fec8:10dc%eth0
        D/lkj-getNetworkSSid(13642): NetworkInterface ip:192.168.1.101
        D/lkj-getNetworkSSid(13642): NetworkInterface name :wlan0
        D/lkj-getNetworkSSid(13642): NetworkInterface ip:fe80::aea2:13ff:fe62:d1e2%wlan0
        D/lkj-getNetworkSSid(13642): NetworkInterface ip:192.168.1.113
*/

/*
        ImageView iv = (ImageView) v.findViewById(R.id.wifi_signal_imageview);

        int signal = wifiInfo.getRssi();

        if ((signal <= 0) && (signal > -40)){
            iv.setImageResource(R.drawable.ic_signal_wifi_4);
        } else if ((signal <= -40) && (signal > -60)){
            iv.setImageResource(R.drawable.ic_signal_wifi_3);
        } else if ((signal <= -60) && (signal > -70)){
            iv.setImageResource(R.drawable.ic_signal_wifi_2);
        } else if ((signal <= -70) && (signal > -80)){
            iv.setImageResource(R.drawable.ic_signal_wifi_1);
        } else iv.setImageResource(R.drawable.ic_signal_wifi_0);
      */
    }

    public String setBuildVersion(){

        String s = "Version v.";

        try{

            //Get version name from package
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String fString = pInfo.versionName;

            //Parse version and date
            String hash = fString.substring(0,fString.indexOf(" "));
            String date = fString.substring(fString.indexOf(" "), fString.length());

            //Format hash
            String [] fHash = hash.split(";");

            //Format date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm",new Locale("es", "ES"));
            String fDate = sdf.format(new java.util.Date(date));

            //Get version code / Jenkins build
            String code;
            if (pInfo.versionCode == 0) code = "IDE";
            else code = "#"+ pInfo.versionCode;

            //Build string
            s = s + fHash[0] + " " + fHash[1] + " " + fDate + " " + code;

        }catch(Exception e){

            e.printStackTrace();
        }

        return s;
    }
}
