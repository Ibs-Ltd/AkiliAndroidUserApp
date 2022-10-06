package com.elluminati.eber;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.elluminati.eber.components.CustomDialogBigLabel;
import com.elluminati.eber.components.CustomDialogEnable;
import com.elluminati.eber.components.MyAppTitleFontTextView;
import com.elluminati.eber.components.MyFontButton;
import com.elluminati.eber.interfaces.AdminApprovedListener;
import com.elluminati.eber.interfaces.ConnectivityReceiverListener;
import com.elluminati.eber.models.responsemodels.GenerateFirebaseTokenResponse;
import com.elluminati.eber.models.responsemodels.IsSuccessResponse;
import com.elluminati.eber.models.singleton.AddressUtils;
import com.elluminati.eber.models.singleton.CurrentTrip;
import com.elluminati.eber.parse.ApiClient;
import com.elluminati.eber.parse.ApiInterface;
import com.elluminati.eber.parse.ParseContent;
import com.elluminati.eber.utils.AppLog;
import com.elluminati.eber.utils.Const;
import com.elluminati.eber.utils.CurrencyHelper;
import com.elluminati.eber.utils.LanguageHelper;
import com.elluminati.eber.utils.NetworkHelper;
import com.elluminati.eber.utils.PreferenceHelper;
import com.elluminati.eber.utils.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by elluminati on 10-05-2016.
 */
public abstract class BaseAppCompatActivity extends AppCompatActivity implements View.OnClickListener, ConnectivityReceiverListener, AdminApprovedListener {


