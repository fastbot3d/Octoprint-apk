package android.app.printerapp.library;

import android.app.ProgressDialog;
import android.app.printerapp.Log;
import android.app.printerapp.R;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;

/**
 * This class will search for files inside the External directory to add them as projects.
 * <p/>
 * Created by alberto-baeza on 1/15/15.
 */
public class FileScanner {

    //private ArrayList<File> mFileList2 = new ArrayList<File>();
    private ArrayList<File> mFileList = null;

    ProgressDialog mPprogressDialog;
    Handler mHandler;
    MaterialDialog md;
    Context mContext;
    public FileScanner(final String path, Context context, Handler handler,  ArrayList<File> fileList) {


        Log.i("Scanner", "Starting scanner! xx");

        mContext = context;
        mHandler = handler;
        mFileList = fileList;
        if(mFileList.size() > 0 ) {
            mFileList.clear();
        }

        startScan(path);
        handler.sendMessage(handler.obtainMessage(1001, 10, 0));

        String sdCard1Path = "/storage/sdcard1/";
        startScan(sdCard1Path);
        handler.sendMessage(handler.obtainMessage(1001, 60, 0));

        String usbPath = "/storage/usb1/";
        startScan(usbPath);
        handler.sendMessage(handler.obtainMessage(1001, 100, 0));

        handler.sendMessage(handler.obtainMessage(1003, 100, 0));
        //addDialog2(mContext);

        Log.i("Scanner", "Found " + fileList.size() + " elements!");
    }

    //Scan recursively for files in the external directory
    private void startScan(String path) {

        String sdOctoprint = Environment.getExternalStorageDirectory().toString() + "/Octoprint";

        File pathFile = new File(path);

        File[] files = pathFile.listFiles();

        if (files != null)
            for (final File file : files) {
                Bundle bundle = new Bundle();
                bundle.putString("fileName", file.getAbsolutePath());
                Message msg =mHandler.obtainMessage(1002);
                msg.setData(bundle);
                msg.sendToTarget();


              //  Log.d("lkj search ", "search :" + file.getAbsolutePath());
                //If folder
                if (file.isDirectory() && !file.getAbsolutePath().contains(sdOctoprint)) {
                    //exclude files from the application folder
                    if(file.getAbsolutePath().contains("LOST.DIR") || file.getName().startsWith(".")){
                        continue;
                    }
                    if (!file.getAbsolutePath().contains(LibraryController.getParentFolder().getAbsolutePath())) {
                        startScan(file.getAbsolutePath());
                    }
                } else {

                    //Add stl/gcodes to the search list
                    if (LibraryController.hasExtension(0, file.getName()) || (LibraryController.hasExtension(1, file.getName()))) {

                        Log.i("Scanner", "File found! " + file.getName());

                        if (!LibraryController.fileExists(file.getName())) {

                            mFileList.add(file);
                        }


                    }


                }


            }

    }

    //Creates a dialog to add the files
    public static void addDialog(final Context context, final ArrayList<File> fileList) {

        String[] fileNames = new String[fileList.size()];
        final boolean[] checkedItems = new boolean[fileList.size()];

        int i = 0;

        for (File f : fileList) {

            fileNames[i] = f.getName();
            checkedItems[i] = false;
            i++;

        }

        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.dialog_list, null);

        final uk.co.androidalliance.edgeeffectoverride.ListView listView =
                (uk.co.androidalliance.edgeeffectoverride.ListView) view.findViewById(R.id.dialog_list_listview);
        listView.setSelector(context.getResources().getDrawable(R.drawable.list_selector));
        TextView emptyText = (TextView) view.findViewById(R.id.dialog_list_emptyview);
        listView.setEmptyView(emptyText);

        ArrayAdapter<String> ad = new ArrayAdapter<String>(context, R.layout.list_item_add_models_dialog, R.id.text1, fileNames);
        listView.setAdapter(ad);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setDivider(null);

        new MaterialDialog.Builder(context)
                .title(R.string.library_scan_dialog_title)
                .customView(view, false)
                .negativeText(R.string.cancel)
                .negativeColorRes(R.color.body_text_2)
                .positiveText(R.string.dialog_continue)
                .positiveColorRes(R.color.theme_accent_1)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        SparseBooleanArray ids = listView.getCheckedItemPositions();

                        ArrayList<File> mCheckedFiles = new ArrayList<File>();
                        for (int i = 0; i < ids.size(); i++) {

                            if (ids.valueAt(i)) {

                                File file = fileList.get(ids.keyAt(i));
                                mCheckedFiles.add(file);
                                Log.i("Scanner", "Adding: " + file.getName());

                            }
                        }

                        if (mCheckedFiles.size() > 0)
                            LibraryModelCreation.enqueueJobs(context, mCheckedFiles); //enqueue checked files

                    }
                })
                .build()
                .show();

    }


    //Creates a dialog to add the files
    private void addDialog2(final Context context) {

        String[] fileNames = new String[mFileList.size()];
        final boolean[] checkedItems = new boolean[mFileList.size()];

        int i = 0;

        for (File f : mFileList) {

            fileNames[i] = f.getName();
            checkedItems[i] = false;
            i++;

        }

        LayoutInflater li = LayoutInflater.from(context);
        View view = li.inflate(R.layout.dialog_list, null);

        final uk.co.androidalliance.edgeeffectoverride.ListView listView =
                (uk.co.androidalliance.edgeeffectoverride.ListView) view.findViewById(R.id.dialog_list_listview);
        listView.setSelector(context.getResources().getDrawable(R.drawable.list_selector));
        TextView emptyText = (TextView) view.findViewById(R.id.dialog_list_emptyview);
        listView.setEmptyView(emptyText);

        ArrayAdapter<String> ad = new ArrayAdapter<String>(context, R.layout.list_item_add_models_dialog, R.id.text1, fileNames);
        listView.setAdapter(ad);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setDivider(null);

        new MaterialDialog.Builder(context)
                .title(R.string.library_scan_dialog_title)
                .customView(view, false)
                .negativeText(R.string.cancel)
                .negativeColorRes(R.color.body_text_2)
                .positiveText(R.string.dialog_continue)
                .positiveColorRes(R.color.theme_accent_1)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        SparseBooleanArray ids = listView.getCheckedItemPositions();

                        ArrayList<File> mCheckedFiles = new ArrayList<File>();
                        for (int i = 0; i < ids.size(); i++) {

                            if (ids.valueAt(i)) {

                                File file = mFileList.get(ids.keyAt(i));
                                mCheckedFiles.add(file);
                                Log.i("Scanner", "Adding: " + file.getName());

                            }
                        }

                        if (mCheckedFiles.size() > 0)
                            LibraryModelCreation.enqueueJobs(context, mCheckedFiles); //enqueue checked files

                    }
                })
                .build()
                .show();

    }


}
