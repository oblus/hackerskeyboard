package org.n0pocketworkstation.pckeyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

public class NotificationReceiver extends BroadcastReceiver {
    static final String TAG = "PCKeyboard/Notification";
    static public final String ACTION_SHOW = "org.n0pocketworkstation.pckeyboard.SHOW";
    static public final String ACTION_SETTINGS = "org.n0pocketworkstation.pckeyboard.SETTINGS";

    private LatinIME mIME;

    NotificationReceiver(LatinIME ime) {
        super();
        mIME = ime;
        Log.i(TAG, "NotificationReceiver created, ime=" + mIME);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "NotificationReceiver.onReceive called, action=" + action);

        if (action.equals(ACTION_SHOW)) {
            if (mIME != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mIME.requestShowSelf(0);
                } else {
                    InputMethodManager imm = (InputMethodManager)
                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        //noinspection deprecation
                        imm.showSoftInputFromInputMethod(mIME.mToken, 0);
                    }
                }
            }
        } else if (action.equals(ACTION_SETTINGS)) {
            context.startActivity(new Intent(mIME, LatinIMESettings.class));
        }
    }
}
