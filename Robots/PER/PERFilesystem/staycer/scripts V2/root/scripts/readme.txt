This file describes how to use the scripts that are located in this directory.

autorun
	Runing this scipt will cause the exhibit code to automatically run when the Stargate is turned on.  It will run the file that is at /root/exhibit

noAutorun
	This script undoes the autorun script.  By calling this script, the exhibit code will not automatically run when the Stargate is turned on.

netStation [network]
	This sets the Stargate into Managed mode where it will use base stations.  It turns DHCP on and connects to the network you specify.  If no network is specified, it will connect to CMU.
	The new settings immediately.

netP2P network subnet address
       This function requires you to type in 3 arguments.  It will go into peer to peer (Ad-Hoc) mode with DHCP off.  It will be on the network you specify in the first argument and the IP address will be 192.168.subnet.address.  It will check to make sure that subnet and address are between 0 and 254.
	The new settings immediately.

setIP subnet address
      This function is the same as netP2P except that it will only change the IP address.  It turns DHCP off.
	The new settings immediately.

setDefaultP2P network subnet address
	The arguments are exactly the same as netP2P.  The only difference is that this function does not change the network.  It saves a default peer to peer configutation so that you can easily change between networks and not have to worry about mistyping a network name or IP address.  Call restoreP2P to use these settings

restoreP2P
	This function will set the network to the peer to peer settings specified by setDefaultP2P.  It returns an error if setDefaultP2P was not called.  
	The new settings immediately.

viewDefaultP2P
	This function lists the network settings that were specified by the last call to setDefaultP2P.  An error is returned if setDefaultP2P was never called.