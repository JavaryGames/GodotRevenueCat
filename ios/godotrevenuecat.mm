#include "godotrevenuecat.h"
#import "app_delegate.h"

#ifdef __OBJC__
#import <Purchases/Purchases.h>
#endif


Dictionary create_info_dict(RCEntitlementInfo *entitlement, NSString *product_id);



GodotRevenueCat::GodotRevenueCat() {}

GodotRevenueCat::~GodotRevenueCat() {}


void GodotRevenueCat::init(const int godotId, const String &api_key, const bool is_debug = false){
    if (is_debug){
        RCPurchases.debugLogsEnabled = YES;
        isDebug = true;
    }
    instanceId = godotId;
    NSString *ns_api_key = [NSString stringWithCString: api_key.utf8().get_data()];
    //NSString *ns_app_user_id = [NSString stringWithCString: app_user_id.utf8().get_data()];
    [RCPurchases configureWithAPIKey:ns_api_key appUserID:nil];
}


void GodotRevenueCat::purchase_product(const String &product_id, const String &revenue_cat_offering_id){
    NSString *ns_product_id = [NSString stringWithCString: product_id.utf8().get_data()];
    NSString *ns_product_offering_id = [NSString stringWithCString: revenue_cat_offering_id.utf8().get_data()];

    __block SKProduct *product;
    // Get available products
    [[RCPurchases sharedPurchases] entitlementsWithCompletionBlock:^(RCEntitlements *entitlements, NSError *error) {
        if (error){
            if (isDebug){
                NSLog(@"Error: %@", error);
            }
            Object *obj = ObjectDB::get_instance(instanceId);
            obj->call_deferred(String("revenuecat_purchase_product_failed"), [ns_product_id UTF8String], [error.localizedDescription UTF8String]);
            return;
        }
        // Get SKProduct
        product = entitlements[@"subscription"].offerings[ns_product_offering_id].activeProduct;
        if (product == nil) {
            Object *obj = ObjectDB::get_instance(instanceId);
            obj->call_deferred(String("revenuecat_purchase_product_failed"), [ns_product_id UTF8String], @"Subscription provider is not available at the moment.");
            return;
        }
        // Make purchase
        [[RCPurchases sharedPurchases] makePurchase:product withCompletionBlock:^(SKPaymentTransaction *transaction, RCPurchaserInfo *purchaserInfo, NSError *error, BOOL cancelled) {
            if (error){
                if (isDebug){
                    NSLog(@"Error: %@", error);
                }
                Object *obj = ObjectDB::get_instance(instanceId);
                obj->call_deferred(String("revenuecat_purchase_product_failed"), [ns_product_id UTF8String], [error.localizedDescription UTF8String]);
                return;
            }
            if (!purchaserInfo.entitlements.all[@"subscription"].isActive) {
                Object *obj = ObjectDB::get_instance(instanceId);
                obj->call_deferred(String("revenuecat_purchase_product_failed"), [ns_product_id UTF8String], String("Purchase was not successful"));
            }else{
                // User is "premium"
                Dictionary info = create_info_dict(purchaserInfo.entitlements.all[ns_product_id], ns_product_id);
                Object *obj = ObjectDB::get_instance(instanceId);
                obj->call_deferred(String("revenuecat_purchase_product_succeeded"), [ns_product_id UTF8String], info);
            }
        }];
    }];
}


void GodotRevenueCat::restore_transactions(){
    [[RCPurchases sharedPurchases] restoreTransactionsWithCompletionBlock:^(RCPurchaserInfo *purchaserInfo, NSError *error) {
        if (error){
            if (isDebug){
                NSLog(@"Error: %@", error);
            }
            Object *obj = ObjectDB::get_instance(instanceId);
            obj->call_deferred(String("revenuecat_restore_transactions_failed"), [error.localizedDescription UTF8String]);
            return;
        }
        Object *obj = ObjectDB::get_instance(instanceId);
        obj->call_deferred(String("revenuecat_restore_transactions_succeeded"));
    }];
} 


void GodotRevenueCat::check_subscription_status(const String &subscription_id){
    NSString *ns_subscription_id = [NSString stringWithCString: subscription_id.utf8().get_data()];
    [[RCPurchases sharedPurchases] purchaserInfoWithCompletionBlock: ^(RCPurchaserInfo * purchaserInfo, NSError * error) {
        if (error){
            if (isDebug){
                NSLog(@"Error: %@", error);
            }
            Object *obj = ObjectDB::get_instance(instanceId);
            obj->call_deferred(String("revenuecat_check_subscription_failed"), [error.localizedDescription UTF8String]);
            return;
        }
        Dictionary info = create_info_dict(purchaserInfo.entitlements.all[ns_subscription_id], ns_subscription_id);
        Object *obj = ObjectDB::get_instance(instanceId);
        obj->call_deferred(String("revenuecat_check_subscription_succeeded"), info["isActive"], info);
    }];
}


Dictionary create_info_dict(RCEntitlementInfo *entitlement, NSString *product_id){
    Dictionary info = Dictionary();
    info["isActive"] = entitlement.isActive ? true : false;
    info["productId"] = [product_id UTF8String];
    info["identifier"] = [entitlement.identifier UTF8String];
    info["productIdentifier"] = [entitlement.productIdentifier UTF8String];
    info["willRenew"] = entitlement.willRenew ? true : false;
    info["isSandbox"] = entitlement.isSandbox ? true : false;
    info["periodType"] = (int) entitlement.periodType;
    NSDate* lastTransactionDate = entitlement.latestPurchaseDate;
    info["latestPurchaseDate"] = String::utf8([[NSString stringWithFormat:@"%.0f", [lastTransactionDate timeIntervalSince1970]] UTF8String]);
    NSDate* originalTransactionDate = entitlement.originalPurchaseDate;
    info["originalPurchaseDate"] = String::utf8([[NSString stringWithFormat:@"%.0f", [originalTransactionDate timeIntervalSince1970]] UTF8String]);
    NSDate* expirationDate = entitlement.expirationDate;
    info["expirationDate"] = String::utf8([[NSString stringWithFormat:@"%.0f", [expirationDate timeIntervalSince1970]] UTF8String]);

    return info;
}


void GodotRevenueCat::get_products(const String &entitlement, const String &offerings){
    [[RCPurchases sharedPurchases] entitlementsWithCompletionBlock:^(RCEntitlements *entitlements, NSError *error) {
        // Get SKProduct
        Dictionary products = Dictionary();
        for(NSString* offer in [offerings componentsSeparatedByString:@","]){
            if([entitlements[entitlement].offerings objectForKey:offer] == nil){
                continue;
            }
            products["offer"] = entitlements[entitlement].offerings[offer].activeProduct;
        }
        
        obj->call_deferred(String("revenuecat_get_products"), products);
        return;
        }
    }
}


void GodotRevenueCat::_bind_methods() {
    ClassDB::bind_method("init", &GodotRevenueCat::init);
    ClassDB::bind_method("purchase_product", &GodotRevenueCat::purchase_product);
    ClassDB::bind_method("restore_transactions", &GodotRevenueCat::restore_transactions);
    ClassDB::bind_method("check_subscription_status", &GodotRevenueCat::check_subscription_status);
    ClassDB::bind_method("get_products", &GodotRevenueCat::get_products);
}
