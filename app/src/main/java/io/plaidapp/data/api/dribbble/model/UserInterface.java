package io.plaidapp.data.api.dribbble.model;


import android.support.annotation.DrawableRes;

public interface UserInterface {
    String getUserName();
    String getAvatarUrl();
    @DrawableRes int getAvatarPlaceholder();
}
