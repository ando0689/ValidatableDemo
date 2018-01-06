package test.andranik.validatabledemo.validatable;

import android.util.SparseBooleanArray;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import test.andranik.validatabledemo.utils.ExceptionTracker;

/**
 * Created by andranik on 3/21/17.
 */

public class ValidatorChain {

    private List<Validatable> validatables;
    private SparseBooleanArray validStates;

    private Consumer<Boolean> allValidActionListener;

    private ValidatorChain(List<Validatable> validatables, Consumer<Boolean> allValidActionListener){
        this.validatables = validatables;
        this.allValidActionListener = allValidActionListener;
        this.validStates = new SparseBooleanArray(validatables.size());

        init();
    }

    private void init(){
        for (Validatable validatable : validatables){
            validStates.put(validatable.getIdVl(), validatable.canContinue(false));
            BiConsumer<Integer, Boolean> validationCompleteEvent = (integer, aBoolean) -> {
                validStates.put(integer, aBoolean);

                checkAllValidAndFireEvent();
            };
            validatable.getValidatorVl().setValidationCompleteEvent(validationCompleteEvent);
        }
    }

    private void checkAllValidAndFireEvent(){
        if(allValidActionListener == null){ return; }

        for (int i = 0; i < validStates.size(); i++){
            if(!validStates.valueAt(i)){
                fireAllValidEvent(false);
                return;
            }
        }

        fireAllValidEvent(true);
    }

    private void fireAllValidEvent(boolean allValid){
        try {
            allValidActionListener.accept(allValid);
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
        }
    }

    /////////////////////////// Public interface

    public boolean canContinue(){
        for (int i = 0; i < validStates.size(); i++){
            if(!validStates.valueAt(i)){
                return false;
            }
        }

        return true;
    }

    public boolean canContinueWithRealTimeCheck(boolean showError, boolean checkAll){
        boolean canContinue = true;

        for (Validatable validatable : validatables){
            if(!validatable.canContinue(showError)){
                canContinue = false;

                if(!checkAll){ break; }
            }
        }

        return canContinue;
    }

    public static class Builder{
        private List<Validatable> validatables;
        private Consumer<Boolean> allValidActionListener;

        private Builder(Consumer<Boolean> allValidActionListener) {
            this.allValidActionListener = allValidActionListener;
            this.validatables = new ArrayList<>();
        }

        private Builder(){
            this(null);
        }

        public static Builder forListener(Consumer<Boolean> allValidActionListener){
            return new Builder(allValidActionListener);
        }

        public static Builder chain(){
            return new Builder();
        }

        public Builder addValidatable(Validatable validatable){
            if(!validatables.contains(validatable)) {
                validatables.add(validatable);
            }
            return this;
        }

        public Builder addValidatables(Validatable... vls){
            for(Validatable validatable : vls){
                if(!validatables.contains(validatable)) {
                    validatables.add(validatable);
                }
            }

            return this;
        }

        public ValidatorChain build(){
            return new ValidatorChain(validatables, allValidActionListener);
        }
    }

}
