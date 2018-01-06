package test.andranik.validatabledemo.validatable;

import android.support.annotation.StringRes;
import android.support.v4.util.Pair;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import test.andranik.validatabledemo.R;
import test.andranik.validatabledemo.utils.ExceptionTracker;
import test.andranik.validatabledemo.utils.RegexUtils;
import test.andranik.validatabledemo.utils.Utils;


/**
 * Created by andranik on 3/20/17.
 */

public class Validator {

    private static final String TAG = "Validator";
    private static final int TIME_DELAY_DEFAULT = 2000;

    private Validatable validatable;

    private BooleanSupplier clause;

    private String errorMessage;

    private int timeDelay;

    private Consumer<Boolean> onFocusAction;
    private Action onCompleteAction;
    private Consumer<Boolean> postCheckAction;
    private BiConsumer<Integer, Boolean> validationCompleteEvent;
    private Consumer<Throwable> errorAction;
    private Function<String, String> preCheckTextModifier;
    private List<Pair<BooleanSupplier, String>> clauses;

    private boolean isRequiredField;

    private boolean ignoreNextClear;
    private boolean isValidationEnabled = true;

    private Disposable disposable;

    private Validator(Validatable validatable,
                      BooleanSupplier clause,
                      List<Pair<BooleanSupplier, String>> clauses,
                      String errorMessage,
                      int timeDelay,
                      Consumer<Boolean> onFocusAction,
                      Action onCompleteAction,
                      Consumer<Boolean> postCheckAction,
                      Consumer<Throwable> errorAction,
                      Function<String, String> preCheckTextModifier,
                      boolean isRequiredField) {

        this.validatable = validatable;
        this.clause = clause;
        this.errorMessage = errorMessage;
        this.timeDelay = timeDelay;
        this.onFocusAction = onFocusAction;
        this.onCompleteAction = onCompleteAction;
        this.postCheckAction = postCheckAction;
        this.errorAction = errorAction;
        this.preCheckTextModifier = preCheckTextModifier;
        this.isRequiredField = isRequiredField;
        this.isValidationEnabled = true;
        this.clauses = clauses;

        initTextChangeListener();
        initFocusChangeListener();
    }

    //////////////////////// Initialization ///////////////////////////////

    private void initFocusChangeListener() {
        validatable.setOnFocusChangeListenerVl((v, hasFocus) -> {
            if (!hasFocus) {
                checkRequiredField(validatable.getTextVl());
                checkClause(validatable.getTextVl());
            }
            doOnFocusAction(hasFocus);
        });
    }

