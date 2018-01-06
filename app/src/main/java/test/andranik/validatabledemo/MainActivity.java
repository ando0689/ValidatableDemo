package test.andranik.validatabledemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import test.andranik.validatabledemo.custom_views.ValidEditText;
import test.andranik.validatabledemo.utils.RegexUtils;
import test.andranik.validatabledemo.validatable.Validator;
import test.andranik.validatabledemo.validatable.ValidatorChain;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ValidEditText emailEt;
    private ValidEditText passwordEt;

    private ValidatorChain validatorChain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailEt = findViewById(R.id.activity_main_email);
        passwordEt = findViewById(R.id.activity_main_password);

        findViewById(R.id.activity_main_button).setOnClickListener(this);

        initValidators();
    }

    private void initValidators(){
        Validator.Builder.createFor(emailEt)
                .setRequiredField(true)
                .addValidationRule(RegexUtils.EMAIL_PATTERN, R.string.warning_incorrect_email)
                .build();


        Validator.Builder.createFor(passwordEt)
                .setRequiredField(true)
                .addValidationRule(RegexUtils.PASSWORD_PATTERN, R.string.warning_incorrect_password)
                .build();

        validatorChain = ValidatorChain.Builder.chain()
                .addValidatables(emailEt, passwordEt)
                .build();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.activity_main_button:
                handleButtonClick();
                break;
        }
    }

    private void handleButtonClick(){
        if(validatorChain.canContinueWithRealTimeCheck(true, false)){
            Toast.makeText(this, R.string.correct_input, Toast.LENGTH_SHORT).show();
        }
    }
}
