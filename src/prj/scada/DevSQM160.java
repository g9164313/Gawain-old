package prj.scada;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

/**
 * INFICON SQM-160 Multi-Film Rate/Thickness Monitor
 * Default port setting are "19200,8n1"
 * @author qq
 *
 */
public class DevSQM160 extends DevTTY {

	public DevSQM160() {
	}

	public DevSQM160(String infoPath) {
		open(infoPath);
	}

	/**
	 * Implement SQM-160 communications protocol.<p>
	 * Exclude 'sync' character, length and CRC.<p>
	 * It will also fetch response packet.<p>
	 * @param cmd - just command, excluding 'sync' character, length and CRC
	 * @return
	 */
	public String exec(String cmd) {
		
		char tmp = (char) (cmd.length() + 34);
		
		cmd = "!" + tmp + cmd;
		
		short crc = calcCrc(cmd.toCharArray());
		
		char crc1 = (char)(crcLow(crc));
		
		char crc2 = (char)(crcHigh(crc));
		
		cmd = cmd + crc1  + crc2;
		
		//first, write command
		writeTxt(cmd);
		//second, wait 'Sync' character
		readByte();
		
		return cmd;
	}

	private short calcCrc(char[] str) {
		short crc = 0;
		short tmpCRC;
		int length = 1 + str[1] - 34;
		if (length > 0) {
			crc = (short) 0x3fff;
			for (int jx = 1; jx <= length; jx++) {
				crc = (short) (crc ^ (short) str[jx]);
				for (int ix = 0; ix < 8; ix++) {
					tmpCRC = crc;
					crc = (short) (crc >> 1);
					if ((tmpCRC & 0x1) == 1) {
						crc = (short) (crc ^ 0x2001);
					}
				}
				crc = (short) (crc & 0x3fff);
			}
		}
		return crc;
	}

	private byte crcLow(short crc) {
		byte val = (byte) ((crc & 0x7f) + 34);
		return val;
	}
	
	private byte crcHigh(short crc) {
		byte val = (byte) (((crc >> 7) & 0x7f) + 34);
		return val;
	}
	// --------------------------------//

	/**
	 * we can get information about thickness, rate, frequency for each sensor. <p>
	 * 
	 */
	private class PropValue {
		public StringProperty thick = new SimpleStringProperty("0");
		public StringProperty rate = new SimpleStringProperty("0");
		public StringProperty freq = new SimpleStringProperty("0");
	};

	private PropValue[] propValue = { new PropValue(), new PropValue(), new PropValue(), new PropValue(),
			new PropValue(), new PropValue() };

	/**
	 * We can read a average thickness value from device.
	 * <p>
	 */
	private SimpleStringProperty propAvgThick = new SimpleStringProperty("0");

	/**
	 * The thickness unit for sensor.It can be kÅ, μm, Hz, μg/cm².
	 * <p>
	 */
	private StringProperty propUnitThick = new SimpleStringProperty("A/s");

	/**
	 * The rate unit for sensor.It can be Å/s, nm/s, Hz, μg/cm²/s.
	 * <p>
	 */
	private StringProperty propUnitRate = new SimpleStringProperty("A/s");

	@Override
	protected Node eventLayout() {

		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");

		return root;
	}

}
