package com.weijun.ble.slice;

import com.weijun.ble.BluetoothProfileByteUtil;
import com.weijun.ble.LogUtil;
import com.weijun.ble.ResourceTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.bluetooth.ProfileBase;
import ohos.bluetooth.ble.BleCentralManager;
import ohos.bluetooth.ble.BleCentralManagerCallback;
import ohos.bluetooth.ble.BlePeripheralCallback;
import ohos.bluetooth.ble.BlePeripheralDevice;
import ohos.bluetooth.ble.BleScanResult;
import ohos.bluetooth.ble.GattCharacteristic;
import ohos.bluetooth.ble.GattDescriptor;
import ohos.bluetooth.ble.GattService;

/**
 * 功能描述
 *
 * @author wWX1042998
 * @since 2021-07-01
 */
public class BleCentralAbilitySlice extends AbilitySlice {
    private static final String TAG = LogUtil.TAG_LOG + BleCentralAbilitySlice.class.getSimpleName();

    /*private static final String SERVICE_UUID =          "00001887-0000-1000-8000-00805f9b34fb";
    private static final String NOTIFY_CHARACTER_UUID = "00002a10-0000-1000-8000-00805f9b34fb";
    private static final String WRITE_CHARACTER_UUID =  "00002a11-0000-1000-8000-00805f9b34fb";*/
    private static final String MAC_ADDRESS = "50:FB:19:C8:E4:B9";

    private static final String SERVICE_UUID =          "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String NOTIFY_CHARACTER_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static final String WRITE_CHARACTER_UUID =  "0000fff2-0000-1000-8000-00805f9b34fb";

