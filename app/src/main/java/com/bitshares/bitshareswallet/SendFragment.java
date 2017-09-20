package com.bitshares.bitshareswallet;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bitshares.bitshareswallet.wallet.BitshareData;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.exception.NetworkStatusException;
import com.bitshares.bitshareswallet.wallet.fc.crypto.sha256_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;
import com.kaopiz.kprogresshud.KProgressHUD;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SendFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private KProgressHUD mProcessHud;
    private Spinner mSpinner;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private EditText mEditTextTo;
    private TextView mTextViewId;

    private EditText mEditTextQuantitiy;

    private View mView;
    private Handler mHandler = new Handler();

    public SendFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SendFragment newInstance(String param1, String param2) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_send, container, false);
        EditText editTextFrom = (EditText)mView.findViewById(R.id.editTextFrom);

        String strName = BitsharesWalletWraper.getInstance().get_account().name;
        editTextFrom.setText(strName);

        sha256_object.encoder encoder = new sha256_object.encoder();
        encoder.write(strName.getBytes());

        WebView webViewFrom = (WebView)mView.findViewById(R.id.webViewAvatarFrom);
        loadWebView(webViewFrom, 40, encoder.result().toString());

        TextView textView = (TextView)mView.findViewById(R.id.textViewFromId);
        String strId = String.format(
                Locale.ENGLISH, "#%d",
                BitsharesWalletWraper.getInstance().get_account().id.get_instance()
        );
        textView.setText(strId);

        mProcessHud = KProgressHUD.create(getActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please Wait")
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        mView.findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processSendClick(mView);
            }
        });

        mEditTextTo = (EditText)mView.findViewById(R.id.editTextTo);
        mTextViewId = (TextView)mView.findViewById(R.id.textViewToId);
        mEditTextTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                final String strText = mEditTextTo.getText().toString();
                if (hasFocus == false) {
                    processGetTransferToId(strText, mTextViewId);
                }
            }
        });

        final WebView webViewTo = (WebView)mView.findViewById(R.id.webViewAvatarTo);
        mEditTextTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                sha256_object.encoder encoder = new sha256_object.encoder();
                encoder.write(s.toString().getBytes());
                loadWebView(webViewTo, 40, encoder.result().toString());
            }
        });


        mEditTextQuantitiy = (EditText)mView.findViewById(R.id.editTextQuantity) ;
        mEditTextQuantitiy.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false) {
                    processCalculateFee();
                }
            }
        });

        mSpinner = (Spinner) mView.findViewById(R.id.spinner_unit);

        return mView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            /*throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");*/
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onShow() {
        super.onShow();
        if (mEditTextTo.getText().length() > 0) {
            processGetTransferToId(mEditTextTo.getText().toString(), mTextViewId);
        }
        notifyUpdate();
    }

    @Override
    public void onHide() {
        super.onHide();
        hideSoftKeyboard(mEditTextTo, getActivity());
    }

    private void processTransfer(final String strFrom,
                                 final String strTo,
                                 final String strQuantity,
                                 final String strSymbol,
                                 final String strMemo) {
        mProcessHud.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                signed_transaction signedTransaction = null;
                try {
                    signedTransaction = BitsharesWalletWraper.getInstance().transfer(
                            strFrom,
                            strTo,
                            strQuantity,
                            strSymbol,
                            strMemo
                    );
                } catch (NetworkStatusException e) {
                    e.printStackTrace();
                }

                if (signedTransaction != null) {
                    mListener.notifyTransferComplete(signedTransaction);
                }

                final signed_transaction finalSignedTransaction = signedTransaction;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null && getActivity().isFinishing() == false) {
                            mProcessHud.dismiss();
                            if (finalSignedTransaction != null) {
                                Toast.makeText(getActivity(), "Sent Successfully", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), "Fail to send", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void processSendClick(final View view) {
        if (BitsharesWalletWraper.getInstance().is_locked()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            final View viewGroup = layoutInflater.inflate(R.layout.dialog_password_confirm, null);
            builder.setPositiveButton(
                    R.string.password_confirm_button_confirm,
                    null);

            builder.setNegativeButton(
                    R.string.password_confirm_button_cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }
            );
            builder.setView(viewGroup);
            final AlertDialog dialog = builder.create();
            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText = (EditText) viewGroup.findViewById(R.id.editTextPassword);
                    String strPassword = editText.getText().toString();
                    //strPassword = "yIe25_WQ1_qw3";
                    int nRet = BitsharesWalletWraper.getInstance().unlock(strPassword);
                    if (nRet == 0) {
                        dialog.dismiss();
                        String strFrom = ((EditText) view.findViewById(R.id.editTextFrom)).getText().toString();
                        String strTo = ((EditText) view.findViewById(R.id.editTextTo)).getText().toString();
                        String strQuantity = ((EditText) view.findViewById(R.id.editTextQuantity)).getText().toString();
                        String strSymbol = (String)mSpinner.getSelectedItem();
                        String strMemo = ((EditText)view.findViewById(R.id.editTextMemo)).getText().toString();
                        processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo);
                    } else {
                        viewGroup.findViewById(R.id.textViewPasswordInvalid).setVisibility(View.VISIBLE);
                    }
                }
            });

        } else {
            String strFrom = ((EditText) view.findViewById(R.id.editTextFrom)).getText().toString();
            String strTo = ((EditText) view.findViewById(R.id.editTextTo)).getText().toString();
            String strQuantity = ((EditText) view.findViewById(R.id.editTextQuantity)).getText().toString();
            String strSymbol = (String)mSpinner.getSelectedItem();
            String strMemo = ((EditText)view.findViewById(R.id.editTextMemo)).getText().toString();

            processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo);
        }
    }

    private void processGetTransferToId(final String strAccount, final TextView textViewTo) {
        // 失去焦点，检测该号的id
        new Thread(new Runnable() {
            @Override
            public void run() {
                account_object accountObject = null;
                try {
                    accountObject = BitsharesWalletWraper.getInstance().get_account_object(strAccount);
                    if (accountObject != null) {
                        final account_object finalAccountObject = accountObject;

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() != null && getActivity().isFinishing() == false) {
                                    textViewTo.setText("#" + finalAccountObject.id.get_instance());
                                }
                            }
                        });

                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (getActivity() != null && getActivity().isFinishing() == false) {
                                    textViewTo.setText("#none");
                                }
                            }
                        });
                    }
                } catch (NetworkStatusException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }



    public static void hideSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public static void showSoftKeyboard(View view, Context context) {
        if (view != null && context != null) {
            InputMethodManager imm = (InputMethodManager) context
                    .getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, 0);
        }
    }

    @Override
    public void notifyUpdate() {
        BitshareData bitshareData = BitsharesWalletWraper.getInstance().getBitshareData();
        if (bitshareData == null || getActivity() == null || mSpinner == null) {
            return;
        }

        List<String> listSymbols = new ArrayList<>();
        if (bitshareData != null) {
            for (asset i : bitshareData.listBalances) {
                String strSymbol = bitshareData.mapId2AssetObject.get(i.asset_id).symbol;
                listSymbols.add(strSymbol);
            }
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                listSymbols
        );

        if (mSpinner != null) {
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(arrayAdapter);
        }

        if (mEditTextQuantitiy != null) {
            processCalculateFee();
        }
    }

    private void processCalculateFee() {
        final String strQuantity = ((EditText) mView.findViewById(R.id.editTextQuantity)).getText().toString();
        final String strSymbol = (String) mSpinner.getSelectedItem();
        final String strMemo = ((EditText) mView.findViewById(R.id.editTextMemo)).getText().toString();

        // 用户没有任何货币，这个symbol会为空，则会出现崩溃，进行该处理进行规避
        if (TextUtils.isEmpty(strSymbol)) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    asset fee = BitsharesWalletWraper.getInstance().transfer_calculate_fee(
                            strQuantity,
                            strSymbol,
                            strMemo
                    );

                    asset_object assetObject = BitsharesWalletWraper.getInstance().getBitshareData().mapId2AssetObject.get(fee.asset_id);
                    final asset_object.asset_object_legible legibleObject = assetObject.get_legible_asset_object(fee.amount);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && getActivity().isFinishing() == false) {
                                processDisplayFee(legibleObject);
                            }
                        }
                    });
                } catch (NetworkStatusException e) {
                    e.printStackTrace();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() != null && getActivity().isFinishing() == false) {
                                EditText editTextFee = (EditText) mView.findViewById(R.id.editTextFee);
                                editTextFee.setText("N/A");
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void processDisplayFee(asset_object.asset_object_legible legibleObject) {
        float fResult = legibleObject.lCount + (float) legibleObject.lDecimal / legibleObject.scaled_precision;
        EditText editTextFee = (EditText) mView.findViewById(R.id.editTextFee);
        String strResult = String.format(
                Locale.ENGLISH,
                "%f (%s)",
                fResult,
                "Cannot be modified"
        );
        editTextFee.setText(strResult);

        Spinner spinner = (Spinner) mView.findViewById(R.id.spinner_fee_unit);

        List<String> listSymbols = new ArrayList<>();
        listSymbols.add(legibleObject.symbol);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                listSymbols
        );

        if (mSpinner != null) {
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(arrayAdapter);
        }
    }

    private void loadWebView(WebView webView, int size, String encryptText) {
        String htmlShareAccountName = "<html><head><style>body,html {margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=" + size + ",user-scalable=no/></head><body><canvas width=" + size + " height=" + size + " data-jdenticon-hash=" + encryptText + "></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>";
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadData(htmlShareAccountName, "text/html", "UTF-8");
    }

    public void processDonate(String strName, int nAmount, String strUnit) {
        if (isAdded()) {
            mEditTextTo.setText(strName);
            mEditTextQuantitiy.setText(Integer.toString(nAmount));
            mSpinner.setSelection(0);
        }
    }
}
