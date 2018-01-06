package test.andranik.validatabledemo.validatable;

import android.view.View;
import android.widget.TextView;

/**
 * Created by andranik on 3/20/17.
 */

public interface Validatable {

    TextView getTextViewVl();

    String getTextVl();

    boolean isFocusedVl();

    void setOnFocusChangeListenerVl(View.OnFocusChangeListener onFocusChangeListener);

    void setErrorVl(String error);

    void cleanErrorVl();

    void setValidatorVl(Validator validator);

    Validator getValidatorVl();

    int getIdVl();

    boolean canContinue();

    boolean canContinue(boolean showError);

    void setIgnoreNextClear(boolean ignoreNextClear);

    void setValidationEnabled(boolean enabled);

    void resetValidator();
}
