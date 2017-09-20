package com.bitshares.bitshareswallet;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TransactionSellBuyPasswordDialog {
    private Activity mActivity;
    private AlertDialog.Builder mDialogBuilder;
    private AlertDialog mDialog;
    private OnDialogInterationListener mListener;
    private EditText editTextPassword;

    public TransactionSellBuyPasswordDialog(Activity mActivity) {
        this.mActivity = mActivity;

        mDialogBuilder = new AlertDialog.Builder(mActivity);

        View view = mActivity.getLayoutInflater().inflate(R.layout.dialog_transaction_password_order, null);
        editTextPassword = (EditText)view.findViewById(R.id.editTextPassword);

        TextView txtConfirm = (TextView)view.findViewById(R.id.dco_txt_confirm);
        txtConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onConfirm(mDialog, editTextPassword.getText().toString());
                }
            }
        });

        TextView txtNo = (TextView)view.findViewById(R.id.dco_txt_no);
        txtNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onReject(mDialog);
                }
            }
        });
        mDialogBuilder.setView(view);
    }

    public void show(){
        mDialog = mDialogBuilder.show();
    }

    public void setListener(OnDialogInterationListener listener){
        mListener = listener;
    }

    public interface OnDialogInterationListener {
        void onConfirm(AlertDialog dialog, String passwordString);
        void onReject(AlertDialog dialog);
    }
}
