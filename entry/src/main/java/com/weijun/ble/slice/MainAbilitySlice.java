package com.weijun.ble.slice;

import com.weijun.ble.ResourceTable;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.bluetooth.BluetoothHost;

public class MainAbilitySlice extends AbilitySlice {
    // BUNDLE_NAME、ABILITY_NAME开发者需要替换为自己项目对应的名称
    private static final String BUNDLE_NAME = "com.weijun.ble";
    private static final String CENTRAL_ABILITY_NAME = "com.weijun.ble.BleCentralAbility";
    private static final String PERIPHERAL_ABILITY_NAME = "com.weijun.ble.BlePeripheralAbility";

    // 获取 BluetoothHost 实例，管理本机蓝牙操作
    private BluetoothHost bluetooth = BluetoothHost.getDefaultHost(this);

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);
        initComponent();

        if (bluetooth.getBtState() != BluetoothHost.STATE_ON) {
            bluetooth.enableBt();
        }
    }

    private void initComponent() {
        findComponentById(ResourceTable.Id_ble_central).setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                Intent intent = new Intent();
                Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(BUNDLE_NAME)
                    .withAbilityName(CENTRAL_ABILITY_NAME)
                    .build();
                intent.setOperation(operation);
                startAbility(intent);
            }
        });

        findComponentById(ResourceTable.Id_ble_peripheral).setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                Intent intent = new Intent();
                Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(BUNDLE_NAME)
                    .withAbilityName(PERIPHERAL_ABILITY_NAME)
                    .build();
                intent.setOperation(operation);
                startAbility(intent);
            }
        });
    }
}


