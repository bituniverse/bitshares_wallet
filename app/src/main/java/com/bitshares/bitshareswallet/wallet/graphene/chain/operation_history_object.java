package com.bitshares.bitshareswallet.wallet.graphene.chain;

import java.util.List;
import java.util.Objects;


public class operation_history_object {
    public String id;
    public operations.operation_type op;
    public int block_num;
    public int trx_in_block;
    public int op_in_trx;
    public int virtual_op;

}
