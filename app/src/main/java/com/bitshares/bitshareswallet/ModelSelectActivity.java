package com.bitshares.bitshareswallet;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ModelSelectActivity extends AppCompatActivity {
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_select);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.textViewAccountModel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModelSelectActivity.this, ImportActivty.class);
                intent.putExtra("model", ImportActivty.ACCOUNT_MODEL);
                startActivity(intent);
            }
        });

        findViewById(R.id.textViewWalletModelWifKey).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModelSelectActivity.this, ImportActivty.class);
                intent.putExtra("model", ImportActivty.WALLET_MODEL_WIF_KEY);
                startActivity(intent);
            }
        });

        findViewById(R.id.textViewWalletModelBin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModelSelectActivity.this, ImportActivty.class);
                intent.putExtra("model", ImportActivty.WALLET_MODEL_BIN_FILE);
                startActivity(intent);
            }
        });

        findViewById(R.id.textViewWalletModelBrainKey).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ModelSelectActivity.this, ImportActivty.class);
                intent.putExtra("model", ImportActivty.WALLET_MODEL_BRAIN_KEY);
                startActivity(intent);
            }
        });
    }
}
