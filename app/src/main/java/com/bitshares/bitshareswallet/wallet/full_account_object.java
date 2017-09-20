package com.bitshares.bitshareswallet.wallet;


import com.bitshares.bitshareswallet.wallet.graphene.chain.limit_order_object;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class full_account_object {
    public account_object account;
    public List<limit_order_object> limit_orders;

    public static class deserializer implements JsonDeserializer<full_account_object> {
        @Override
        public full_account_object deserialize(JsonElement json, Type typeOfT,
                                               JsonDeserializationContext context)
                throws JsonParseException {
            if (!json.isJsonArray()) {
                throw new JsonParseException("invalid full account entry");
            }
            JsonArray arr = json.getAsJsonArray();
            if (arr.size() < 2) {
                throw new JsonParseException("unexpected element count in account entry");
            }
            JsonObject fullAccountJson = arr.get(1).getAsJsonObject();
            JsonObject accountJson = fullAccountJson.getAsJsonObject("account");
            if (accountJson == null) {
                throw new JsonParseException("missing 'account' field");
            }
            JsonArray limitOrdersJson = fullAccountJson.getAsJsonArray("limit_orders");
            if (limitOrdersJson == null || !limitOrdersJson.isJsonArray()) {
                throw new JsonParseException("missing 'limit_orders' field");
            }
            full_account_object fullAccountObject = new full_account_object();
            fullAccountObject.account = context.deserialize(accountJson, account_object.class);
            fullAccountObject.limit_orders = new ArrayList<>(limitOrdersJson.size());
            for (int i = 0; i < limitOrdersJson.size(); i++) {
                limit_order_object limitOrder = context.deserialize(
                        limitOrdersJson.get(i).getAsJsonObject(), limit_order_object.class);
                fullAccountObject.limit_orders.add(limitOrder);
            }
            return fullAccountObject;
        }
    }
}
