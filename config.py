
def can_build(env, platform):
    return platform == "iphone"

def configure(env):
    if env['platform'] == "iphone":
        env.Append(FRAMEWORKPATH=['ios/lib'])
        env.Append(LINKFLAGS=['-ObjC', '-framework', 'Purchases'])
