package com.boredofnothing.flashcard.util;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ToastUtil {

    public void show(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    public void showLong(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }
}
