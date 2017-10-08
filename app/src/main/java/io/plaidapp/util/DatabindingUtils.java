package io.plaidapp.util;


import android.databinding.BindingAdapter;
import android.databinding.ObservableBoolean;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.plaidapp.R;
import io.plaidapp.util.glide.GlideApp;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

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

    @BindingAdapter({"scrimBackground"})
    public static void setScrimBackground(ViewGroup viewGroup, int numStops) {
        viewGroup.setBackground(ScrimUtil.makeCubicGradientScrimDrawable(
                ContextCompat.getColor(viewGroup.getContext(), R.color.scrim),
                numStops, Gravity.BOTTOM));
    }

    @BindingAdapter({"placeholder","loggedInAvatar"})
    public static void setLoggedInUserAvatar(ImageView imageView, int placeholder, String url) {
        // need to use app context here as the activity will be destroyed shortly
        GlideApp.with(imageView.getContext().getApplicationContext())
                .load(url)
                .placeholder(placeholder)
                .circleCrop()
                .transition(withCrossFade())
                .into(imageView);
    }
}
