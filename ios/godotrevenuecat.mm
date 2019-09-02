#include "godotrevenuecat.h"
#import "app_delegate.h"

#ifdef __OBJC__
#import <Purchases/Purchases.h>
#endif


GodotRevenueCat::GodotRevenueCat() {}

GodotRevenueCat::~GodotRevenueCat() {}


void GodotRevenueCat::init(const int godotId, const String &api_key, const bool is_debug = false){
    if (is_debug){
        RCPurchases.debugLogsEnabled = YES;
    }
    instanceId = godotId;
    NSString *ns_api_key = [NSString stringWithCString: api_key.utf8().get_data()];
    //NSString *ns_app_user_id = [NSString stringWithCString: app_user_id.utf8().get_data()];
    [RCPurchases configureWithAPIKey:ns_api_key appUserID:nil];
}


void GodotRevenueCat::checkSubscriptionStatus(const String &subscription_id){
    NSString *ns_subscription_id = [NSString stringWithCString: subscription_id.utf8().get_data()];
    [[RCPurchases sharedPurchases] purchaserInfoWithCompletionBlock: ^(RCPurchaserInfo * purchaserInfo, NSError * error) {
        if (error){
            Object *obj = ObjectDB::get_instance(instanceId);
            obj->call_deferred(String("revenuecat_check_subscription_failed"), [error.localizedDescription UTF8String]);
            return;
        }
        Dictionary info = Dictionary();
        info["isActive"] = purchaserInfo.entitlements.all[ns_subscription_id].isActive ? true : false;
        info["productIdentifier"] = [purchaserInfo.entitlements.all[ns_subscription_id].productIdentifier UTF8String];
        info["willRenew"] = purchaserInfo.entitlements.all[ns_subscription_id].willRenew ? true : false;
        info["isSandbox"] = purchaserInfo.entitlements.all[ns_subscription_id].isSandbox ? true : false;
        NSDate* lastTransactionDate = purchaserInfo.entitlements.all[ns_subscription_id].latestPurchaseDate;
        info["latestPurchaseDate"] = String::utf8([[NSString stringWithFormat:@"%.0f", [lastTransactionDate timeIntervalSince1970]] UTF8String]);
        NSDate* originalTransactionDate = purchaserInfo.entitlements.all[ns_subscription_id].originalPurchaseDate;
        info["originalPurchaseDate"] = String::utf8([[NSString stringWithFormat:@"%.0f", [originalTransactionDate timeIntervalSince1970]] UTF8String]);

        Object *obj = ObjectDB::get_instance(instanceId);
        obj->call_deferred(String("revenuecat_check_subscription_succeeded"), info["isActive"], info);
    }];
}



void GodotRevenueCat::_bind_methods() {
    ClassDB::bind_method("init", &GodotRevenueCat::init);
    ClassDB::bind_method("checkSubscriptionStatus", &GodotRevenueCat::checkSubscriptionStatus);

}