    private static final String DESCRIPTOR_CHARACTER_UUID =  "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * 启动通知
     */
    private static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};

    /**
     * 禁用通知
     */
    private static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};

    private BlePeripheralDevice peripheralDevice = null;
    private GattCharacteristic writeCharacteristic;
    private boolean isConnected = false;
    private boolean isScanning = false;
    private Text deviceText;
    private Text statusText;
    private TextField field;
    private Text dataText;
    private Button scanButton;
    private Button connectButton;
    private Button sendButton;
    private Button closeNotify;

    private boolean canNofity = false;
    private boolean canWrite = false;

    // 实现外围设备操作回调
    private class MyBlePeripheralCallback extends BlePeripheralCallback {
        // 在外围设备上发现服务的回调
        @Override
        public void servicesDiscoveredEvent(int status) {
            super.servicesDiscoveredEvent(status);
            LogUtil.info(TAG, "status == BlePeripheralDevice.OPERATION_SUCC = " +
                (status == BlePeripheralDevice.OPERATION_SUCC));
            if (status == BlePeripheralDevice.OPERATION_SUCC) {
                // 发现服务列表后，需要找到一个服务下的特征列表，根据硬件给出
                // 的Characteristic的读写uuid 过滤出读写的特征用户后面的收发信息
                for (GattService service : peripheralDevice.getServices()) {
                    checkGattCharacteristic(service);
                }
            }
        }

        private void checkGattCharacteristic(GattService service) {
            LogUtil.info(TAG, "service.getUuid() = " + service.getUuid().toString());
            for (GattCharacteristic tmpChara : service.getCharacteristics()) {
                UUID uuid = tmpChara.getUuid();
                LogUtil.info(TAG, "checkGattCharacteristic = " + uuid);
                // 服务发现，就是找到设备定义的serviceuuid下的特征值(也是uuid)，包括
                // 往设备端写与设备往手机端写的特征值
                if (uuid.equals(UUID.fromString(NOTIFY_CHARACTER_UUID))) {
                    // 必须启用特征通知
                    boolean enable = peripheralDevice.setNotifyCharacteristic(tmpChara, true);
                    LogUtil.info(TAG, "启用特征通知(也就是收消息) ? " + enable);

                    Optional<GattDescriptor> descriptor = tmpChara.getDescriptor(
                        UUID.fromString(DESCRIPTOR_CHARACTER_UUID));
                    if (descriptor.isPresent()) {
                        GattDescriptor gattDescriptor = descriptor.get();
                        gattDescriptor.setValue(ENABLE_NOTIFICATION_VALUE);
                        // 必须写描述值
                        boolean flag = peripheralDevice.writeDescriptor(gattDescriptor);
                        LogUtil.info(TAG, "写描述值成功 ? " + flag);
                        canNofity = flag;
                        sendData();
                    }

                }

                if (tmpChara.getUuid().equals(UUID.fromString(WRITE_CHARACTER_UUID))) {
                    LogUtil.info(TAG, "获取写特征(也就是发消息) ");
                    // 获取GattCharacteristic
                    writeCharacteristic = tmpChara;
                    canWrite = true;
                    sendData();
                }
            }
        }

        // 连接状态变更的回调
        @Override
        public void connectionStateChangeEvent(int connectionState) {
            super.connectionStateChangeEvent(connectionState);

            if (connectionState == ProfileBase.STATE_CONNECTED && !isConnected) {
                isConnected = true;
                // 连接成功在外围设备上发现GATT服务
                peripheralDevice.discoverServices();
                updateComponent(statusText, "状态：已连接");
            }
        }

        @Override
        public void characteristicReadEvent(GattCharacteristic characteristic, int ret) {
            super.characteristicReadEvent(characteristic, ret);
            LogUtil.info(TAG, "characteristicReadEvent ...");
        }

        @Override
        public void characteristicWriteEvent(GattCharacteristic characteristic, int ret) {
            super.characteristicWriteEvent(characteristic, ret);
            LogUtil.info(TAG, "characteristicWriteEvent ...ret:"+ret+", content="+ BluetoothProfileByteUtil.bytesToHexString(characteristic.getValue()));
        }

        // 特征变更的回调,也就是外围设备向中心设备发送数据后中心设备的回调
        @Override
        public void characteristicChangedEvent(GattCharacteristic characteristic) {
            super.characteristicChangedEvent(characteristic);
            // 接收外围设备发送的数据
            // updateComponent(dataText, new String(characteristic.getValue()));
            LogUtil.info(TAG, "characteristicChangedEvent ...");
            String hexString = BluetoothProfileByteUtil.bytesToHexString(characteristic.getValue());
            LogUtil.info(TAG, "收到16进制消息:" + hexString);
            updateComponent(dataText, hexString);
        }
    }



    // 获取外围设备操作回调
    private MyBlePeripheralCallback blePeripheralCallback = new MyBlePeripheralCallback();

    // 实现中心设备管理回调
    private class MyBleCentralManagerCallback implements BleCentralManagerCallback {
        boolean found = false;
        // 扫描结果的回调
        @Override
        public void scanResultEvent(BleScanResult bleScanResult) {
            LogUtil.info(TAG, "scanResultEvent = " + bleScanResult);
            // 根据扫描结果获取外围设备实例
            /*if (peripheralDevice == null) {
                // 获取广播数据中的服务uuids
                List<UUID> uuids = bleScanResult.getServiceUuids();
                for (UUID uuid : uuids) {
                    LogUtil.info(TAG, "uuid = " + uuid);
                    if (SERVICE_UUID.equals(uuid.toString())) {
                        peripheralDevice = bleScanResult.getPeripheralDevice();
                        int length = peripheralDevice.toString().length();
                        String deviceId = peripheralDevice.toString().substring(length - 7, length);
                        updateComponent(deviceText, "设备：" + deviceId);
                    }
                }
            }*/
                if (!found) {
                    peripheralDevice = bleScanResult.getPeripheralDevice();

                    String deviceId = peripheralDevice.getDeviceAddr();
                    LogUtil.info(TAG, "deviceId = " + deviceId);
                    if (MAC_ADDRESS.equals(deviceId)) {
                        centralManager.stopScan();
                        found = true;
                        List<UUID> uuids = bleScanResult.getServiceUuids();
                        LogUtil.info(TAG, "uuid = " + uuids);
                        int length = peripheralDevice.toString().length();
                        // String deviceId = peripheralDevice.toString().substring(length - 7, length);
                        updateComponent(deviceText, "设备：" + deviceId);

                        connectOrDisconnect();
                    }
                }

        }

        // 扫描失败回调
        @Override
        public void scanFailedEvent(int i) {
            updateComponent(deviceText, "设备：扫描失败，请重新扫描！");
        }

        // 组扫描成功回调
        @Override
        public void groupScanResultsEvent(List list) {
            // 使用组扫描时在此对扫描结果进行处理
        }
    }

    // 获取中心设备管理回调
    private MyBleCentralManagerCallback centralManagerCallback = new MyBleCentralManagerCallback();

    // 获取中心设备管理对象
    private BleCentralManager centralManager = new BleCentralManager(this, centralManagerCallback);

    // 创建扫描过滤器
    private List filters = new ArrayList<>();

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_ble_central);
        initComponent();
        initClickedListener();
    }

    // 初始化组件
    private void initComponent() {
        if (findComponentById(ResourceTable.Id_device_info) instanceof Text) {
            deviceText = (Text) findComponentById(ResourceTable.Id_device_info);
        }
        if (findComponentById(ResourceTable.Id_status) instanceof Text) {
            statusText = (Text) findComponentById(ResourceTable.Id_status);
        }
        if (findComponentById(ResourceTable.Id_data) instanceof Text) {
            dataText = (Text) findComponentById(ResourceTable.Id_data);
        }
        if (findComponentById(ResourceTable.Id_input) instanceof TextField) {
            field = (TextField) findComponentById(ResourceTable.Id_input);
        }
        if (findComponentById(ResourceTable.Id_scan) instanceof Button) {
            scanButton = (Button) findComponentById(ResourceTable.Id_scan);
        }
        if (findComponentById(ResourceTable.Id_connect) instanceof Button) {
            connectButton = (Button) findComponentById(ResourceTable.Id_connect);
        }
        if (findComponentById(ResourceTable.Id_send) instanceof Button) {
            sendButton = (Button) findComponentById(ResourceTable.Id_send);
        }
        if (findComponentById(ResourceTable.Id_closeNotify) instanceof Button) {
            closeNotify = (Button) findComponentById(ResourceTable.Id_closeNotify);
        }

    }

    private void updateComponent(Text text, String content) {
        getUITaskDispatcher().syncDispatch(new Runnable() {
            @Override
            public void run() {
                text.setText(content);
            }
        });
    }

    // 初始化点击回调
    private void initClickedListener() {
        scanButton.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (!isScanning) {
                    isScanning = true;
                    scanButton.setText("停止扫描");
                    deviceText.setText("设备：正在扫描...");
                    // 开始扫描带有过滤器的指定BLE设备
                    centralManager.startScan(filters);
                } else {
                    isScanning = false;
                    scanButton.setText("开始扫描");
                    deviceText.setText("设备：暂无设备");
                    // 停止扫描
                    centralManager.stopScan();
                }
            }
        });

        connectButton.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                connectOrDisconnect();
            }
        });

        sendButton.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                /*if (field.getText().isEmpty() || (peripheralDevice == null) || !isConnected) {
                    return;
                }

                // 向外围设备发送数据
                writeCharacteristic.setValue(field.getText().getBytes());
                peripheralDevice.writeCharacteristic(writeCharacteristic);*/

                sendData();
            }
        });


        closeNotify.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {

            }
        });
    }

    private void connectOrDisconnect() {
        if (peripheralDevice == null) {
            statusText.setText("状态：请先扫描获取设备信息");
            return;
        }

        if (!isConnected) {
            connectButton.setText("断开连接");
            statusText.setText("状态：连接中...");
            // 连接到BLE外围设备
            peripheralDevice.connect(false, blePeripheralCallback);
        } else {
            isConnected = false;
            connectButton.setText("连接设备");
            statusText.setText("状态：未连接");
            deviceText.setText("设备：暂无设备");
            writeCharacteristic.setValue("Disconnect".getBytes());
            peripheralDevice.writeCharacteristic(writeCharacteristic);
            // 断开连接
            peripheralDevice.disconnect();
            peripheralDevice = null;
        }
    }

    boolean closeNotifyFlag = true;
    String FRAME_HEADER = "AE01";
    String FRAME_TAIL = "C9";
    private void sendData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LogUtil.info(TAG, "休息完500ms 发送数据");
                sendRealData();
            }
        }).start();

    }

    private void sendRealData() {
        LogUtil.info(TAG, "send canNofity:" + canNofity+", canWrite:"+canWrite);
        if (!canNofity || !canWrite) {
            return;
        }

        String checkSum = generateCheckSum("01", "01", "03");
        // 帧头+命令+数据长度+数据+校验和+帧尾
        StringBuilder sb = new StringBuilder();
        sb.append(FRAME_HEADER).append("01").append("01").append("03")
            .append(checkSum).append(FRAME_TAIL);
        LogUtil.info(TAG, "send data:" + sb.toString());

        // 向外围设备发送数据
        writeCharacteristic.setValue(BluetoothProfileByteUtil.hexToBytes(sb.toString()));
        peripheralDevice.writeCharacteristic(writeCharacteristic);
    }

    String generateCheckSum(String cmd, String dataLength, String data) {
        // 校验和=帧头+命令+数据长度+数据
         int re = (Integer.parseInt("AE", 16) + Integer.parseInt("01", 16)
            + Integer.parseInt(cmd, 16) + Integer.parseInt(dataLength, 16)
            + Integer.parseInt(data, 16));
        String hex = Integer.toHexString(re).toUpperCase();

        return hex;
    }
}


