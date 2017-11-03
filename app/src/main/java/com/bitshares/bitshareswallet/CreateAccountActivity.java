package com.bitshares.bitshareswallet;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.common.ErrorCode;
import com.bitshares.bitshareswallet.wallet.exception.ErrorCodeException;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.faucet.CreateAccountException;
import com.kaopiz.kprogresshud.KProgressHUD;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.bitshares.bitshareswallet.wallet.common.ErrorCode.*;

public class CreateAccountActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private KProgressHUD mProcessHud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mProcessHud = KProgressHUD.create(CreateAccountActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        findViewById(R.id.buttonCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProcessHud.show();

                String strAccount = ((EditText)findViewById(R.id.editTextAccountName)).getText().toString();
                String strPassword = ((EditText)findViewById(R.id.editTextPassword)).getText().toString();
                String strPasswordConfirm = ((EditText)findViewById(R.id.editTextPasswordConfirm)).getText().toString();
                CheckBox checkBox = (CheckBox)findViewById(R.id.checkBoxConfirm);


                TextView textViewAccountError = (TextView)findViewById(R.id.textViewErrorAccount);
                TextView textViewPasswordError = (TextView)findViewById(R.id.textViewErrorPasswrod);
                TextView textViewPasswordConfirmError = (TextView)findViewById(R.id.textViewErrorInfo);

                boolean bError = false;
                if (strAccount.isEmpty()) {
                    textViewAccountError.setText(R.string.create_account_account_name_empty);
                    bError = true;
                }

                if (strPassword.isEmpty()) {
                    textViewPasswordError.setText(R.string.create_account_password_empty);
                    bError = true;
                }

                if (strPasswordConfirm.isEmpty()) {
                    textViewPasswordConfirmError.setText(R.string.create_account_password_confirm_empty);
                    bError = true;
                }

                if (bError == false && textViewAccountError.getText().length() == 0 && textViewPasswordError.getText().length() == 0) {
                    if (checkBox.isChecked() == true) {
                        processCreateAccount(strAccount, strPassword, strPasswordConfirm);
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
                        builder.setMessage(R.string.create_account_check_box_confirm);
                        builder.show();
                        mProcessHud.dismiss();
                    }
                } else {
                    mProcessHud.dismiss();
                }
            }
        });

        EditText editTextAccountName = (EditText)findViewById(R.id.editTextAccountName);
        InputFilter lowercaseFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                return source.toString().toLowerCase();
            }
        };
        editTextAccountName.setFilters(new InputFilter[] {lowercaseFilter, new InputFilter.LengthFilter(63)});

        editTextAccountName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strAccountName = s.toString();
                if (strAccountName.isEmpty() == true) {
                    return;
                }

                TextView textViewError = (TextView)findViewById(R.id.textViewErrorAccount);
                if (Character.isLetter(strAccountName.charAt(0)) == false) {
                    textViewError.setText(R.string.create_account_account_name_error_start_letter);
                    findViewById(R.id.imageViewAccountCheck).setVisibility(View.INVISIBLE);
                } else if (strAccountName.length() <= 4) {  // 用户名太短
                    textViewError.setText(R.string.create_account_account_name_too_short);
                    findViewById(R.id.imageViewAccountCheck).setVisibility(View.INVISIBLE);
                } else if (strAccountName.endsWith("-")) {
                    textViewError.setText(R.string.create_account_account_name_error_dash_end);
                    findViewById(R.id.imageViewAccountCheck).setVisibility(View.INVISIBLE);
                } else {
                    boolean bCombineAccount = false;
                    for (char c : strAccountName.toCharArray()) {
                        if (Character.isLetter(c) == false) {
                            bCombineAccount = true;
                        }
                    }

                    if (bCombineAccount == false) {
                        textViewError.setText(R.string.create_account_account_name_error_full_letter);
                        findViewById(R.id.imageViewAccountCheck).setVisibility(View.INVISIBLE);
                    } else {
                        textViewError.setText("");
                        //findViewById(R.id.imageViewAccountCheck).setVisibility(View.VISIBLE);
                        processCheckAccount(strAccountName);
                    }
                }
            }
        });

        final EditText editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strPassword = s.toString();

                TextView textViewError = (TextView)findViewById(R.id.textViewErrorPasswrod);
                if (strPassword.length() < 12) {
                    textViewError.setText(R.string.create_account_password_requirement);
                } else {
                    boolean bDigit = strPassword.matches(".*\\d+.*");
                    boolean bUpperCase = strPassword.matches(".*[A-Z]+.*");
                    boolean bLowerCase = strPassword.matches(".*[a-z]+.*");
                    if ((bDigit && bUpperCase && bLowerCase) == false) {
                        textViewError.setText(R.string.create_account_password_requirement);
                    } else {
                        textViewError.setText("");
                    }
                }
            }
        });

        EditText editTextPasswordConfirm = (EditText)findViewById(R.id.editTextPasswordConfirm);
        editTextPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strPassword = editTextPassword.getText().toString();
                String strPasswordConfirm = s.toString();

                TextView textViewErrorInfo = (TextView)findViewById(R.id.textViewErrorInfo);
                if (strPassword.compareTo(strPasswordConfirm) == 0) {
                    findViewById(R.id.imageViewPasswordConfirmCheck).setVisibility(View.VISIBLE);
                    textViewErrorInfo.setText("");
                } else {
                    findViewById(R.id.imageViewPasswordConfirmCheck).setVisibility(View.INVISIBLE);
                    textViewErrorInfo.setText(R.string.create_account_password_confirm_error);
                }
            }
        });
    }

    private void processCreateAccount(final String strAccount, final String strPassword, String strPasswordConfirm) {
        if (strPassword.compareTo(strPasswordConfirm) != 0) {
            processErrorCode(ERROR_PASSWORD_CONFIRM_FAIL);
            return;
        }

        Flowable.just(0)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    int nRet = BitsharesWalletWraper.getInstance().build_connect();
                    if (nRet != 0) {
                        throw new NetworkStatusException("it failed to connect to server");
                    }

                    nRet = BitsharesWalletWraper.getInstance().create_account_with_password(
                            strAccount,
                            strPassword
                    );
                    if (nRet != 0) {
                        Observable.error(new ErrorCodeException(nRet, "it failed to create account"));
                    }
                    return nRet;
                }).map(result -> {
                    int nCount = 0;
                    int nRet;
                    do {
                        // 进入导入帐号流程
                        nRet = BitsharesWalletWraper.getInstance().import_account_password(
                            strAccount,
                            strPassword
                        );
                        nCount++;
                        if (nRet == ERROR_NO_ACCOUNT_OBJECT) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } while (nRet == ERROR_NO_ACCOUNT_OBJECT && nCount < 10);
                    if (nRet != 0) { // 一切就绪，进入首页
                        Observable.error(new ErrorCodeException(nRet, "it failed to import account"));
                    }
                    return nRet;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    mProcessHud.dismiss();
                    Intent intent = new Intent(CreateAccountActivity.this, MainActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }, throwable -> {
                    mProcessHud.dismiss();
                    if (throwable instanceof NetworkStatusException) {
                        processErrorCode(ErrorCode.ERROR_NETWORK_FAIL);
                    } else if (throwable instanceof CreateAccountException) {
                        processExceptionMessage(throwable.getMessage());
                    } else if (throwable instanceof ErrorCodeException) {
                        ErrorCodeException errorCodeException = (ErrorCodeException) throwable;
                        processErrorCode(errorCodeException.getErrorCode());
                    }
                });

    }

    private void processErrorCode(final int nErrorCode) {
        final TextView textView = (TextView) findViewById(R.id.textViewErrorInfo);
        switch (nErrorCode) {
            case ERROR_NETWORK_FAIL:
                textView.setText(R.string.create_account_activity_network_fail);
                break;
            case ERROR_ACCOUNT_OBJECT_EXIST:
                textView.setText(R.string.create_account_activity_account_object_exist);
                break;
            case ERROR_SERVER_RESPONSE_FAIL:
                textView.setText(R.string.create_account_activity_response_fail);
                break;
            case ERROR_SERVER_CREATE_ACCOUNT_FAIL:
                textView.setText(R.string.create_account_activity_create_fail);
                break;
            case ERROR_FILE_NOT_FOUND:
                textView.setText(R.string.import_activity_file_failed);
                break;
            case ERROR_FILE_READ_FAIL:
                textView.setText(R.string.import_activity_file_failed);
                break;
            case ERROR_NO_ACCOUNT_OBJECT:
                textView.setText(R.string.import_activity_account_name_invalid);
                break;
            case ERROR_IMPORT_NOT_MATCH_PRIVATE_KEY:
                textView.setText(R.string.import_activity_private_key_invalid);
                break;
            case ERROR_PASSWORD_INVALID:
                textView.setText(R.string.import_activity_password_invalid);
                break;
            case ERROR_UNKNOWN:
                textView.setText(R.string.import_activity_unknown_error);
                break;
            default:
                textView.setText(R.string.import_activity_unknown_error);
                break;
        }
    }

    private void processExceptionMessage(final String strMessage) {
        final TextView textView = (TextView) findViewById(R.id.textViewErrorInfo);
        textView.setText(strMessage);
    }

    private void processCheckAccount(final String strAccount) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int nRet = BitsharesWalletWraper.getInstance().build_connect();
                if (nRet == 0) {
                    try {
                        final account_object accountObect = BitsharesWalletWraper.getInstance().get_account_object(strAccount);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (accountObect == null) {
                                    findViewById(R.id.imageViewAccountCheck).setVisibility(View.VISIBLE);
                                } else {
                                    EditText editTextAccount = (EditText)findViewById(R.id.editTextAccountName);
                                    String strAccountName = editTextAccount.getText().toString();
                                    if (strAccountName.compareTo(accountObect.name) == 0) {
                                        TextView textViewError = (TextView) findViewById(R.id.textViewErrorAccount);
                                        textViewError.setText(R.string.create_account_activity_account_object_exist);
                                        findViewById(R.id.imageViewAccountCheck).setVisibility(View.INVISIBLE);
                                    }
                                }
                            }
                        });

                    } catch (NetworkStatusException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
