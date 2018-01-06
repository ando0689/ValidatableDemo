package test.andranik.validatabledemo.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import test.andranik.validatabledemo.R;
import test.andranik.validatabledemo.validatable.Validatable;
import test.andranik.validatabledemo.validatable.Validator;


/**
 * Created by andranik on 5/10/2017.
 */

public class ValidEditText extends android.support.v7.widget.AppCompatEditText
        implements Validatable {
    private Validator validator;

    private Drawable validStateBackground;

    private Drawable errorStatedBackground;

    public ValidEditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.appcompat.R.attr.editTextStyle);
    }

    public ValidEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        validStateBackground = getBackground();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ValidEditText, 0, 0);

        try {
            int backgroundError = typedArray.getResourceId(R.styleable.ValidEditText_background_error, R.drawable.section_background_error);
            errorStatedBackground = ContextCompat.getDrawable(getContext(), backgroundError);
        } catch (Exception e) {
            e.printStackTrace();
            typedArray.recycle();
        }
    }


    @Override
    public TextView getTextViewVl() {
        return this;
    }

    @Override
    public String getTextVl() {
        return getText().toString();
    }

    @Override
    public boolean isFocusedVl() {
        return super.isFocused();
    }

    @Override
    public void setOnFocusChangeListenerVl(View.OnFocusChangeListener onFocusChangeListener) {
        setOnFocusChangeListener(onFocusChangeListener);
    }

    @Override
    public void setErrorVl(String error) {
        setBackground(errorStatedBackground);
        Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void cleanErrorVl() {
        setBackground(validStateBackground);
    }

    @Override
    public void setValidatorVl(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Validator getValidatorVl() {
        return validator;
    }

    @Override
    public int getIdVl() {
        return getId();
    }

    @Override
    public boolean canContinue() {
        return canContinue(true);
    }

    @Override
    public boolean canContinue(boolean showError) {
        return validator.canContinue(showError);
    }

    @Override
    public void setIgnoreNextClear(boolean ignoreNextClear) {
        validator.setIgnoreNextClear(ignoreNextClear);
    }

    @Override
    public void setValidationEnabled(boolean enabled) {
        validator.setValidationEnabled(enabled);
    }

    @Override
    public void resetValidator() {
        if(validator != null){
            validator.reset();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        resetValidator();
        super.onDetachedFromWindow();
    }
}
