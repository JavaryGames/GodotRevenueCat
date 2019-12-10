
def can_build(env, platform):
    return platform == "iphone"

def configure(env):
    if (env['platform'] == 'android'):
        env.android_add_dependency("implementation 'com.revenuecat.purchases:purchases:3.0.3'")
        env.android_add_java_dir("android/src/")
    if env['platform'] == "iphone":
        env.Append(FRAMEWORKPATH=['ios/lib'])
        env.Append(LINKFLAGS=['-ObjC', '-framework', 'Purchases'])
