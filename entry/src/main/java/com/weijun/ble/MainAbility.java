package com.weijun.ble;

import com.weijun.ble.slice.MainAbilitySlice;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

public class MainAbility extends Ability {
    @Override
    public void onStart(Intent intent) {
        String[] permissions = {"ohos.permission.LOCATION"};
        requestPermissionsFromUser(permissions, 0);

        super.onStart(intent);
        super.setMainRoute(MainAbilitySlice.class.getName());
    }
}
