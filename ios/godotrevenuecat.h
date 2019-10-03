#ifndef __GODOTREVENUECAT_H__
#define __GODOTREVENUECAT_H__

#include "core/reference.h"

class GodotRevenueCat : public Reference {

    GDCLASS(GodotRevenueCat, Reference);

    int instanceId;
    bool isDebug;

protected:
    static void _bind_methods();


public:

    // Place all methods headers here
    void init(const int godotId, const String &api_key, const bool is_debug);
    void purchase_product(const String &product_id, const String &revenue_cat_offering_id);
    void restore_transactions();
    void check_subscription_status(const String &subscription_id);



    GodotRevenueCat();
    ~GodotRevenueCat();

};

#endif