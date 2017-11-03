package com.bitshares.bitshareswallet;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bitshares.bitshareswallet.room.BitsharesBalanceAsset;
import com.bitshares.bitshareswallet.viewmodel.WalletViewModel;

import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BalancesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BalancesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BalancesFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private BalancesAdapter mBalancesAdapter;

    private OnFragmentInteractionListener mListener;

    class BalanceItemViewHolder extends RecyclerView.ViewHolder {
        public View view;
        public TextView viewNumber;
        public TextView viewUnit;
        public TextView viewEqual;
        public TextView viewConvertNumber;
        public TextView viewConvertUnit;

        public BalanceItemViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            viewNumber = (TextView) itemView.findViewById(R.id.textViewNumber);
            viewUnit = (TextView) itemView.findViewById(R.id.textViewUnit);
            viewEqual = (TextView) itemView.findViewById(R.id.textViewEqual);
            viewConvertNumber = (TextView) itemView.findViewById(R.id.textViewNumber2);
            viewConvertUnit = (TextView) itemView.findViewById(R.id.textViewUnit2);
        }
    }

    class BalancesAdapter extends RecyclerView.Adapter<BalanceItemViewHolder> {
        private List<BitsharesBalanceAsset> bitsharesBalanceAssetList;

        @Override
        public BalanceItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_balances, parent, false);
            return new BalanceItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BalanceItemViewHolder holder, int position) {
            BitsharesBalanceAsset bitsharesBalanceAsset = bitsharesBalanceAssetList.get(position);
            String strBalances = String.format(
                    Locale.ENGLISH,
                    "%.4f",
                    (float)bitsharesBalanceAsset.amount / bitsharesBalanceAsset.quote_precision
            );

            holder.viewNumber.setText(strBalances);
            holder.viewUnit.setText(bitsharesBalanceAsset.quote);
            if (bitsharesBalanceAsset.quote.compareTo("BTS") != 0) {
                int nResult = (int)Math.rint(bitsharesBalanceAsset.total / bitsharesBalanceAsset.base_precision);

                holder.viewEqual.setText("=");
                holder.viewConvertNumber.setText(Integer.valueOf(nResult).toString());
                holder.viewConvertUnit.setText("BTS");
            }

        }

        @Override
        public int getItemCount() {
            if (bitsharesBalanceAssetList == null) {
                return 0;
            } else {
                return bitsharesBalanceAssetList.size();
            }
        }

        public void notifyBalancesDataChanged(List<BitsharesBalanceAsset> bitsharesBalanceAssetList) {
            this.bitsharesBalanceAssetList = bitsharesBalanceAssetList;

            notifyDataSetChanged();
        }
    }

    public BalancesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BalancesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BalancesFragment newInstance(String param1, String param2) {
        BalancesFragment fragment = new BalancesFragment();
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
    public void onResume() {
        super.onResume();

        WalletViewModel walletViewModel = ViewModelProviders.of(getActivity()).get(WalletViewModel.class);
        walletViewModel.getBalanceData().observe(
                this, resourceBalanceList -> {
                    switch (resourceBalanceList.status) {
                        case SUCCESS:
                            mBalancesAdapter.notifyBalancesDataChanged(resourceBalanceList.data);
                            break;
                        case LOADING:
                            if (resourceBalanceList.data != null) {
                                mBalancesAdapter.notifyBalancesDataChanged(resourceBalanceList.data);
                            }
                            break;
                    }
                });
    }

    @Override
    public void onShow() {
        super.onShow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_balances, container, false);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBalancesAdapter = new BalancesAdapter();
        recyclerView.setAdapter(mBalancesAdapter);

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
    public void notifyUpdate() {

    }
}
