print("Initializing Carol")
isCreateConnected = False
isLinkConnected = False
try:
    from kovan import create_connect, set_create_distance, set_create_normalized_angle
    isLinkConnected = True
    if(create_connect()>=0):
        isCreateConnected = True
    if(isCreateConnected):
        set_create_distance(0);
        set_create_normalized_angle(0)
except ImportError:
    pass
