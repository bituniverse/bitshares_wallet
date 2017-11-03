package com.bitshares.bitshareswallet;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitshares.bitshareswallet.room.BitsharesAssetObject;
import com.bitshares.bitshareswallet.room.BitsharesOperationHistory;
import com.bitshares.bitshareswallet.viewmodel.TransactionViewModel;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.graphene.chain.config;
import com.bitshares.bitshareswallet.wallet.graphene.chain.memo_data;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;

import java.util.Locale;

/**
 * Created by lorne on 01/11/2017.
 */
class TranactionsAdapter extends RecyclerView.Adapter<TranactionsAdapter.TransactionsItemViewHolder> {
    class TransactionsItemViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public TextView viewActionDetail;
        public TextView viewAction;
        public ImageView imageView;
        public TransactionsItemViewHolder(View itemView) {
            super(itemView);
            view = itemView;

            viewActionDetail = (TextView) view.findViewById(R.id.textViewOperationDetail);
            viewAction = (TextView) view.findViewById(R.id.textViewOperation);
            imageView = (ImageView) view.findViewById(R.id.imageView);
        }
    }
    private TransactionsFragment transactionsFragment;
    private TransactionViewModel.OperationHistoryWrapper operationHistoryWrapper;
    private final LayoutInflater mLayoutInflater;

    public TranactionsAdapter(TransactionsFragment transactionsFragment, Context context) {
        this.transactionsFragment = transactionsFragment;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public TransactionsItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.recyclerview_item_transactions, parent, false);
        return new TransactionsItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TransactionsItemViewHolder holder, int position) {
        BitsharesOperationHistory object = operationHistoryWrapper.bitsharesOperationHistoryList.get(position);
        TextView viewActionDetail = (TextView) holder.view.findViewById(R.id.textViewOperationDetail);
        TextView viewAction = (TextView) holder.view.findViewById(R.id.textViewOperation);

        int op = object.operationHistoryObject.op.nOperationType;

        String strResult = "";
        switch (op) {
            case operations.ID_TRANSER_OPERATION:
                strResult = process_transfer_operation(holder, object.operationHistoryObject.op);
                break;
            case operations.ID_CREATE_LIMIT_ORDER_OPERATION:
                strResult = process_limit_order_create_operation(holder, object.operationHistoryObject.op);
                break;
            case operations.ID_CANCEL_LMMIT_ORDER_OPERATION:
                strResult = process_limit_order_cancel_operation(holder, object.operationHistoryObject.op);
                break;
            case operations.ID_UPDATE_LMMIT_ORDER_OPERATION:
                strResult = process_call_order_update_operation(holder, object.operationHistoryObject.op);
                break;
            case operations.ID_FILL_LMMIT_ORDER_OPERATION:
                strResult = process_fill_order_operation(holder, object.operationHistoryObject.op);
                break;
            case operations.ID_CREATE_ACCOUNT_OPERATION:
                strResult = process_account_create_operation(holder, object.operationHistoryObject.op);
                break;
        }
        if (op != operations.ID_TRANSER_OPERATION) {
            processMemoMessage(holder, null);
        }

        viewActionDetail.setText(Html.fromHtml(strResult));

        //Date operationTime = new Date(operationHistoryWrapper.bitsharesOperationHistoryList.get(position).timestamp);
        long operationTime = operationHistoryWrapper.bitsharesOperationHistoryList.get(position).timestamp;
        long lDifferenceTime = (System.currentTimeMillis() - operationTime) / 1000;

        TextView textViewTime = (TextView) holder.view.findViewById(R.id.textViewTime);
        String strTime = "";
        if (lDifferenceTime >= 365 * 24 * 3600) {
            long lYear = lDifferenceTime / (365 * 24 * 3600);
            strTime = formatTimeString(lYear, R.string.operation_history_years, R.string.operation_history_year);
        } else if (lDifferenceTime >= 30 * 24 * 3600) {
            long lMonth = lDifferenceTime / (30 * 24 * 3600);
            strTime = formatTimeString(lMonth, R.string.operation_history_months, R.string.operation_history_month);
        } else if (lDifferenceTime >= 7 * 24 * 3600) {
            long lWeek = lDifferenceTime / (7 * 24 * 3600);
            strTime = formatTimeString(lWeek, R.string.operation_history_weeks, R.string.operation_history_week);
        } else if (lDifferenceTime >= 24 * 3600) {
            long lDay = lDifferenceTime / (24 * 3600);
            strTime = formatTimeString(lDay, R.string.operation_history_days, R.string.operation_history_day);
        } else if (lDifferenceTime >= 3600) {
            long lHour = lDifferenceTime / 3600;
            strTime = formatTimeString(lHour, R.string.operation_history_hours, R.string.operation_history_hour);
        } else if (lDifferenceTime >= 60) {
            long lMinite = lDifferenceTime / 60;
            strTime = formatTimeString(lMinite, R.string.operation_history_mintes, R.string.operation_history_minte);
        } else {
            strTime = formatTimeString(lDifferenceTime, R.string.operation_history_seconds, R.string.operation_history_second);
        }
        textViewTime.setText(strTime);
    }

    private String formatTimeString(long lCount, int nResIds, int nResId) {
        String strResult = "";
        if (lCount > 1) {
            strResult = transactionsFragment.getString(nResIds, lCount);
        } else {
            strResult = transactionsFragment.getString(nResId, lCount);
        }
        return strResult;
    }

    @Override
    public int getItemCount() {
        if (operationHistoryWrapper == null) {
            return 0;
        } else {
            return operationHistoryWrapper.bitsharesOperationHistoryList.size();
        }
    }

    public void notifyTransactionDataChanged(TransactionViewModel.OperationHistoryWrapper operationHistoryWrapper) {
        this.operationHistoryWrapper = operationHistoryWrapper;
        notifyDataSetChanged();
    }

    private String process_account_create_operation(TransactionsItemViewHolder holder,
                                                    operations.operation_type operationType) {
        holder.viewAction.setText("Create");
        operations.account_create_operation operation = (operations.account_create_operation) operationType.operationContent;
        String strRegistarName = operationHistoryWrapper.mapId2AccountObject.get(operation.registrar).name;

        String strTransferFormat = "<font color=\'#2d95ff\'>%s</font> registered the account <font color=\'#2d95ff\'>%s</font>";
        String strResult = String.format(
                Locale.ENGLISH,
                strTransferFormat,
                strRegistarName,
                operation.name
        );

        holder.imageView.setImageResource(R.mipmap.create_account);

        return strResult;
    }

    private String process_transfer_operation(TransactionsItemViewHolder holder,
                                              operations.operation_type operationType) {
        holder.viewAction.setText("Transfer");
        operations.transfer_operation operation = (operations.transfer_operation) operationType.operationContent;

        String strFrom = operationHistoryWrapper.mapId2AccountObject.get(operation.from).name;
        String strTo = operationHistoryWrapper.mapId2AccountObject.get(operation.to).name;
        BitsharesAssetObject bitsharesAssetObject = operationHistoryWrapper.mapId2AssetObject.get(operation.amount.asset_id);

        String strTransferFormat = "<font color=\'#2d95ff\'>%s</font> sent <font color=\'#000000\'>%s %s</font> to <font color=\'#2d95ff\'>%s</font>";

        String strResult = String.format(
                Locale.ENGLISH, strTransferFormat,
                strFrom,
                (float) operation.amount.amount / bitsharesAssetObject.precision,
                bitsharesAssetObject.symbol,
                strTo
        );

        holder.imageView.setImageResource(R.mipmap.transfer);

        processMemoMessage(holder, operation.memo);
        return strResult;
    }

    private void processMemoUnlockClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(transactionsFragment.getActivity());
        LayoutInflater layoutInflater = transactionsFragment.getActivity().getLayoutInflater();
        final View viewGroup = layoutInflater.inflate(R.layout.dialog_password_unlock, null);
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
                int nRet = BitsharesWalletWraper.getInstance().unlock(strPassword);
                if (nRet == 0) {
                    // 解开所有的memo数据
                    dialog.dismiss();
                    notifyDataSetChanged();
                } else {
                    viewGroup.findViewById(R.id.textViewPasswordInvalid).setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void processMemoMessage(TransactionsItemViewHolder holder,
                                    memo_data memoData) {
        if (memoData != null) {
            View layoutMemo = holder.view.findViewById(R.id.layoutMemo);
            layoutMemo.setVisibility(View.VISIBLE);
            View layoutTransaction = holder.view.findViewById(R.id.layoutTransactionDetail);

            if (BitsharesWalletWraper.getInstance().is_locked()) {
                layoutTransaction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        processMemoUnlockClick();
                    }
                });
            } else {
                TextView textViewMemo = (TextView) holder.view.findViewById(R.id.textViewMemo);
                holder.view.findViewById(R.id.imageViewMemoLock).setVisibility(View.GONE);
                final String strMemo = "Memo: " + BitsharesWalletWraper.getInstance().get_plain_text_message(memoData);
                textViewMemo.setText(strMemo);

                layoutTransaction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(transactionsFragment.getActivity());
                        builder.setMessage(strMemo);
                        builder.show();
                    }
                });
            }
        } else {
            View layoutMemo = holder.view.findViewById(R.id.layoutMemo);
            layoutMemo.setVisibility(View.GONE);

            View layoutTransaction = holder.view.findViewById(R.id.layoutTransactionDetail);
            layoutTransaction.setOnClickListener(null);
        }
    }

    private String process_limit_order_create_operation(TransactionsItemViewHolder holder,
                                                        operations.operation_type operationType) {
        holder.viewAction.setText("Create Order");
        operations.limit_order_create_operation operation = (operations.limit_order_create_operation) operationType.operationContent;

        String strTransferFormat = "<font color=\'#000000\'>To buy %.5f %s</font> at %.5f %s/%s";
        String strResult = processOrderOperation(operation.amount_to_sell, operation.min_to_receive, strTransferFormat);


        holder.imageView.setImageResource(R.mipmap.order);

        return strResult;
    }

    private String process_limit_order_cancel_operation(TransactionsItemViewHolder holder,
                                                        operations.operation_type operationType) {
        holder.viewAction.setText("Cancel Order");
        operations.limit_order_cancel_operation operation = (operations.limit_order_cancel_operation) operationType.operationContent;

        String strTransferFormat = "<font color=\'#000000\'>Canceled order #%d</font>";
        String strResult = String.format(
                Locale.ENGLISH,
                strTransferFormat,
                operation.order.get_instance()
        );

        holder.imageView.setImageResource(R.mipmap.cancel_order);

        return strResult;
    }

    private String process_call_order_update_operation(TransactionsItemViewHolder holder,
                                                       operations.operation_type operationType) {
        holder.viewAction.setText("Update Order");
        operations.call_order_update_operation operation = (operations.call_order_update_operation) operationType.operationContent;

        String strTransferFormat = "<font color=\'#000000\'>%s</font>";

        String strResult = String.format(
                Locale.ENGLISH,
                strTransferFormat,
                "Updated Order"
        );

        holder.imageView.setImageResource(R.mipmap.transaction_send);

        return strResult;
    }

    private String process_fill_order_operation(TransactionsItemViewHolder holder,
                                                operations.operation_type operationType) {
        holder.viewAction.setText("Fill Order");
        operations.fill_order_operation operation = (operations.fill_order_operation) operationType.operationContent;

        String strTransferFormat = "<font color=\'#000000\'>Bought %.5f %s</font> at %.5f %s/%s";
        String strResult = processOrderOperation(operation.pays, operation.receives, strTransferFormat);

        if (operation.receives.asset_id.equals(config.asset_object_base)) {
            holder.imageView.setImageResource(R.mipmap.transaction_recv);
        } else {
            holder.imageView.setImageResource(R.mipmap.transaction_send);
        }

        return strResult;
    }

    private String processOrderOperation(asset assetSell, asset assetReceive, String strFormat) {
        BitsharesAssetObject sellObject = operationHistoryWrapper.mapId2AssetObject.get(assetSell.asset_id);
        BitsharesAssetObject receiveObject = operationHistoryWrapper.mapId2AssetObject.get(assetReceive.asset_id);

        double fReceiverResult = (double) assetReceive.amount / receiveObject.precision;
        double fPayResult = (double) assetSell.amount / sellObject.precision;

        double fExchangeRate = fPayResult / fReceiverResult;

        String strResult = String.format(
                Locale.ENGLISH,
                strFormat,
                fReceiverResult,
                receiveObject.symbol,
                fExchangeRate,
                sellObject.symbol,
                receiveObject.symbol
        );
        return strResult;
    }
}
