package com.weijun.ble;

/**
 * Util class
 */
public class BluetoothProfileByteUtil {
    /**
     * 16 radix
     */
    public static final int CONVERT_RADIX_16 = 16;

    /**
     * state connected
     */
    public static final int STATE_CONNECTED = 2;

    private static final String TAG = "BluetoothProfileByteUtil";
    private static final int STRING_BUFFER_DEFAULT_SIZE = 16;
    private static final int BINARY_COMPLEMENT = 0xff;
    private static final int DATA_BYTE_LENGTH = 2;

    /**
     * bytes To Hex String
     *
     * @param src byte array
     * @return String
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder(STRING_BUFFER_DEFAULT_SIZE);
        if ((src == null) || (src.length <= 0)) {
            return "";
        }
        for (byte item : src) {
            int intValue = item & BINARY_COMPLEMENT;
            String stringValue = Integer.toHexString(intValue);
            System.out.println("item="+item+", intValue:"+intValue+", stringValue:"+stringValue);
            if (stringValue.length() < DATA_BYTE_LENGTH) {
                stringBuilder.append(0);
            }
            stringBuilder.append(stringValue);
        }
        return stringBuilder.toString();
    }

    /**
     * Hex string to byte array
     *
     * @param string The hexadecimal string to be converted
     * @return byte[] The byte array corresponding to the hexadecimal string
     */
    public static byte[] hexToBytes(String string) {
        if (string == null) {
            return new byte[0];
        }
        String hexString = string.replace(" ", "");
        int cycleLength = hexString.length() / DATA_BYTE_LENGTH;
        byte[] resultByte = new byte[cycleLength];
        int beginIndex;
        int endIndex;
        for (int i = 0; i < cycleLength; i++) {
            beginIndex = i * DATA_BYTE_LENGTH;
            endIndex = i * DATA_BYTE_LENGTH + DATA_BYTE_LENGTH;
            try {
                resultByte[i] = (byte) Integer.parseInt(hexString.substring(beginIndex, endIndex), CONVERT_RADIX_16);
            } catch (NumberFormatException e) {
                LogUtil.error(TAG, "hexToBytes NumberFormatException");
            }
        }
        return resultByte;
    }
}
