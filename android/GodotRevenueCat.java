package org.godotengine.godot;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.Purchase;
import com.revenuecat.purchases.EntitlementInfo;
import com.revenuecat.purchases.Entitlement;
import com.revenuecat.purchases.PurchaserInfo;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.MakePurchaseListener;
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener;
import com.revenuecat.purchases.interfaces.ReceiveEntitlementsListener;

public class GodotRevenueCat extends Godot.SingletonBase {
    private Activity activity = null;
    private int instanceId = 0;
    private Context context;

    private static boolean _running = true;

    private Map<String, Entitlement> entitlements = null;
    
    static public Godot.SingletonBase initialize(Activity p_activity) {
        return new GodotRevenueCat(p_activity);
    }

    public GodotRevenueCat(Activity p_activity) {
        registerClass("RevenueCat", new String[]{
            "init",
            "purchase_product",
            "restore_transactions",
            "check_subscription_status"
        });

        activity = p_activity;
        context = activity.getApplicationContext();

    }

    public void init(int instanceId, String apiKey, boolean p_isDebug) {
        this.instanceId = instanceId;
        Purchases.setDebugLogsEnabled(p_isDebug);
        Purchases.configure(this.context, apiKey, null);
        Purchases.getSharedInstance().getEntitlements(new ReceiveEntitlementsListener() {
            @Override
            public void onReceived(@Nullable Map<String, Entitlement> entitlements) {
                setEntitlements(entitlements);
            }
        
            @Override
            public void onError(@NonNull PurchasesError error) {}
        });
    }

    public void purchase_product(final String product_id, String revenue_cat_offering_id){
        if (this.entitlements.get("subscription").getOfferings().get(revenue_cat_offering_id) == null
            || this.entitlements.get("subscription").getOfferings().get(revenue_cat_offering_id).getSkuDetails() == null){
            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_failed", new Object[]{product_id, "Invalid Product."});
            return;
        }
        Purchases.getSharedInstance().makePurchase(
            this.activity,
            this.entitlements.get("subscription").getOfferings().get(revenue_cat_offering_id).getSkuDetails(),
            new MakePurchaseListener() {
                @Override
                public void onCompleted(@NonNull Purchase purchase, @NonNull PurchaserInfo purchaserInfo) {
                    Dictionary info = create_info_dict(purchaserInfo.getEntitlements().get(product_id), product_id);
                    if (purchaserInfo.getEntitlements().get(product_id).isActive()) {
                    // Unlock that great "pro" content
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_succeeded", new Object[]{product_id, info});
                        }
                    }
                }

                @Override
                public void onError(@NonNull PurchasesError error, Boolean userCancelled) {
                    // No purchase
                    if (userCancelled){
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_failed", new Object[]{product_id, "Purchase cancelled by user"});
                        }
                    }else{
                        if (instanceId != 0){
                            GodotLib.calldeferred(instanceId, "revenuecat_purchase_product_failed", new Object[]{product_id, error.getMessage()});
                        }
                    }
                }
            }
        );
    }

    public void restore_transactions(){
        Purchases.getSharedInstance().restorePurchases(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                //... check purchaserInfo to see if entitlement is now active
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_restore_transactions_succeeded", new Object[]{});
                }
            }
            @Override
            public void onError(@NonNull PurchasesError error) {
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_restore_transactions_failed", new Object[]{error.getMessage()});
                }
            }
        });
    }

    public void check_subscription_status(final String subscription_id) {
        Purchases.getSharedInstance().getPurchaserInfo(new ReceivePurchaserInfoListener() {
            @Override
            public void onReceived(@NonNull PurchaserInfo purchaserInfo) {
                // access latest purchaserInfo
                Dictionary info = create_info_dict(purchaserInfo.getEntitlements().get(subscription_id), subscription_id);
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_check_subscription_succeeded", new Object[]{info.get("isActive"), info});
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                if (instanceId != 0){
                    GodotLib.calldeferred(instanceId, "revenuecat_check_subscription_failed", new Object[]{error.getMessage()});
                }
            }
        });
    }

    private Dictionary create_info_dict(EntitlementInfo entitlementInfo, String product_id) {
        Dictionary info = new Dictionary();
        if(entitlementInfo != null){
            info.put("isActive", entitlementInfo.isActive());
            info.put("productId", product_id);
            info.put("identifier", entitlementInfo.getIdentifier());
            info.put("productIdentifier", entitlementInfo.getProductIdentifier());
            info.put("willRenew", entitlementInfo.getWillRenew());
            info.put("isSandbox", entitlementInfo.isSandbox());
            info.put("periodType", (int)entitlementInfo.getPeriodType().ordinal());
            info.put("latestPurchaseDate", entitlementInfo.getLatestPurchaseDate().getTime() + "");
            info.put("originalPurchaseDate", entitlementInfo.getOriginalPurchaseDate().getTime() + "");
            if(entitlementInfo.getExpirationDate() != null){
                info.put("expirationDate", entitlementInfo.getExpirationDate().getTime() + "");
            }else{
                info.put("expirationDate", "0");
            }
        }else{
            info.put("isActive", false);
            info.put("productId", product_id);
            info.put("identifier", null);
            info.put("productIdentifier", null);
            info.put("willRenew", null);
            info.put("isSandbox", null);
            info.put("periodType", null);
            info.put("latestPurchaseDate", "0");
            info.put("originalPurchaseDate", "0");
            info.put("expirationDate", "0");
        }

        return info;
    }

    public void setEntitlements(Map<String, Entitlement> entitlements){
        this.entitlements = entitlements;
    }
}