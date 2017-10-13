/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.plaidapp.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.security.InvalidParameterException;

import in.uncod.android.bypass.Bypass;
import io.plaidapp.R;
import io.plaidapp.databinding.AboutIconBinding;
import io.plaidapp.databinding.AboutLibIntroBinding;
import io.plaidapp.databinding.AboutLibsBinding;
import io.plaidapp.databinding.AboutPlaidBinding;
import io.plaidapp.databinding.ActivityAboutBinding;
import io.plaidapp.databinding.LibraryBinding;
import io.plaidapp.ui.widget.ElasticDragDismissFrameLayout;
import io.plaidapp.util.HtmlUtils;
import io.plaidapp.util.customtabs.CustomTabActivityHelper;
import io.plaidapp.util.glide.GlideApp;
import io.plaidapp.util.glide.GlideRequest;


/**
 * About screen. This displays 3 pages in a ViewPager:
 * – About Plaid
 * – Credit Roman for the awesome icon
 * – Credit libraries
 */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);
        ViewPager pager = binding.pager;
        pager.setAdapter(new AboutPagerAdapter(AboutActivity.this));
        pager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.spacing_normal));
        binding.indicator.setViewPager(pager);

        final ElasticDragDismissFrameLayout draggableFrame = binding.draggableFrame;
        draggableFrame.addListener(
                new ElasticDragDismissFrameLayout.SystemChromeFader(this) {
                    @Override
                    public void onDragDismissed() {
                        // if we drag dismiss downward then the default reversal of the enter
                        // transition would slide content upward which looks weird. So reverse it.
                        if (draggableFrame.getTranslationY() > 0) {
                            getWindow().setReturnTransition(
                                    TransitionInflater.from(AboutActivity.this)
                                            .inflateTransition(R.transition.about_return_downward));
                        }
                        finishAfterTransition();
                    }
                });
    }

    static class AboutPagerAdapter extends PagerAdapter {

        private View aboutPlaid;
        private View aboutIcon;
        private View aboutLibs;
        RecyclerView libsList;

        private final LayoutInflater layoutInflater;
        private final Bypass markdown;
        private final Activity host;
        private final Resources resources;

        AboutPagerAdapter(@NonNull Activity host) {
            this.host = host;
            resources = host.getResources();
            layoutInflater = LayoutInflater.from(host);
            markdown = new Bypass(host, new Bypass.Options());
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            View layout = getPage(position, collection);
            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        private View getPage(int position, ViewGroup parent) {
            switch (position) {
                case 0:
                    if (aboutPlaid == null) {
                        AboutPlaidBinding binding = AboutPlaidBinding.inflate(layoutInflater);
                        aboutPlaid = binding.getRoot();
                        TextView plaidDescription = binding.aboutDescription;
                        // fun with spans & markdown
                        CharSequence about0 = markdown.markdownToSpannable(resources
                                .getString(R.string.about_plaid_0), plaidDescription, null);
                        SpannableString about1 = new SpannableString(
                                resources.getString(R.string.about_plaid_1));
                        about1.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        SpannableString about2 = new SpannableString(markdown.markdownToSpannable
                                (resources.getString(R.string.about_plaid_2),
                                        plaidDescription, null));
                        about2.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        SpannableString about3 = new SpannableString(markdown.markdownToSpannable
                                (resources.getString(R.string.about_plaid_3),
                                        plaidDescription, null));
                        about3.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                                0, about3.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        CharSequence desc = TextUtils.concat(about0, "\n\n", about1, "\n", about2,
                                "\n\n", about3);
                        HtmlUtils.setTextWithNiceLinks(plaidDescription, desc);
                    }
                    return aboutPlaid;
                case 1:
                    if (aboutIcon == null) {
                        AboutIconBinding binding = AboutIconBinding.inflate(layoutInflater);
                        aboutIcon = binding.getRoot();
                        TextView iconDescription = binding.iconDescription;
                        CharSequence icon0 = resources.getString(R.string.about_icon_0);
                        CharSequence icon1 = markdown.markdownToSpannable(resources
                                .getString(R.string.about_icon_1), iconDescription, null);
                        CharSequence iconDesc = TextUtils.concat(icon0, "\n", icon1);
                        HtmlUtils.setTextWithNiceLinks(iconDescription, iconDesc);
                    }
                    return aboutIcon;
                case 2:
                    if (aboutLibs == null) {
                        AboutLibsBinding binding = AboutLibsBinding.inflate(layoutInflater);
                        aboutLibs = binding.getRoot();
                        libsList = binding.libsList;
                        libsList.setAdapter(new LibraryAdapter(host));
                    }
                    return aboutLibs;
            }
            throw new InvalidParameterException();
        }
    }

    private static class LibraryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_INTRO = 0;
        private static final int VIEW_TYPE_LIBRARY = 1;
        static final Library[] libs = {
                new Library("Android support libraries",
                        "The Android support libraries offer a number of features that are not built into the framework.",
                        "https://developer.android.com/topic/libraries/support-library",
                        "https://developer.android.com/images/android_icon_125.png",
                        false),
                new Library("ButterKnife",
                        "Bind Android views and callbacks to fields and methods.",
                        "http://jakewharton.github.io/butterknife/",
                        "https://avatars.githubusercontent.com/u/66577",
                        true),
                new Library("Bypass",
                        "Skip the HTML, Bypass takes markdown and renders it directly.",
                        "https://github.com/Uncodin/bypass",
                        "https://avatars.githubusercontent.com/u/1072254",
                        true),
                new Library("Glide",
                        "An image loading and caching library for Android focused on smooth scrolling.",
                        "https://github.com/bumptech/glide",
                        "https://avatars.githubusercontent.com/u/423539",
                        false),
                new Library("JSoup",
                        "Java HTML Parser, with best of DOM, CSS, and jquery.",
                        "https://github.com/jhy/jsoup/",
                        "https://avatars.githubusercontent.com/u/76934",
                        true),
                new Library("OkHttp",
                        "An HTTP & HTTP/2 client for Android and Java applications.",
                        "http://square.github.io/okhttp/",
                        "https://avatars.githubusercontent.com/u/82592",
                        false),
                new Library("Retrofit",
                        "A type-safe HTTP client for Android and Java.",
                        "http://square.github.io/retrofit/",
                        "https://avatars.githubusercontent.com/u/82592",
                        false)};

        final Activity host;

        LibraryAdapter(Activity host) {
            this.host = host;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_INTRO:
                    return new LibraryIntroHolder(AboutLibIntroBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent, false));
                case VIEW_TYPE_LIBRARY:
                    return createLibraryHolder(parent);
            }
            throw new InvalidParameterException();
        }

        private @NonNull LibraryHolder createLibraryHolder(ViewGroup parent) {
            final LibraryBinding libraryBinding = LibraryBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent, false);
            final LibraryHolder holder = new LibraryHolder(libraryBinding, host);
            return holder;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == VIEW_TYPE_LIBRARY) {
                ((LibraryHolder) holder).bind(libs[position - 1]);
            } else {
                ((LibraryIntroHolder)holder).bind();
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_INTRO : VIEW_TYPE_LIBRARY;
        }

        @Override
        public int getItemCount() {
            return libs.length + 1; // + 1 for the static intro view
        }
    }

    public static class LibraryHolder extends RecyclerView.ViewHolder {
        private final LibraryBinding libraryBinding;
        private Activity host;

        LibraryHolder(LibraryBinding binding, Activity host) {
            super(binding.getRoot());
            this.libraryBinding = binding;
            this.host = host;
        }

        public void bind(final Library lib) {
            libraryBinding.setLibrary(lib);
            libraryBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LibraryHolder.this.onLibraryLinkClick(lib.link);
                }
            });
            libraryBinding.libraryLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LibraryHolder.this.onLibraryLinkClick(lib.link);
                }
            });
            libraryBinding.executePendingBindings();
        }

        public void onLibraryLinkClick(String link) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            CustomTabActivityHelper.openCustomTab(
                    host,
                    new CustomTabsIntent.Builder()
                            .setToolbarColor(ContextCompat.getColor(host, R.color.primary))
                            .addDefaultShareMenuItem()
                            .build(), Uri.parse(link));
        }

        @BindingAdapter({"imageUrl", "circleCrop"})
        public static void setAvatar(ImageView imageView, String url, boolean isCircleCropped) {
            GlideRequest<Drawable> request = GlideApp.with(imageView.getContext())
                    .load(url)
                    .placeholder(R.drawable.avatar_placeholder);
            if (isCircleCropped) {
                request.circleCrop();
            }
            request.into(imageView);
        }
    }

    static class LibraryIntroHolder extends RecyclerView.ViewHolder {

        private final AboutLibIntroBinding aboutLibIntroBinding;
        TextView intro;

        LibraryIntroHolder(AboutLibIntroBinding binding) {
            super(binding.getRoot());
            aboutLibIntroBinding = binding;
            intro = (TextView) itemView;
        }

        public void bind() {
            aboutLibIntroBinding.executePendingBindings();
        }
    }

    /**
     * Models an open source library we want to credit
     */
    public static class Library {
        public final String name;
        public final String link;
        public final String description;
        public final String imageUrl;
        public final boolean circleCrop;

        Library(String name, String description, String link, String imageUrl, boolean circleCrop) {
            this.name = name;
            this.description = description;
            this.link = link;
            this.imageUrl = imageUrl;
            this.circleCrop = circleCrop;
        }
    }

}
