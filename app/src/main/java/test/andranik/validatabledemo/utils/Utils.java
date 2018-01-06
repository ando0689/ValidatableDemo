package test.andranik.validatabledemo.utils;

import android.content.Context;
import android.support.annotation.StringRes;

/**
 * Created by andranik on 3/7/17.
 */

public class Utils {

    public static String getStringFromRes(Context context, @StringRes int resId) {
        return context.getString(resId);
    }

}
