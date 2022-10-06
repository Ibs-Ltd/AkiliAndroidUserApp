package com.elluminati.eber;

import static com.elluminati.eber.utils.Const.MESSAGE_CODE_USER_EXIST;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.elluminati.eber.components.CustomCountryDialog;
import com.elluminati.eber.components.CustomDialogEnable;
import com.elluminati.eber.components.MyFontEdittextView;
import com.elluminati.eber.components.MyFontTextView;
import com.elluminati.eber.models.datamodels.Country;
import com.elluminati.eber.models.responsemodels.VerificationResponse;
import com.elluminati.eber.models.singleton.CurrentTrip;
import com.elluminati.eber.parse.ApiClient;
import com.elluminati.eber.parse.ApiInterface;
import com.elluminati.eber.utils.AppLog;
import com.elluminati.eber.utils.Const;
import com.elluminati.eber.utils.LocationHelper;
import com.elluminati.eber.utils.Utils;
import com.facebook.CallbackManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseAppCompatActivity implements LocationHelper.OnLocationReceived  {

    private MyFontTextView tvCountryCode, ivGetMovingWith;
    private LocationHelper locationHelper;
    private Location currentLocation;
    private MyFontEdittextView etContactNumber;
    private ArrayList<Country> codeArrayList;
    private CustomCountryDialog customCountryDialog;
    private Country country;
    private FloatingActionButton btnGetOtp;
    private EditText numberEdittext;
    TextView continueLogin;
    private int phoneNumberLength, phoneNumberMinLength;

    private CustomDialogEnable customDialogEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindowAnimations();
        setContentView(R.layout.activity_login);
        codeArrayList = parseContent.getRawCountryCodeList();
        CurrentTrip.getInstance().setCountryCodes(codeArrayList);

        etContactNumber = findViewById(R.id.etContactNumber);
        etContactNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        loadExtraData();
        initToolBar();
        // change toolbar color here
        toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.color_white, null));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setElevation(0);
        }
        setTitleOnToolbar("");
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.color_black), PorterDuff.Mode.SRC_ATOP);
        locationHelper = new LocationHelper(this);
        locationHelper.setLocationReceivedLister(this);

        tvCountryCode = findViewById(R.id.tvCountryCode);
        ivGetMovingWith = findViewById(R.id.ivGetMovingWith);
        continueLogin = findViewById(R.id.continueLogin);
        numberEdittext = findViewById(R.id.numberEdittext);
        ivGetMovingWith.setText(getString(R.string.text_get_moving_with) + " " + getString(R.string.app_name));
        tvCountryCode.setOnClickListener(this);

        btnGetOtp = findViewById(R.id.btnGetOtp);
        btnGetOtp.setOnClickListener(this);
        etContactNumber.setLongClickable(false);
        etContactNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        inItId();
        setCountry(0);
        continueLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyBord();
                if (isValidate()) {
                    verifyUserToServer(country, numberEdittext.getText().toString().trim());
                }
            }
        });
    }

    private void inItId() {
        TextView countryCode = (TextView) findViewById(R.id.countryCode);

        countryCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCountryCodeDialog();
            }
        });
    }


    private void loadExtraData() {
        if (getIntent().getExtras() != null) {
            country = getIntent().getExtras().getParcelable(Const.Params.COUNTRY);
//            phoneNumberLength = country.getPhoneNumberLength();
//            phoneNumberMinLength = country.getPhoneNumberMinLength();
//            setContactNoLength(phoneNumberLength);
        }

    }

    private void setContactNoLength(int length) {
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(length);
        etContactNumber.setFilters(FilterArray);
    }

    @Override
    protected boolean isValidate() {
        String msg = null;
        if (TextUtils.isEmpty(numberEdittext.getText().toString().trim())) {
            msg = getString(R.string.msg_enter_number);
            numberEdittext.setError(msg);
            numberEdittext.requestFocus();
        } else if (!Utils.isValidPhoneNumber(numberEdittext.getText().toString(), preferenceHelper.getMinimumPhoneNumberLength(), preferenceHelper.getMaximumPhoneNumberLength())) {
            msg = getString(R.string.msg_enter_valid_number);
            numberEdittext.requestFocus();
            numberEdittext.setError(msg);
        }

        return TextUtils.isEmpty(msg);
    }

    @Override
    public void goWithBackArrow() {
        hideKeyBord();
        onBackPressed();
    }


    private void openCountryCodeDialog() {

        customCountryDialog = null;
        customCountryDialog = new CustomCountryDialog(this, CurrentTrip.getInstance().getCountryCodes()) {
            @Override
            public void onSelect(int position, ArrayList<Country> filterList) {
                country = filterList.get(position);
                tvCountryCode.setText(country.getCountryPhoneCode());
//                phoneNumberLength = country.getPhoneNumberLength();
//                phoneNumberMinLength = country.getPhoneNumberMinLength();
//                setContactNoLength(phoneNumberLength);
                customCountryDialog.dismiss();
            }
        };
        customCountryDialog.show();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvCountryCode:
                openCountryCodeDialog();
                break;
            case R.id.btnGetOtp:
                hideKeyBord();
                if (isValidate()) {
                    verifyUserToServer(country, etContactNumber.getText().toString().trim());
                }
                break;
            default:

                break;
        }
    }

    private void verifyUserToServer(final Country country, String contactNumber) {
        Utils.showCustomProgressDialog(this, getResources().getString(R.string.msg_loading), false, null);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Const.Params.PHONE, contactNumber);
            jsonObject.put(Const.Params.COUNTRY_PHONE_CODE, country.getCountryPhoneCode());
            Call<VerificationResponse> call = ApiClient.getClient().create(ApiInterface.class).checkUserRegistered(ApiClient.makeJSONRequestBody(jsonObject));
            call.enqueue(new Callback<VerificationResponse>() {
                @Override
                public void onResponse(Call<VerificationResponse> call, Response<VerificationResponse> response) {
                    if (parseContent.isSuccessful(response)) {
                        if (response.body().isSuccess()) {
                            Utils.hideCustomProgressDialog();
                            if (!TextUtils.isEmpty(response.body().getMessage())) {
                                if (response.body().getMessage().equalsIgnoreCase(MESSAGE_CODE_USER_EXIST)) {
                                    // go to password enter screen
                                    Intent passwordIntent = new Intent(LoginActivity.this, EnterPasswordActivity.class);
                                    passwordIntent.putExtra(Const.Params.COUNTRY, country);
                                    passwordIntent.putExtra(Const.Params.PHONE, contactNumber.toString().trim());
                                    goToEnterPasswordActivity(passwordIntent);
                                }
                            } else {
                                if (response.body().isUserSms()) {
                                    Intent otpIntent = new Intent(LoginActivity.this, OtpVerifyActivity.class);
                                    otpIntent.putExtra(Const.Params.COUNTRY, country);
                                    otpIntent.putExtra(Const.Params.PHONE, etContactNumber.getText().toString().trim());
                                    otpIntent.putExtra(Const.Params.IS_OPEN_FOR_RESULT, false);
                                    otpIntent.putExtra(Const.Params.OTP_FOR_SMS, response.body().getOtpForSMS());
                                    otpIntent.putExtra(Const.Params.IS_VERIFIED, false);
                                    goToOtpVerifyActivity(otpIntent);
                                } else {
                                    // go to register screen.
                                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                                    intent.putExtra(Const.Params.PHONE, etContactNumber.getText().toString().trim());
                                    intent.putExtra(Const.Params.IS_VERIFIED, true);
                                    intent.putExtra(Const.Params.LOGIN_BY, Const.MANUAL);
                                    intent.putExtra(Const.Params.COUNTRY, country);
                                    goToRegisterActivity(intent);
                                }
                            }


                        } else {
                            Utils.hideCustomProgressDialog();
                            Utils.showErrorToast(response.body().getErrorCode(), LoginActivity
                                    .this);
                        }

                    }

                }

                @Override
                public void onFailure(Call<VerificationResponse> call, Throwable t) {
                    AppLog.handleThrowable(LoginActivity.class.getSimpleName(), t);
                }
            });

        } catch (JSONException e) {
            AppLog.handleException(LoginActivity.class.getSimpleName(), e);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setAdminApprovedListener(this);
        setConnectivityListener(this);
    }


    private void goToEnterPasswordActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void goToRegisterActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onAdminApproved() {

    }

    @Override
    public void onAdminDeclined() {

    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            closedEnableDialogInternet();
        } else {
            openInternetDialog();
        }
    }


    private void setupWindowAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            Transition transition;
            transition = TransitionInflater.from(this).inflateTransition(R.transition.slide_and_changebounds);
            getWindow().setSharedElementEnterTransition(transition);
        }
    }

    protected void goToOtpVerifyActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (currentLocation == null) {
            currentLocation = location;
            if (currentLocation != null) {
                new GetCityAsyncTask().execute();
            } else {
                setCountry(0);
            }
        }
    }

    protected class GetCityAsyncTask extends AsyncTask<String, Void, Address> {

        @Override
        protected Address doInBackground(String... params) {

            Geocoder geocoder = new Geocoder(LoginActivity.this, new Locale("en_US"));
            try {
                List<Address> addressList = geocoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
                if (addressList != null && !addressList.isEmpty()) {
                    return addressList.get(0);
                }

            } catch (IOException | IllegalArgumentException e) {
                AppLog.handleException(TAG, e);
                publishProgress();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            setCountry(0);
        }

        @Override
        protected void onPostExecute(Address address) {
            if (address != null) {
                getCountryCodeList(address.getCountryName() == null ? "" : address.getCountryName());
            }
        }
    }

    private void getCountryCodeList(String country) {
        int countryListSize = codeArrayList.size();
        for (int i = 0; i < countryListSize; i++) {
            if (codeArrayList.get(i).getCountryName().toUpperCase().startsWith(country.toUpperCase())) {
                setCountry(i);
                return;
            }
        }
        setCountry(0);

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Const.PERMISSION_FOR_LOCATION);
        } else {
            locationHelper.onStart();
            loadCountryList();
        }
    }

    private void loadCountryList() {
        locationHelper.getLastLocation(location -> {
            currentLocation = location;
            if (currentLocation != null) {
                new GetCityAsyncTask().execute();
            } else {
                setCountry(0);
            }
        });
    }

    private void setCountry(int position) {
        country = codeArrayList.get(position);
        tvCountryCode.setText(country.getCountryPhoneCode());

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            switch (requestCode) {
                case Const.PERMISSION_FOR_LOCATION:
                    goWithLocationPermission(grantResults);
                    break;
                default:
                    //Do som thing
                    break;
            }
        }


    }

    private void goWithLocationPermission(int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Do the stuff that requires permission...
            loadCountryList();
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                openPermissionDialog();
            } else {
                openPermissionNotifyDialog(Const.PERMISSION_FOR_LOCATION);
            }
        }
    }


    private void openPermissionDialog() {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            return;
        }
        customDialogEnable = new CustomDialogEnable(this, getResources().getString(R.string.msg_reason_for_permission_location), getString(R.string.text_i_am_sure), getString(R.string.text_re_try)) {
            @Override
            public void doWithEnable() {
                ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Const.PERMISSION_FOR_LOCATION);
                closedPermissionDialog();
            }

            @Override
            public void doWithDisable() {
                closedPermissionDialog();
                finishAffinity();
            }
        };
        customDialogEnable.show();
    }

    private void closedPermissionDialog() {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            customDialogEnable.dismiss();
            customDialogEnable = null;

        }
    }

    private void openPermissionNotifyDialog(final int code) {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            return;
        }
        customDialogEnable = new CustomDialogEnable(this, getResources().getString(R.string.msg_permission_notification), getResources().getString(R.string.text_exit_caps), getResources().getString(R.string.text_settings)) {
            @Override
            public void doWithEnable() {
                closedPermissionDialog();
                startActivityForResult(getIntentForPermission(), code);
            }

            @Override
            public void doWithDisable() {
                closedPermissionDialog();
                finishAffinity();
            }
        };
        customDialogEnable.show();

    }


}
