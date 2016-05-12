package android.app.printerapp.devices.camera;

import android.app.printerapp.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;


/**
 * External class
 * @author alberto-baeza
 *
 */
public class MjpegInputStream extends DataInputStream {
    private static final String TAG = "MjpegInputStream";

    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final String CONTENT_LENGTH = "Content-Length";
    private final String CONTENT_END = "\r\n";
    private final static int HEADER_MAX_LENGTH = 500;
    //private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
    private final static int FRAME_MAX_LENGTH = 200000 + HEADER_MAX_LENGTH;
   // private final static int FRAME_MAX_LENGTH = 9000 + HEADER_MAX_LENGTH;


    private final static byte[] gFrameData = new byte[FRAME_MAX_LENGTH];
    private final static byte[] gHeader = new byte[HEADER_MAX_LENGTH];
    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

    private int mContentLength = -1;

    byte[] CONTENT_LENGTH_BYTES;
    byte[] CONTENT_END_BYTES;

    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
        bitmapOptions.inSampleSize = 1;
        bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmapOptions.inPreferQualityOverSpeed = false;
        bitmapOptions.inPurgeable = true;
        try
        {
            CONTENT_LENGTH_BYTES = CONTENT_LENGTH.getBytes("UTF-8");
            CONTENT_END_BYTES = CONTENT_END.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length) {
                    return i + 1;
                }
            } else {
                seqIndex = 0;
            }
        }
        return -1;
    }

    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int findPattern(byte[] buffer, int bufferLen, byte[] pattern, int offset)
    {
        int seqIndex = 0;
        for(int i=offset; i < bufferLen; ++i)
        {
            if(buffer[i] == pattern[seqIndex])
            {
                ++seqIndex;
                if(seqIndex == pattern.length)
                {
                    return i + 1;
                }
            } else
            {
                seqIndex = 0;
            }
        }

        return -1;
    }

    private int parseContentLength(byte[] headerBytes, int length) throws IOException, NumberFormatException
    {
        int begin = findPattern(headerBytes, length, CONTENT_LENGTH_BYTES, 0);
        int end = findPattern(headerBytes, length, CONTENT_END_BYTES, begin) - CONTENT_END_BYTES.length;

        // converting string to int
        int number = 0;
        int radix = 1;
        for(int i = end - 1; i >= begin; --i)
        {
            if(headerBytes[i] > 47 && headerBytes[i] < 58)
            {
                number += (headerBytes[i] - 48) * radix;
                radix *= 10;
            }
        }

        return number;
    }

    private int parseContentLength(byte[] headerBytes) throws IOException, NumberFormatException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }   

    public Bitmap readMjpegFrame() throws IOException {

        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);

        if(headerLen < 0)
            return null;

        reset();
      //  byte[] header = new byte[headerLen];
        //readFully(header);
        readFully(gHeader, 0, headerLen);


        try {
            mContentLength = parseContentLength(gHeader, headerLen);
        } catch (NumberFormatException nfe) { 
            nfe.getStackTrace();
            Log.d(TAG, "catch NumberFormatException hit");
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER); 
        }

        reset();
       // byte[] frameData = new byte[mContentLength];
       // skipBytes(headerLen);
        readFully(gFrameData, headerLen, mContentLength);

        //return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
        Bitmap bm = BitmapFactory.decodeByteArray(gFrameData, 0, mContentLength, bitmapOptions);
        bitmapOptions.inBitmap = bm;
        return bm;
    }

    public Bitmap readMjpegFrame2() throws IOException {

        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);

        if(headerLen < 0)
            return null;

        // Log.d(TAG, "headerLen=" + headerLen);
        reset();

        //  byte[] header = new byte[headerLen];
        //readFully(header);
        try {
            readFully(gHeader, 0, headerLen);
        } catch (Exception nfe) {
            android.util.Log.d(TAG, "readFully catch Exception=" + nfe.toString());
            return null;
        }

        try {
            mContentLength = parseContentLength(gHeader, headerLen);
        } catch (NumberFormatException nfe) {
            nfe.getStackTrace();
            android.util.Log.d(TAG, "catch NumberFormatException hit");
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER);
        } catch (Exception nfe) {
            nfe.getStackTrace();
            android.util.Log.d(TAG, "catch Exception hit=" + nfe.toString());
        }

        reset();
        // byte[] frameData = new byte[mContentLength];
        skipBytes(headerLen);
        readFully(gFrameData, 0, mContentLength);

        //return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
        Bitmap bm = BitmapFactory.decodeByteArray(gFrameData, 0, mContentLength, bitmapOptions);
        bitmapOptions.inBitmap = bm;
        return bm;
    }
}
