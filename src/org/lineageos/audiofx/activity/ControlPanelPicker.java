/*
 * SPDX-FileCopyrightText: 2011 The Android Open Source Project
 * SPDX-FileCopyrightText: 2015-2016 The Cyanogenmod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.audiofx.activity;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.lineageos.audiofx.Compatibility;
import org.lineageos.audiofx.Compatibility.Service;
import org.lineageos.audiofx.R;

import java.util.List;

/**
 * shows a dialog that lets the user switch between control panels
 */
public class ControlPanelPicker extends AppCompatActivity implements OnClickListener {

    private int mCheckedItem;
    private Cursor mCursor;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] cols = new String[]{"_id", "title", "package", "name"};
        MatrixCursor c = new MatrixCursor(cols);

        PackageManager pmgr = getPackageManager();
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        List<ResolveInfo> ris = pmgr.queryIntentActivities(i,
                PackageManager.GET_DISABLED_COMPONENTS);
        SharedPreferences pref = getSharedPreferences("musicfx", MODE_PRIVATE);
        String savedDefPackage = pref.getString("defaultpanelpackage", null);
        String savedDefName = pref.getString("defaultpanelname", null);
        int cnt = -1;
        int defpanelidx = 0;
        for (ResolveInfo foo : ris) {
            if (foo.activityInfo.name.equals(Compatibility.Redirector.class.getName())) {
                continue;
            }
            CharSequence name = pmgr.getApplicationLabel(foo.activityInfo.applicationInfo);
            c.addRow(new Object[]{0, name, foo.activityInfo.packageName, foo.activityInfo.name});
            cnt += 1;
            if (foo.activityInfo.name.equals(savedDefName) &&
                    foo.activityInfo.packageName.equals(savedDefPackage) &&
                    foo.activityInfo.enabled) {
                // mark as default in the list
                defpanelidx = cnt;
            }
        }

        mCursor = c;
        mCheckedItem = defpanelidx;

        new AlertDialog.Builder(this)
                .setTitle(R.string.picker_title)
                .setSingleChoiceItems(c, defpanelidx, "title", (dialog, which) -> {
                    mCheckedItem = which;
                })
                .setPositiveButton(getOkStringResId(), this)
                .setNegativeButton(getCancelStringResId(), this)
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    private int getOkStringResId() {
        int resId = getResources().getIdentifier("ok", "string", "android");
        return resId != 0 ? resId : android.R.string.ok;
    }

    private int getCancelStringResId() {
        int resId = getResources().getIdentifier("cancel", "string", "android");
        return resId != 0 ? resId : android.R.string.cancel;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // set new default
            Intent updateIntent = new Intent(this, Service.class);
            mCursor.moveToPosition(mCheckedItem);
            updateIntent.putExtra("defPackage", mCursor.getString(2));
            updateIntent.putExtra("defName", mCursor.getString(3));
            startService(updateIntent);
        }
        finish();
    }
}
