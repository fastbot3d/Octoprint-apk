package android.app.printerapp.octoprint;

/**
 * Class with the type of states the printers can hold at any moment
 * @author alberto-baeza
 *
 */
public class StateUtils {
	public static final int STATE_ADHOC = -2;
	public static final int	STATE_NEW = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_OPEN_SERIAL = 1;
    public static final int STATE_DETECT_SERIAL = 2;
	public static final int	STATE_DETECT_BAUDRATE = 3;
	public static final int	STATE_CONNECTING = 4;
	public static final int	STATE_OPERATIONAL = 5;
	public static final int	STATE_PRINTING = 6;
	public static final int	STATE_PAUSED = 7;
	public static final int	STATE_CLOSED = 8;
	public static final int	STATE_ERROR = 9;
	public static final int	STATE_CLOSED_WITH_ERROR = 10;
	public static final int	STATE_TRANSFERING_FILE = 11;

    public static final int SLICER_HIDE = -1;
    public static final int SLICER_UPLOAD = 0;
    public static final int SLICER_SLICE = 1;
    public static final int SLICER_DOWNLOAD = 2;

    public static final int TYPE_WITBOX = 1;
    public static final int TYPE_PRUSA = 2;
    public static final int TYPE_CUSTOM = 3;

	public static String ConverttoOctoprintStatus(int status){
		String ret = null;

		switch (status){
			case STATE_NONE:
				ret = "Offline";
				break;

			case STATE_OPEN_SERIAL:
				ret = "Opening";
				break;

			case STATE_DETECT_SERIAL:
				ret = "Detecting serial port";
				break;

			case STATE_DETECT_BAUDRATE:
				ret = "Detecting baudrate";
				break;

			case STATE_CONNECTING:
				ret = "Connecting";
				break;

			case STATE_OPERATIONAL:
				ret = "Operational";
				break;

			case STATE_PRINTING:
				ret = "Printing";
				break;

			case STATE_PAUSED:
				ret = "Paused";
				break;
			case STATE_CLOSED:
				ret = "Closed";
				break;

			case STATE_ERROR:
				ret = "Error:";
				break;
			case STATE_CLOSED_WITH_ERROR:
				ret = "Error close";
				break;

			case STATE_TRANSFERING_FILE:
				ret = "Transfering file to SD";
				break;
			default:
				ret = "error default";
				break;
		}

		return ret;
	}

	public static int OctoprintStatusToInt(String status) {
		int ret = 0;

		if( status.contains("Offline") ){
			ret = STATE_NONE;
		} else if ( status.contains("Opening") ) {
			ret = STATE_OPEN_SERIAL;
		} else if ( status.contains("serial") ) {
			ret = STATE_DETECT_SERIAL;
		} else if ( status.contains("baudrate") ) {
			ret = STATE_DETECT_BAUDRATE;
		} else if ( status.contains("Connecting") ) {
			ret = STATE_CONNECTING;
		} else if ( status.contains("Operational") ) {
			ret = STATE_OPERATIONAL;
		} else if ( status.contains("Printing") ) {
			ret = STATE_PRINTING;
		} else if ( status.contains("Paused") ) {
			ret = STATE_PAUSED;
		} else if ( status.contains("Closed") ) {
			ret = STATE_CLOSED;
		} else if ( status.contains("Error") ) {
			ret = STATE_ERROR;
		} else if ( status.contains("Transfering") ) {
			ret = STATE_TRANSFERING_FILE;
		} else {
			ret = STATE_NONE;
		}

		return ret;
	}

	public static boolean octoprintIsOffline(int status) {
		if (status == STATE_NONE)
			return true;

		return false;
	}

	public static boolean octoprintIsClosed(int status) {
		if (status == STATE_OPERATIONAL || status == STATE_PRINTING || status == STATE_PAUSED )
			return false;

		return true;
	}
}
