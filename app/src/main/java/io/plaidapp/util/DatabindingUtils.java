package io.plaidapp.util;


import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
public class DatabindingUtils {
    public static class LoadingState {
        public final ObservableBoolean isLoading = new ObservableBoolean();

        public LoadingState() {
        }
    }

}
