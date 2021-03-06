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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.plaidapp.BuildConfig;
import io.plaidapp.R;
import io.plaidapp.data.api.designernews.model.AccessToken;
import io.plaidapp.data.api.designernews.model.User;
import io.plaidapp.data.prefs.DesignerNewsPrefs;
import io.plaidapp.databinding.ActivityDesignerNewsLoginBinding;
import io.plaidapp.databinding.ToastLoggedInConfirmationBinding;
import io.plaidapp.ui.transitions.FabTransform;
import io.plaidapp.ui.transitions.MorphTransform;
import io.plaidapp.util.DatabindingUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DesignerNewsLogin extends Activity {

    private static final int PERMISSIONS_REQUEST_GET_ACCOUNTS = 0;

    boolean isDismissing = false;
    ViewGroup container;
    AutoCompleteTextView username;
    EditText password;
    DesignerNewsPrefs designerNewsPrefs;
    private boolean shouldPromptForPermission = false;
    private ActivityDesignerNewsLoginBinding activityBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityBinding = DataBindingUtil.setContentView(this, R.layout.activity_designer_news_login);
        activityBinding.setHandlers(this);
        
        final DesignerNewsCredentials credentials = new DesignerNewsCredentials();
        activityBinding.setCredentials(credentials);

        DatabindingUtils.LoadingState loadingState = new DatabindingUtils.LoadingState();
        loadingState.isLoading.set(false);
        activityBinding.setLoadingState(loadingState);

        PermissionPrimerModel permissionPrimerModel = new PermissionPrimerModel();
        activityBinding.setPermissionModel(permissionPrimerModel);

        container = activityBinding.container;
        username = activityBinding.username;
        password = activityBinding.password;
        if (!FabTransform.setup(this, container)) {
            MorphTransform.setup(this, container,
                    ContextCompat.getColor(this, R.color.background_light),
                    getResources().getDimensionPixelSize(R.dimen.dialog_corners));
        }

        setupAccountAutocomplete();
        designerNewsPrefs = DesignerNewsPrefs.get(this);
    }

    @Override @SuppressLint("NewApi")
    public void onEnterAnimationComplete() {
        /* Postpone some of the setup steps so that we can run it after the enter transition (if
        there is one). Otherwise we may show the permissions dialog or account dropdown during the
        enter animation which is jarring. */
        if (shouldPromptForPermission) {
            requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS},
                    PERMISSIONS_REQUEST_GET_ACCOUNTS);
            shouldPromptForPermission = false;
        }
        maybeShowAccounts();
    }

    // the primer checkbox messes with focus order so force it
    public boolean onNameEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            password.requestFocus();
            return true;
        }
        return false;
    }

    public boolean onPasswordEditorAction(int actionId, DesignerNewsCredentials credentials) {
        if (actionId == EditorInfo.IME_ACTION_DONE && credentials.hasCredentials.get()) {
            activityBinding.login.performClick();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        dismiss(null);
    }

    @Override @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_GET_ACCOUNTS) {
            TransitionManager.beginDelayedTransition(container);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAccountAutocomplete();
                username.requestFocus();
                username.showDropDown();
            } else {
                // if permission was denied check if we should ask again in the future (i.e. they
                // did not check 'never ask again')
                if (shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS)) {
                    setupPermissionPrimer();
                } else {
                    // denied & shouldn't ask again. deal with it (•_•) ( •_•)>⌐■-■ (⌐■_■)
                    TransitionManager.beginDelayedTransition(container);
                    activityBinding.getPermissionModel().showPermissionPrimer.set(false);
                }
            }
        }
    }

    public void doLogin(DesignerNewsCredentials credentials) {
        TransitionManager.beginDelayedTransition(container);
        activityBinding.getLoadingState().isLoading.set(true);
        getAccessToken(credentials);
    }

    public void signup() {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.designernews.co/users/new")));
    }

    public void dismiss(View view) {
        isDismissing = true;
        setResult(Activity.RESULT_CANCELED);
        finishAfterTransition();
    }

    /**
     * Postpone some of the setup steps so that we can run it after the enter transition
     * (if there is one). Otherwise we may show the permissions dialog or account dropdown
     * during the enter animation which is jarring.
     */
    void finishSetup() {
        if (shouldPromptForPermission) {
            requestPermissions(new String[]{ Manifest.permission.GET_ACCOUNTS },
                    PERMISSIONS_REQUEST_GET_ACCOUNTS);
            shouldPromptForPermission = false;
        }
        maybeShowAccounts();
    }

    public void onNameFocusChange() {
        maybeShowAccounts();
    }

    void maybeShowAccounts() {
        if (username.hasFocus()
                && username.isAttachedToWindow()
                && username.getAdapter() != null
                && username.getAdapter().getCount() > 0) {
            username.showDropDown();
        }
    }

    void showLoggedInUser() {
        final Call<User> authedUser = designerNewsPrefs.getApi().getAuthedUser();
        authedUser.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) return;
                final User user = response.body();
                designerNewsPrefs.setLoggedInUser(user);
                final Toast confirmLogin = new Toast(getApplicationContext());
                ToastLoggedInConfirmationBinding loggedInConfirmation = ToastLoggedInConfirmationBinding.inflate(
                        LayoutInflater.from(DesignerNewsLogin.this), null, false);
                loggedInConfirmation.setUser(user);
                confirmLogin.setView(loggedInConfirmation.getRoot());
                confirmLogin.setGravity(Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0);
                confirmLogin.setDuration(Toast.LENGTH_LONG);
                confirmLogin.show();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(getClass().getCanonicalName(), t.getMessage(), t);
            }
        });
    }

    void showLoginFailed() {
        Snackbar.make(container, R.string.login_failed, Snackbar.LENGTH_SHORT).show();
        TransitionManager.beginDelayedTransition(container);
        activityBinding.getLoadingState().isLoading.set(false);
        password.requestFocus();
    }

    private void getAccessToken(DesignerNewsCredentials credentials) {
        final Call<AccessToken> login = designerNewsPrefs.getApi().login(
                buildLoginParams(credentials.username, credentials.password));
        login.enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    designerNewsPrefs.setAccessToken(DesignerNewsLogin.this, response.body().access_token);
                    showLoggedInUser();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    showLoginFailed();
                }
            }

            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                Log.e(getClass().getCanonicalName(), t.getMessage(), t);
                showLoginFailed();
            }
        });
    }

    private Map<String, String> buildLoginParams(@NonNull String username, @NonNull String password) {
        final Map<String, String> loginParams = new HashMap<>(5);
        loginParams.put("client_id", BuildConfig.DESIGNER_NEWS_CLIENT_ID);
        loginParams.put("client_secret", BuildConfig.DESIGNER_NEWS_CLIENT_SECRET);
        loginParams.put("grant_type", "password");
        loginParams.put("username", username);
        loginParams.put("password", password);
        return loginParams;
    }

    @SuppressLint("NewApi")
    private void setupAccountAutocomplete() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) ==
                PackageManager.PERMISSION_GRANTED) {
            activityBinding.getPermissionModel().showPermissionPrimer.set(false);
            final Account[] accounts = AccountManager.get(this).getAccounts();
            final Set<String> emailSet = new HashSet<>();
            for (Account account : accounts) {
                if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                    emailSet.add(account.name);
                }
            }
            username.setAdapter(new ArrayAdapter<>(this,
                    R.layout.account_dropdown_item, new ArrayList<>(emailSet)));
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.GET_ACCOUNTS)) {
                setupPermissionPrimer();
            } else {
                activityBinding.getPermissionModel().showPermissionPrimer.set(false);
                shouldPromptForPermission = true;
            }
        }
    }

    private void setupPermissionPrimer() {
        activityBinding.getPermissionModel().isChecked.set(false);
        activityBinding.getPermissionModel().showPermissionPrimer.set(true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void onPrimerChecked() {
        requestPermissions(new String[]{ Manifest.permission.GET_ACCOUNTS },
                    PERMISSIONS_REQUEST_GET_ACCOUNTS);
    }

    public static class DesignerNewsCredentials {
        private String username;
        private String password;

        public final ObservableBoolean hasCredentials = new ObservableBoolean();

        public DesignerNewsCredentials() {
        }

        public String getPassword() {
            return password;
        }

        private boolean hasUsernameAndPassword() {
            return !TextUtils.isEmpty(username) && !TextUtils.isEmpty(password);
        }

        public void setPassword(String password) {
            this.password = password;
            hasCredentials.set(hasUsernameAndPassword());
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
            hasCredentials.set(hasUsernameAndPassword());
        }
    }

    public class PermissionPrimerModel {
        public final ObservableBoolean showPermissionPrimer = new ObservableBoolean();
        public final ObservableBoolean isChecked = new ObservableBoolean();
    }
}
