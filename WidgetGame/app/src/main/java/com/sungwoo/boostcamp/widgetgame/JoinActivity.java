package com.sungwoo.boostcamp.widgetgame;

import android.app.ProgressDialog;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sungwoo.boostcamp.widgetgame.CommonUtility.CommonUtility;
import com.sungwoo.boostcamp.widgetgame.Repositories.CommonRepo;
import com.sungwoo.boostcamp.widgetgame.RetrofitRequests.UserInformationRetrofit;

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class JoinActivity extends AppCompatActivity {
    public static final int JOIN_SUCCESS_RESULT_CODE = 100;

    private static final int JOIN_SUCCESS = 100;
    private static final int JOIN_DUPLICATE_EMAIL = 200;
    private static final int JOIN_DUPLICATE_NICKNAME = 300;
    private static final int JOIN_FORMAT_ERROR = 400;
    private static final int JOIN_SERVER_ERROR = 500;


    @BindView(R.id.join_email_et)
    protected EditText mJoinEmailEt;
    @BindView(R.id.join_password_et)
    protected EditText mJoinPasswordEt;
    @BindView(R.id.join_nickname_et)
    protected EditText mJoinNickname_et;

    private static final String TAG = JoinActivity.class.getSimpleName();

    //회원가입 스트링 유효성 파악을 위한 패턴들
    private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_PASSWORD_REGEX = Pattern.compile("^[A-Z0-9!@#$%]{6,20}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_NICKNAME_REGEX = Pattern.compile("^[A-Z0-9가-힣_]{2,16}$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        ButterKnife.bind(this);
    }

    private boolean validateCredentialLocallyDisplayErrorMessageIfNeeded(String email, String password, String nickname) {   //각 값들을 확인해 이상이 있을 시 토스트알림을 띄워준다

        if (!isValidEmail(email)) {
            CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_JOIN_INVALID_EMAIL_CONTENT, R.string.DIALOG_CONFIRM);
            return false;
        } else if (!isValidPassword(password)) {
            CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_JOIN_INVALID_PASSWORD_CONTENT, R.string.DIALOG_CONFIRM);
            return false;
        } else if (!isValidNickname(nickname)) {
            CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_JOIN_INVALID_NICKNAME_CONTENT, R.string.DIALOG_CONFIRM);
            return false;
        }
        return true;
    }

    @OnClick(R.id.join_join_btn)
    public void onClick() {
        String email = mJoinEmailEt.getText().toString();
        String password = mJoinPasswordEt.getText().toString();
        String nickname = mJoinNickname_et.getText().toString();
        if (!validateCredentialLocallyDisplayErrorMessageIfNeeded(email, password, nickname)) {   // 각 값들을 로컬에서 테스트하고 이상이 있을 시 서버로 넘기지 않는다
            return;
        }
        checkJoinServer(email, password, nickname);     // 각 값들을 서버에서 중복확인을 한다
    }

    private boolean isValidEmail(String email) {
        return VALID_EMAIL_ADDRESS_REGEX.matcher(email).find();
    }

    private boolean isValidPassword(String password) {
        return VALID_PASSWORD_REGEX.matcher(password).find();
    }

    private boolean isValidNickname(String nickname) {
        return VALID_NICKNAME_REGEX.matcher(nickname).find();
    }

    private void checkJoinServer(String email, String password, String nickname) {   //서버에 값들을 보내 중복이 없을 시 회원가입까지, 있으면 오류코드를 받아온다.
        if (!CommonUtility.isNetworkAvailableShowErrorMessageIfNeeded(JoinActivity.this)) {
            return;
        }

        final ProgressDialog progressDialog = CommonUtility.showProgressDialogAndReturnInself(this);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.URL_WIDGET_GAME_SERVER))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        UserInformationRetrofit userInformationRetrofit = retrofit.create(UserInformationRetrofit.class);
        Call<CommonRepo.ResultCodeRepo> testJoinServerCall = userInformationRetrofit.testJoinServer(email, password, nickname);
        testJoinServerCall.enqueue(new Callback<CommonRepo.ResultCodeRepo>() {
            @Override
            public void onResponse(Call<CommonRepo.ResultCodeRepo> call, Response<CommonRepo.ResultCodeRepo> response) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                CommonRepo.ResultCodeRepo codeRepo = response.body();
                switch (codeRepo.getCode()) {
                    case JOIN_SUCCESS:
                        setResult(JOIN_SUCCESS_RESULT_CODE);
                        finish();
                        break;
                    case JOIN_DUPLICATE_EMAIL:
                        CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_JOIN_EMAIL_EXISTS_CONTENT, R.string.DIALOG_CONFIRM);
                        break;
                    case JOIN_DUPLICATE_NICKNAME:
                        CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_JOIN_NICKNAME_EXISTS_CONTENT, R.string.DIALOG_CONFIRM);
                        break;
                    case JOIN_FORMAT_ERROR:
                        CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_COMMON_UNKNOWN_ERROR_CONTENT, R.string.DIALOG_CONFIRM);
                        Log.e(TAG, codeRepo.getErrorMessage());
                        break;
                    case JOIN_SERVER_ERROR:
                        CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_COMMON_SERVER_ERROR_CONTENT, R.string.DIALOG_CONFIRM);
                        break;
                    default:

                }
            }

            @Override
            public void onFailure(Call<CommonRepo.ResultCodeRepo> call, Throwable t) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                CommonUtility.showNeutralDialog(JoinActivity.this, R.string.DIALOG_ERR_TITLE, R.string.DIALOG_COMMON_SERVER_ERROR_CONTENT, R.string.DIALOG_CONFIRM);
                try {
                    throw t;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
    }
}