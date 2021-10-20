package com.weijun.ble;

import com.weijun.ble.slice.BleCentralAbilitySlice;
import com.weijun.ble.slice.BlePeripheralAbilitySlice;
import com.weijun.ble.slice.MainAbilitySlice;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;

/**
 * 功能描述
 *
 * @author wWX1042998
 * @since 2021-07-01
 */
public class BlePeripheralAbility extends Ability {

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(BlePeripheralAbilitySlice.class.getName());
    }
}
