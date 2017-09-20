package com.bitshares.bitshareswallet;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bitshares.bitshareswallet.wallet.BitshareData;
import com.bitshares.bitshareswallet.wallet.BitsharesWalletWraper;
import com.bitshares.bitshareswallet.wallet.graphene.chain.signed_transaction;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WalletFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WalletFragment extends BaseFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private BtsFragmentPageAdapter mWalletFragmentPageAdapter;
    private Handler mHandler = new Handler();

    private SendFragment mSendFragment;

    public WalletFragment() {
        // Required empty public constructor
    }


    private Runnable mRefreshRunnable = new Runnable() {
        private int nCount = 0;
        @Override
        public void run() {
            if (nCount == 0) {
                fetchData(false);
            } else {
                fetchData(true);
            }
            mHandler.postDelayed(this, 15 * 1000);
        }
    };

    public void onNewIntent(Intent intent){
        String strAction = intent.getStringExtra("action");
        if (TextUtils.isEmpty(strAction) == false) {
            mViewPager.setCurrentItem(2);
            String strName = intent.getStringExtra("name");
            int nAmount = Integer.valueOf(intent.getStringExtra("amount"));
            String strUnit = intent.getStringExtra("unit");
            mSendFragment.processDonate(strName, nAmount, strUnit);
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WalletFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WalletFragment newInstance(String param1, String param2) {
        WalletFragment fragment = new WalletFragment();
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
    public void onShow() {
        super.onShow();
        mHandler.post(mRefreshRunnable);
    }

    @Override
    public void onHide() {
        super.onHide();
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    public void notifyTransferComplete(signed_transaction signedTransaction) {
        // 沿用该线程，阻塞住了系统来进行数据更新
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitshareData bitshareData = BitsharesWalletWraper.getInstance().prepare_data_to_display(true);
                notifyBitshareDatachanged(bitshareData);
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.fw_viewPager);

        mTabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        Resources res = getResources();
        mWalletFragmentPageAdapter = new BtsFragmentPageAdapter(getFragmentManager());
        mWalletFragmentPageAdapter.addFragment(BalancesFragment.newInstance("", ""), res.getString(R.string.tab_balances));
        mWalletFragmentPageAdapter.addFragment(TransactionsFragment.newInstance("", ""), res.getString(R.string.tab_transactions));
        mSendFragment = SendFragment.newInstance("", "");
        mWalletFragmentPageAdapter.addFragment(mSendFragment, res.getString(R.string.tab_send));

        mViewPager.setAdapter(mWalletFragmentPageAdapter);
        initPager(mViewPager, mWalletFragmentPageAdapter);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() != 2) {
                    MainActivity.hideSoftKeyboard(mTabLayout, getActivity());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mTabLayout.setupWithViewPager(mViewPager);
        return view;
    }

    public void fetchData(final boolean bRefresh) {
        BitshareData bitshareData = null;
        if (bRefresh == false) {
            bitshareData = BitsharesWalletWraper.getInstance().getBitshareData();
        }

        if (bitshareData == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BitshareData bitshareData = null;
                    int nRet = BitsharesWalletWraper.getInstance().build_connect();
                    if (nRet == 0) {
                        bitshareData = BitsharesWalletWraper.getInstance().prepare_data_to_display(true);
                    }

                    if (bitshareData != null) {
                        notifyBitshareDatachanged(bitshareData);
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!getActivity().isFinishing()) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                    builder.setPositiveButton(R.string.connect_fail_dialog_retry, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            fetchData(bRefresh);
                                        }
                                    });
                                    builder.setMessage(R.string.connect_fail_message);
                                    builder.show();
                                }
                            }
                        });
                    }
                }
            }).start();
        } else {
            notifyBitshareDatachanged(bitshareData);
        }
    }

    private void notifyBitshareDatachanged(BitshareData bitshareData) {
        final BitshareData.TotalBalances totalBalances = bitshareData.getTotalAmountBalances();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(getView()==null)
                    return;
                TextView textViewBalances = (TextView) getView().findViewById(R.id.textTotalBalance);
                textViewBalances.setText(totalBalances.strTotalBalances);

                TextView textViewCurency = (TextView) getView().findViewById(R.id.textViewCurrency);

                String strTotalCurrency = String.format(
                        Locale.ENGLISH,
                        "= %s (%s)",
                        totalBalances.strTotalCurrency,
                        totalBalances.strExchangeRate
                );

                textViewCurency.setText(strTotalCurrency);
                textViewCurency.setVisibility(View.VISIBLE);

                mWalletFragmentPageAdapter.notifyUpdate();
            }
        });
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
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
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
}