    private void initTextChangeListener() {
       disposable = RxTextView.textChanges(validatable.getTextViewVl())
                .filter(charSequence -> isValidationEnabled)
                .skip(1)
                .map(charSequence -> {
                    if (preCheckTextModifier != null) {
                        return preCheckTextModifier.apply(charSequence.toString());
                    }
                    return charSequence;
                })
                .map(charSequence -> {
                    clearError();
                    return charSequence;
                })
                .debounce(timeDelay, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .filter(charSequence -> validatable.isFocusedVl())
                .subscribe(charSequence -> {
                            boolean canContinue = canContinue(charSequence.toString());
                            doPostCheckAction(canContinue);
                            sendValidationCompleteEvent(canContinue);
                        }
                        , this::doOnErrorAction
                        , this::doOnCompleteAction);
    }

    //////////////////////// Public interface //////////////////////////

    public void setIgnoreNextClear(boolean ignoreNextClear) {
        this.ignoreNextClear = ignoreNextClear;
    }

    public void setValidationEnabled(boolean enabled) {
        isValidationEnabled = enabled;

        if (!enabled) {
            clearError();
        }
    }

    // can be used only by validation chain
    void setValidationCompleteEvent(BiConsumer<Integer, Boolean> validationCompleteEvent) {
        this.validationCompleteEvent = validationCompleteEvent;
    }

    //////////////////////// Callbacks ///////////////////////////////

    private void doOnCompleteAction() {
        if (onCompleteAction == null) {
            return;
        }
        try {
            onCompleteAction.run();
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    private void doOnErrorAction(Throwable t) {
        if (errorAction == null) {
            return;
        }
        try {
            errorAction.accept(t);
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    private void sendValidationCompleteEvent(boolean canContinue) {
        if (validationCompleteEvent == null) {
            return;
        }

        try {
            validationCompleteEvent.accept(validatable.getIdVl(), canContinue);
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    private void doPostCheckAction(boolean canContinue) {
        if (postCheckAction == null) {
            return;
        }

        try {
            postCheckAction.accept(canContinue);
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    private void doOnFocusAction(boolean isFocused) {
        if (onFocusAction == null) {
            return;
        }

        try {
            onFocusAction.accept(isFocused);
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    //////////////////////// Checks ///////////////////////////////

    public boolean canContinue(boolean showError) {
        return canContinue(validatable.getTextVl(), showError);
    }

    private boolean canContinue(String text) {
        return checkRequiredField(text, true) && checkClause(text, true);
    }

    private boolean canContinue(String text, boolean showError) {
        return checkRequiredField(text, showError) && checkClause(text, showError);
    }

    private boolean checkRequiredField(String text) {
        return checkRequiredField(text, true);
    }

    private boolean checkRequiredField(String text, boolean showError) {
        if (!isRequiredField()) {
            return true;
        }

        if (isEmpty(text)) {
            if (showError) validatable.setErrorVl(Utils.getStringFromRes(validatable.getTextViewVl().getContext(), R.string.required_field));
            return false;
        }

        clearError();
        return true;
    }

    private boolean checkClause(String text) {
        return checkClause(text, true);
    }

    private boolean checkClause(String text, boolean showError) {
        if (!hasClause()) {
            return true;
        }

        if (isEmpty(text)) {
            return false;
        }


        if (isSingleClause()) {
            // check for single clause
            if (!isMatchToClause()) {
                if (showError) validatable.setErrorVl(errorMessage);
                return false;
            }
        } else {
            //we have a list of clauses
            if (!isMatchToListOfClauses()){
                if (showError) validatable.setErrorVl(invalidClauseErrorMessage());
                return false;
            }
        }

        clearError();
        return true;
    }

    private boolean isSingleClause() {
        return clause != null;
    }

    private void clearError() {
        if (ignoreNextClear) {
            ignoreNextClear = false;
            return;
        }

        validatable.cleanErrorVl();
    }

    private boolean hasClause() {
        return clause != null || clauses.size() > 0;
    }

    private boolean isRequiredField() {
        return isRequiredField;
    }

    private boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    private boolean isMatchToClause() {
        try {
            return clause.getAsBoolean();
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }

        return false;
    }

    private boolean isMatchToListOfClauses() {
        try {
            for (Pair<BooleanSupplier, String> clause : clauses) {

                if (!clause.first.getAsBoolean()) {
                    // if doesn't matches with clause
                    return false;
                }
            }
            // matches with clause
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String invalidClauseErrorMessage() {
        try {
            for (Pair<BooleanSupplier, String> clause : clauses) {
                if (!clause.first.getAsBoolean()) {
                    // if doesn't matches with clause
                    return clause.second;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public void reset(){
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }

    //////////////////////// CETemplateSectionManager ///////////////////////////////

    public static class Builder {
        private Validatable validatable;

        private BooleanSupplier clause;
        private String errorMessage;

        private List<Pair<BooleanSupplier, String>> clauses = new ArrayList<>();

        private int timeDelay = TIME_DELAY_DEFAULT;

        private Consumer<Boolean> onFocusAction;
        private Action onCompleteAction;
        private Consumer<Boolean> postCheckAction;
        private Consumer<Throwable> errorAction;
        private Function<String, String> preCheckTextModifier;

        private boolean isRequiredField;

        private Builder(Validatable validatable) {
            this.validatable = validatable;
        }

        public static Builder createFor(Validatable validatable) {
            return new Builder(validatable);
        }

        public Builder addValidationRule(BooleanSupplier clause, String errorMessage) {
            if (this.clause != null) {
                // add single clause to list with it's error message
                clauses.add(Pair.create(this.clause, this.errorMessage));
                this.clause = null;
            }
            clauses.add(Pair.create(clause, errorMessage));
            return this;
        }

        public Builder addValidationRule(BooleanSupplier clause, @StringRes int errorMessageRes) {
            addValidationRule(clause, Utils.getStringFromRes(validatable.getTextViewVl().getContext(), errorMessageRes));
            return this;
        }

        public Builder addValidationRule(String regex, String errorMessage) {
            addValidationRule(() -> RegexUtils.isMatchToRegex(validatable.getTextVl(), regex), errorMessage);
            return this;
        }

        public Builder addValidationRule(String regex, @StringRes int errorMessageRes) {
            addValidationRule(regex, Utils.getStringFromRes(validatable.getTextViewVl().getContext(), errorMessageRes));
            return this;
        }

        /**
         * clears all clauses, and set current clause
         */
        public Builder setValidationRule(BooleanSupplier clause, String errorMessage) {
            this.clauses.clear();
            this.clause = clause;
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder setValidationRule(BooleanSupplier clause, @StringRes int errorMessageRes) {
            setValidationRule(clause, Utils.getStringFromRes(validatable.getTextViewVl().getContext(), errorMessageRes));
            return this;
        }

        public Builder setValidationRule(String regex, String errorMessage) {
            this.clauses.clear();
            this.clause = () -> RegexUtils.isMatchToRegex(validatable.getTextVl(), regex);
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder setValidationRule(String regex, @StringRes int errorMessageRes) {
            setValidationRule(regex, Utils.getStringFromRes(validatable.getTextViewVl().getContext(), errorMessageRes));
            return this;
        }

        public Builder setTimeDelay(int timeDelay) {
            this.timeDelay = timeDelay;
            return this;
        }

        public Builder setOnFocusAction(Consumer<Boolean> onFocusAction) {
            this.onFocusAction = onFocusAction;
            return this;
        }

        public Builder setOnCompleteAction(Action onCompleteAction) {
            this.onCompleteAction = onCompleteAction;
            return this;
        }

        public Builder setPostCheckAction(Consumer<Boolean> postCheckAction) {
            this.postCheckAction = postCheckAction;
            return this;
        }

        public Builder setErrorAction(Consumer<Throwable> errorAction) {
            this.errorAction = errorAction;
            return this;
        }

        public Builder setPreCheckTextModifier(Function<String, String> preCheckTextModifier) {
            this.preCheckTextModifier = preCheckTextModifier;
            return this;
        }

        public Builder setRequiredField(boolean requiredField) {
            isRequiredField = requiredField;
            return this;
        }

        public Validator build() {
            Validator validator = new Validator(
                    validatable,
                    clause,
                    clauses,
                    errorMessage,
                    timeDelay,
                    onFocusAction,
                    onCompleteAction,
                    postCheckAction,
                    errorAction,
                    preCheckTextModifier,
                    isRequiredField
            );

            validatable.setValidatorVl(validator);

            return validator;
        }
    }
}
