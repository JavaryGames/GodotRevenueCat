#ifndef __GODOTREVENUECAT_H__
#define __GODOTREVENUECAT_H__

#include "core/reference.h"

class GodotRevenueCat : public Reference {

    GDCLASS(GodotRevenueCat, Reference);

    int instanceId;

protected:
    static void _bind_methods();


public:

    // Place all methods headers here
    void init(const int godotId, const String &api_key, const bool is_debug);
    //purchases
    void checkSubscriptionStatus(const String &subscription_id);



    GodotRevenueCat();
    ~GodotRevenueCat();

};

#endif