#include "register_types.h"
#include "core/class_db.h"
#include "core/engine.h"
#ifdef IPHONE_ENABLED
#include "ios/godotrevenuecat.h"
#endif

void register_GodotRevenueCat_types() {
#ifdef IPHONE_ENABLED
    Engine::get_singleton()->add_singleton(Engine::Singleton("RevenueCat", memnew(GodotRevenueCat)));
#endif

}

void unregister_GodotRevenueCat_types() {}