    private final AppReceiver appReceiver = new AppReceiver();
    public MyAppTitleFontTextView tvTitleToolbar;
    public MyFontButton btnToolbar;
    public ParseContent parseContent;
    public PreferenceHelper preferenceHelper;
    public CurrentTrip currentTrip;
    public AddressUtils addressUtils;
    public String TAG = this.getClass().getSimpleName();
    public CurrencyHelper currencyHelper;
    protected Toolbar toolbar;
    protected ActionBar actionBar;
    private ImageView ivToolbarIcon;
    private CustomDialogBigLabel customDialogExit;
    private CustomDialogEnable customDialogEnableInternet;
    private ConnectivityReceiverListener connectivityReceiverListener;
    private AdminApprovedListener adminApprovedListener;
    private NetworkHelper networkHelper;
    public FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(ResourcesCompat.getColor(getResources(), R.color.color_app_status_bar, null));
        parseContent = ParseContent.getInstance();
        parseContent.getContext(getApplicationContext());
        parseContent.initDateTimeFormat();
        currentTrip = CurrentTrip.getInstance();
        addressUtils = AddressUtils.getInstance();
        preferenceHelper = PreferenceHelper.getInstance(this);
        currencyHelper = CurrencyHelper.getInstance(this);
        adjustFontScale(getResources().getConfiguration());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Const.ACTION_DECLINE);
        intentFilter.addAction(Const.ACTION_APPROVED);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            networkHelper = NetworkHelper.getInstance();
            networkHelper.initConnectivityManager(this);
        } else {
            intentFilter.addAction(Const.NETWORK_ACTION);
        }
        registerReceiver(appReceiver, intentFilter);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(appReceiver);
        if (ivToolbarIcon != null) {
            ivToolbarIcon.setOnClickListener(null);
        }
        Utils.hideCustomProgressDialog();
        super.onDestroy();

    }

    /**
     * Use for initialize toolbar .
     * It call in every activity which extends this activity.
     */
    protected void initToolBar() {
        toolbar = findViewById(R.id.appToolbar);
        tvTitleToolbar = toolbar.findViewById(R.id.tvToolbarTitle);
        ivToolbarIcon = toolbar.findViewById(R.id.ivToolbarIcon);
        btnToolbar = toolbar.findViewById(R.id.btnToolBar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goWithBackArrow();
            }
        });
    }

    public void setToolbarNavigationIcon(int resId) {
        if (toolbar != null) {
            toolbar.setNavigationIcon(AppCompatResources.getDrawable(this, resId));
        }
    }

    /**
     * Use for set toolbar title.
     *
     * @param title
     */
    public void setTitleOnToolbar(String title) {
        tvTitleToolbar.setText(title);
        btnToolbar.setVisibility(View.GONE);
        ivToolbarIcon.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    public void setToolbarBackgroundWithElevation(boolean isDrawable, int backgroundId, int elevationId) {
        if (toolbar != null) {
            if (isDrawable) {
                toolbar.setBackground(AppCompatResources.getDrawable(this, backgroundId));
            } else {
                toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), backgroundId, null));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (elevationId > 0) {
                    toolbar.setElevation(getResources().getDimensionPixelOffset(elevationId));
                } else {
                    toolbar.setElevation(0);
                }


            }
        }
    }

    public void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
    }

    /**
     * Use for set right side icon like fare estimate.
     *
     * @param drawable
     * @param onClickListener
     */
    public void setToolbarRightSideIcon(Drawable drawable, View.OnClickListener onClickListener) {
        if (btnToolbar != null) {
            btnToolbar.setVisibility(View.GONE);
        }
        if (ivToolbarIcon != null) {
            if (drawable != null) {
                ivToolbarIcon.setVisibility(View.VISIBLE);
                ivToolbarIcon.setImageDrawable(drawable);
                ivToolbarIcon.setOnClickListener(onClickListener);
            } else {
                ivToolbarIcon.setVisibility(View.GONE);
            }
        }

    }

    public void hideToolbarRightSideIcon(boolean isHide) {
        if (ivToolbarIcon != null) {
            ivToolbarIcon.setVisibility(isHide ? View.GONE : View.VISIBLE);
        }


    }


    /**
     * Use for set right side button like invoice , cancel trip , submit invoice , etc.
     *
     * @param title
     * @param onClickListener
     */
    public void setToolbarRightSideButton(String title, View.OnClickListener onClickListener) {
        ivToolbarIcon.setVisibility(View.GONE);
        btnToolbar.setVisibility(View.VISIBLE);
        btnToolbar.setOnClickListener(onClickListener);
        btnToolbar.setText(title);
    }

    protected void openExitDialog(final Activity activity) {

        customDialogExit = new CustomDialogBigLabel(this, getString(R.string.text_exit_caps), getString(R.string.msg_are_you_sure), getString(R.string.text_yes), getString(R.string.text_no)) {
            @Override
            public void positiveButton() {
                customDialogExit.dismiss();
                activity.finishAffinity();
            }

            @Override
            public void negativeButton() {
                customDialogExit.dismiss();
            }
        };
        customDialogExit.show();
    }

    protected void openInternetDialog() {
        if (customDialogEnableInternet != null && customDialogEnableInternet.isShowing()) {
            return;
        }

        customDialogEnableInternet = new CustomDialogEnable(this, getString(R.string.msg_internet_enable), getString(R.string.text_no), getString(R.string.text_yes)) {
            @Override
            public void doWithEnable() {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }

            @Override
            public void doWithDisable() {
                closedEnableDialogInternet();
                finishAffinity();
            }
        };

        if (!this.isFinishing()) customDialogEnableInternet.show();

    }

    protected void closedEnableDialogInternet() {
        if (customDialogEnableInternet != null && customDialogEnableInternet.isShowing()) {
            customDialogEnableInternet.dismiss();
            customDialogEnableInternet = null;

        }
    }


    protected void goToMainDrawerActivity() {
        Intent mainDrawerIntent = new Intent(this, MainDrawerActivity.class);
        mainDrawerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainDrawerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainDrawerIntent);
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
    }

    protected void goToReferralEnterActivity() {
        Intent referralIntent = new Intent(this, ReferralEnterActivity.class);
        referralIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        referralIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(referralIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void goToMainActivity() {
        Intent sigInIntent = new Intent(this, MainActivity.class);
        sigInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sigInIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sigInIntent);
        finishAffinity();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    public void goToLoginActivity() {
        Intent sigInIntent = new Intent(this, LoginActivity.class);
        sigInIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        sigInIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sigInIntent);
        finishAffinity();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void goToSplashActivity() {
        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    protected abstract boolean isValidate();


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

    public void setStatusbarColor(int resId) {
        Window window = this.getWindow();
        window.setStatusBarColor(ResourcesCompat.getColor(getResources(), resId, null));
    }


    public int getStatusbarHeight() {
        int height = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height = getResources().getDimensionPixelSize(resourceId);
        }
        return height;
    }


    public abstract void goWithBackArrow();

    public void contactUsWithEmail() {
        Uri gmmIntentUri = Uri.parse("mailto:" + preferenceHelper.getContactUsEmail() + "?subject=" + "Request to Admin " + "&body=" + "Hello sir");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.gm");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Utils.showToast(getResources().getString(R.string.msg_google_mail_app_not_installed), this);
        }
    }

    public void logOut(boolean isForServer) {
        Utils.showCustomProgressDialog(this, getResources().getString(R.string.msg_waiting_for_log_out), false, null);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Const.Params.USER_ID, preferenceHelper.getUserId());
            jsonObject.put(Const.Params.TOKEN, preferenceHelper.getSessionToken());

            Call<IsSuccessResponse> call = ApiClient.getClient().create(ApiInterface.class).logout(ApiClient.makeJSONRequestBody(jsonObject));
            call.enqueue(new Callback<IsSuccessResponse>() {
                @Override
                public void onResponse(Call<IsSuccessResponse> call, Response<IsSuccessResponse> response) {
                    if (parseContent.isSuccessful(response)) {
                        if (response.body().isSuccess()) {
                            Utils.hideCustomProgressDialog();
                            Utils.showMessageToast(response.body().getMessage(), BaseAppCompatActivity.this);
                            preferenceHelper.logout();// clear session token
                            mAuth.signOut();
                            if (isForServer) {
                                goToSplashActivity();
                            } else {
                                goToMainActivity();
                            }
                        } else {
                            Utils.hideCustomProgressDialog();
                            Utils.showErrorToast(response.body().getErrorCode(), BaseAppCompatActivity.this);
                        }
                    }


                }

                @Override
                public void onFailure(Call<IsSuccessResponse> call, Throwable t) {
                    AppLog.handleThrowable(BaseAppCompatActivity.class.getSimpleName(), t);
                }
            });

        } catch (JSONException e) {
            AppLog.handleException(Const.Tag.MAIN_DRAWER_ACTIVITY, e);
        }
    }


    public void goWithAdminApproved() {
        //preferenceHelper.putIsApproved(Const.IS_APPROVED);
        //closedUnderReviewDialog();
        //goToMainDrawerActivity();
    }

    public void goWithAdminDecline() {
        //preferenceHelper.putIsApproved(Const.IS_DECLINE);
        //  openUnderReviewDialog();
    }


    public void setConnectivityListener(ConnectivityReceiverListener listener) {
        connectivityReceiverListener = listener;
        if (networkHelper != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkHelper.setNetworkAvailableListener(connectivityReceiverListener);
        }
    }

    /**
     * Use for get app version which define in app level gradle file
     *
     * @return
     */
    public String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            AppLog.handleException(BaseAppCompatActivity.class.getName(), e);
        }
        return null;
    }

    public void setAdminApprovedListener(AdminApprovedListener listener) {
        adminApprovedListener = listener;
    }

    protected Intent getIntentForPermission() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        return intent;
    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * Remove  comment when you live eber in palyStore
         */
        //        if (!TextUtils.equals(Locale.getDefault().getLanguage(), Const.EN)) {
        //            openLanguageDialog();
        //        }
    }

    private void openLanguageDialog() {

        final CustomDialogBigLabel customDialogBigLabel = new CustomDialogBigLabel(this, getResources().getString(R.string.text_attention), getResources().getString(R.string.meg_language_not_an_english), getResources().getString(R.string.text_settings), getResources().getString(R.string.text_exit_caps)) {
            @Override
            public void positiveButton() {
                startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS));
                dismiss();
            }

            @Override
            public void negativeButton() {
                dismiss();
                finishAffinity();

            }
        };
        customDialogBigLabel.show();
    }

    protected void goToAddPaymentActivity() {
        Intent addPaymentIntent = new Intent(this, PaymentActivity.class);
        addPaymentIntent.putExtra(Const.IS_CLICK_INSIDE_DRAWER, false);
        addPaymentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        addPaymentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(addPaymentIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void goToDocumentActivity(boolean isClickInsideDrawer) {

        Intent docIntent = new Intent(this, DocumentActivity.class);
        docIntent.putExtra(Const.IS_CLICK_INSIDE_DRAWER, isClickInsideDrawer);
        startActivity(docIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

    }

    /**
     * Use for adjust font scale.
     * If device have big font size from setting then also it display in normal size.
     *
     * @param configuration
     */
    public void adjustFontScale(Configuration configuration) {
        if (configuration.fontScale > Const.DEFAULT_FONT_SCALE) {
            configuration.fontScale = Const.DEFAULT_FONT_SCALE;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.fontScale * metrics.density;
            getBaseContext().getResources().updateConfiguration(configuration, metrics);
        }
    }


    public void hideKeyBord() {
        InputMethodManager inm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    public void restartApp() {
        startActivity(new Intent(this, SplashScreenActivity.class));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapper(newBase, PreferenceHelper.getInstance(newBase).getLanguageCode()));
    }

    /**
     * this method used to make decision after login or register for screen transaction with
     * specific preference
     */
    public void moveWithUserSpecificPreference() {
        if (preferenceHelper.getIsHaveReferral() && preferenceHelper.getIsApplyReferral() == Const.FALSE) {
            goToReferralEnterActivity();
        } else if (preferenceHelper.getAllDocUpload() == Const.FALSE) {
            goToDocumentActivity(false);
        } else {
            goToMainDrawerActivity();
        }
    }

    public String validPhoneNumberMessage(int minDigit, int maxDigit) {
        if (maxDigit == minDigit) {
            return getResources().getString(R.string.msg_please_enter_valid_mobile_number, maxDigit);
        } else {
            return getResources().getString(R.string.msg_please_enter_valid_mobile_number_between, minDigit, maxDigit);
        }
    }

    @Override
    public void onBackPressed() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        super.onBackPressed();
    }

    public class AppReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case Const.NETWORK_ACTION:
                        if (connectivityReceiverListener != null) {
                            connectivityReceiverListener.onNetworkConnectionChanged(networkHelper.isInternetConnected());
                        }
                        break;
                    case Const.ACTION_APPROVED:
                        if (adminApprovedListener != null) {
                            adminApprovedListener.onAdminApproved();
                        }
                        break;
                    case Const.ACTION_DECLINE:
                        if (adminApprovedListener != null) {
                            adminApprovedListener.onAdminDeclined();
                        }
                        break;
                    default:

                        break;
                }
            }
        }
    }

    public void generateFirebaseAccessToken() {
        Utils.showCustomProgressDialog(this, "", false, null);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Const.Params.USER_ID, preferenceHelper.getUserId());
            jsonObject.put(Const.Params.TOKEN, preferenceHelper.getSessionToken());
            jsonObject.put(Const.Params.TYPE, Const.USER_UNIQUE_NUMBER);

            Call<GenerateFirebaseTokenResponse> call = ApiClient.getClient().create(ApiInterface.class).generateFirebaseAccessToken(ApiClient.makeJSONRequestBody(jsonObject));
            call.enqueue(new Callback<GenerateFirebaseTokenResponse>() {
                @Override
                public void onResponse(Call<GenerateFirebaseTokenResponse> call, Response<GenerateFirebaseTokenResponse> response) {
                    if (parseContent.isSuccessful(response)) {
                        if (response.body().getSuccess()) {
                            AppLog.Log(BaseAppCompatActivity.class.getSimpleName(), "generateFirebaseAccessToken");
                            Utils.hideCustomProgressDialog();
                            preferenceHelper.putFirebaseUserToken(response.body().getFirebaseToken());
                            signInAnonymously();
                        } else {
                            Utils.hideCustomProgressDialog();
                            Utils.showErrorToast(response.body().getErrorCode(), BaseAppCompatActivity.this);
                        }
                    }


                }

                @Override
                public void onFailure(Call<GenerateFirebaseTokenResponse> call, Throwable t) {
                    AppLog.handleThrowable(BaseAppCompatActivity.class.getSimpleName(), t);
                }
            });

        } catch (JSONException e) {
            AppLog.handleException(Const.Tag.MAIN_DRAWER_ACTIVITY, e);
        }
    }

    private void signInAnonymously() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            if (!TextUtils.isEmpty(preferenceHelper.getFirebaseUserToken())) {
                mAuth.signInWithCustomToken(preferenceHelper.getFirebaseUserToken()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            Utils.showToast("Authentication failed.", BaseAppCompatActivity.this);
                        }
                    }
                });
            }
        }
    }
}