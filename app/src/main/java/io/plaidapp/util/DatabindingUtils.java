package io.plaidapp.util;


import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.view.View;

public class DatabindingUtils {
    public static class LoadingState {
        public final ObservableBoolean isLoading = new ObservableBoolean();

        public LoadingState() {
        }
    }

    @BindingAdapter("visibility")
    public static void setVisibility(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
