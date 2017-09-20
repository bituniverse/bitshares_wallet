package com.bitshares.bitshareswallet;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bitshares.bitshareswallet.wallet.BitshareData;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.account_object;
import com.bitshares.bitshareswallet.wallet.asset;
import com.bitshares.bitshareswallet.wallet.graphene.chain.asset_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.config;
import com.bitshares.bitshareswallet.wallet.graphene.chain.memo_data;
import com.bitshares.bitshareswallet.wallet.graphene.chain.object_id;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operation_history_object;
import com.bitshares.bitshareswallet.wallet.graphene.chain.operations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TransactionsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TransactionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TransactionsFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TranactionsAdapter mTranactionsAdapter;

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

    class TranactionsAdapter extends RecyclerView.Adapter<TransactionsItemViewHolder> {
        private List<Pair<operation_history_object, Date>> mListHistoryObjectTime = new ArrayList<>();
        private Map<object_id<account_object>, account_object> mMapId2AccountObject;
        private Map<object_id<asset_object>, asset_object> mMapId2AssetObject;
        private BitshareData mBitsharesData;
        private final LayoutInflater mLayoutInflater;

        public TranactionsAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public TransactionsItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.recyclerview_item_transactions, parent, false);
            return new TransactionsItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TransactionsItemViewHolder holder, int position) {
            operation_history_object object = mListHistoryObjectTime.get(position).first;
            TextView viewActionDetail = (TextView) holder.view.findViewById(R.id.textViewOperationDetail);
            TextView viewAction = (TextView) holder.view.findViewById(R.id.textViewOperation);

            int op = object.op.nOperationType;

            String strResult = "";
            switch (op) {
                case operations.ID_TRANSER_OPERATION:
                    strResult = process_transfer_operation(holder, object.op);
                    break;
                case operations.ID_CREATE_LIMIT_ORDER_OPERATION:
                    strResult = process_limit_order_create_operation(holder, object.op);
                    break;
                case operations.ID_CANCEL_LMMIT_ORDER_OPERATION:
                    strResult = process_limit_order_cancel_operation(holder, object.op);
                    break;
                case operations.ID_UPDATE_LMMIT_ORDER_OPERATION:
                    strResult = process_call_order_update_operation(holder, object.op);
                    break;
                case operations.ID_FILL_LMMIT_ORDER_OPERATION:
                    strResult = process_fill_order_operation(holder, object.op);
                    break;
                case operations.ID_CREATE_ACCOUNT_OPERATION:
                    strResult = process_account_create_operation(holder, object.op);
                    break;
            }
            if (op != operations.ID_TRANSER_OPERATION) {
                processMemoMessage(holder, null);
            }

            viewActionDetail.setText(Html.fromHtml(strResult));

            Date operationTime = mListHistoryObjectTime.get(position).second;
            long lDifferenceTime = (System.currentTimeMillis() - operationTime.getTime()) / 1000;

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
                strResult = getString(nResIds, lCount);
            } else {
                strResult = getString(nResId, lCount);
            }
            return strResult;
        }

        @Override
        public int getItemCount() {
            if (mBitsharesData == null) {
                return 0;
            } else {
                return mBitsharesData.listHistoryObject.size();
            }
        }

        public void notifyTransactionDataChanged(BitshareData bitshareData) {
            mBitsharesData = bitshareData;
            mListHistoryObjectTime = bitshareData.listHistoryObject;
            mMapId2AccountObject = bitshareData.mapId2AccountObject;
            mMapId2AssetObject = bitshareData.mapId2AssetObject;
            notifyDataSetChanged();
        }

        private String process_account_create_operation(TransactionsItemViewHolder holder,
                                                        operations.operation_type operationType) {
            holder.viewAction.setText("Create");
            operations.account_create_operation operation = (operations.account_create_operation) operationType.operationContent;
            String strRegistarName = mMapId2AccountObject.get(operation.registrar).name;

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

            String strFrom = mMapId2AccountObject.get(operation.from).name;
            String strTo = mMapId2AccountObject.get(operation.to).name;
            asset_object assetObject = mMapId2AssetObject.get(operation.amount.asset_id);
            asset_object.asset_object_legible assetObjectLigible = assetObject.get_legible_asset_object(operation.amount.amount);

            String strTransferFormat = "<font color=\'#2d95ff\'>%s</font> sent <font color=\'#000000\'>%s %s</font> to <font color=\'#2d95ff\'>%s</font>";

            String strResult = String.format(
                    Locale.ENGLISH, strTransferFormat,
                    strFrom,
                    assetObjectLigible.count,
                    assetObjectLigible.symbol,
                    strTo
            );

            holder.imageView.setImageResource(R.mipmap.transfer);

            processMemoMessage(holder, operation.memo);

            /*if (operation.memo != null) {
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
                    TextView textViewMemo = (TextView)holder.view.findViewById(R.id.textViewMemo);
                    holder.view.findViewById(R.id.imageViewMemoLock).setVisibility(View.GONE);
                    final String strMemo = "Memo: " + BitsharesWalletWraper.getInstance().get_plain_text_message(operation.memo);
                    textViewMemo.setText(strMemo);

                    layoutTransaction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            }*/

            return strResult;
        }

        private void processMemoUnlockClick() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
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
                    TextView textViewMemo = (TextView)holder.view.findViewById(R.id.textViewMemo);
                    holder.view.findViewById(R.id.imageViewMemoLock).setVisibility(View.GONE);
                    final String strMemo = "Memo: " + BitsharesWalletWraper.getInstance().get_plain_text_message(memoData);
                    textViewMemo.setText(strMemo);

                    layoutTransaction.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            operations.limit_order_cancel_operation operation = (operations.limit_order_cancel_operation)operationType.operationContent;

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
            operations.call_order_update_operation operation = (operations.call_order_update_operation)operationType.operationContent;

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
            operations.fill_order_operation operation = (operations.fill_order_operation)operationType.operationContent;

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
            asset_object sellObject = mMapId2AssetObject.get(assetSell.asset_id);
            asset_object receiveObject = mMapId2AssetObject.get(assetReceive.asset_id);

            asset_object.asset_object_legible legibleReceive = receiveObject.get_legible_asset_object(assetReceive.amount);
            asset_object.asset_object_legible legiblePays = sellObject.get_legible_asset_object(assetSell.amount);
            double fReceiverResult = legibleReceive.lCount + legibleReceive.lDecimal / receiveObject.get_scaled_precision();
            double fPayResult = legiblePays.lCount + legiblePays.lDecimal / sellObject.get_scaled_precision();

            double fExchangeRate =  fPayResult / fReceiverResult;

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

    @Override
    public void onShow() {
        super.onShow();
        BitshareData bitshareData = BitsharesWalletWraper.getInstance().getBitshareData();
        if (bitshareData != null && mTranactionsAdapter != null) {
            mTranactionsAdapter.notifyTransactionDataChanged(bitshareData);
        }
    }

    public TransactionsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TransactionsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TransactionsFragment newInstance(String param1, String param2) {
        TransactionsFragment fragment = new TransactionsFragment();
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
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mTranactionsAdapter = new TranactionsAdapter(getActivity());
        recyclerView.setAdapter(mTranactionsAdapter);


        return view;
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void notifyUpdate(){
        BitshareData bitshareData = BitsharesWalletWraper.getInstance().getBitshareData();
        if (bitshareData != null && mTranactionsAdapter != null) {
            mTranactionsAdapter.notifyTransactionDataChanged(bitshareData);
        }
    }
}
