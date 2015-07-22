    Change Log
Version 5.2 introduced September 7, 2005
	Diagnostic program updated so a new picture is received from rover roughly
        twice every second. This allows user to use picture window for navigation.  

        Changed the VERSION constant in PERConstants to a String rather than a
	double to accommodate version numbers with multiple periods.

        Deprecated the WEBCAM_CYCLY_NOPIC constant in RoverState and replaced it
        with WEBCAM_CYCLE_NOPIC. Added UV_ON contant to RoverState.

        Changed the values of the WEBCAM_* constants in RoverState to match the
        corresponding constants in WebCam.h on the rover (as of Stargate code 
        version 2.2). Suspect they may have become unmatched when resource 
        locking was introduced in Stargate code version 2.0.1. The WEBCAM_*
        constants are no longer guaranteed to be back compatible with old versions
        of the Stargate code before version 2.1. We do not currently use any of
        the WEBCAM_* contants by name so the change should not be a problem from
        the Exhibit software point of view. 

        Added "Return to Attract Loop" option to popup menu in MissionCentral.

        Made some changes to improve Exhibit performance at shows and 
	demonstrations. The initial trigger to exit the attract loop is now a 
	mouse release rather than a mouse click so that mouse does not have to be 
	stationary to trigger the panorama. The panorama screen now uses a wait
	cursor rather than a hand cursor.

        Removed several old images that are no longer used.

        Updated the SmartWanderAction so that the rover checks that new headings
        are unobstructed before executing a turn. Now the rover also examines
	obstacles in its path by turning on the light and taking a picture.

        Fixed the Leash example in the Examples program.

        Updated some comments throughout to make the API more clear.

        Slightly changed format of Log output in Sequencer. Added a comma and space 
        to the "starting a new mission" line.

        Fixed a bug that caused a resource conflict error when the user selected a
        zero degree turn in the Exhibit program. This "bug" was present in the 
        Exhibit software from the beginning but became noticeable when resource
        locking was introduced in Stargate code version 2.0.1. There is a hack in
        the doTurnTo function of highLevel.cpp which avoids the resource conflict
        when a non-zero degree turn is executed. This hack can be removed now 
        that the problem is solved in the Exhibit software itself. However,
        removing the hack will make new versions of the Stargate code incompatible
        with previous version of the Exhibit software. Exhibit 5.0 Patch A and
        Exhibit 5.1 Patch A are patches for this bug for versions 5.0 through 5.1.1
        of the PER software.

        Increased the timeout for scans so that rovers running exhibit3.0 and later
        will not timeout while scanning for rocks. Older versions of the software
        will not be compatible with rover firmware version exhibit3.0.

        Known Bug: In the Exhibit program, when the rover scans and sees only a
	smooth floor, it results in an "Out of Bounds" message because the floor 
	is not distinguished from a wall. The case of seeing the floor should be
	distinguished from seeing a wall and the "Found Nothing" message should be
	displayed. Bug since the first release. 

	Known Bug: The estimated time to complete Actions such as DriveToAction and
	TurnToAction is not correct when using a PER with the turbo drive motors.
	The getTime and getTimeRemaining functions in these Actions will be 
	inaccurate.
	
	Known Bug: When using the "Test Panorama" button on the Advanced tab of the
	Exhibit program's start up dialog, the test panorama dialog boxes do not
	close when running with Java 5.0. The dialog boxes do close when running
	with Java 1.4.2.

Diagnostic Upgrade 1 for PERFilesystem 5.1 introduced September 29, 2004

	This upgrade adds an image Auto-Update feature to the Diagnostic program.
	This upgrade is tested to work with PERFilesystem version 5.1 or 5.1.1 but
	does not work with earlier versions.

Exhibit 5.1 Patch A introduced February 24, 2005
	This is a patch for the Exhibit program in version 5.1 and 5.1.1 of the
	PER software. This patch fixes a bug that caused a resource conflict error
	when the user executed a mission with a zero degree turn (when using a 
	rover with Stargate code version 2.0.1 through 2.2).

	Without this patch version 5.1 and 5.1.1 of the Exhibit software will not
	work with rovers running Stargate code version 3.0 or later. 

Version 5.1.1 introduced July 07, 2004
	RoverController is now smarter about determining which direction you want
	to turn.

	RoverState now properly tracks the coordinates of non-straight line driveto.

	Floats generally changed to doubles in the new functions to make arithmetic
	more java friendly.

	MoveToAction added, which takes a destination location and orientation and
	moves there.
 
Version 5.1 introduced June 29, 2004
        Coordinate system added.  The methods setPosition and getPosition in
	RoverState access the position stored in HighLevelState.  The X axis runs 
	parallel to the rover.  This requires the stayton firmware 2.1 update for
	use.

        Calibration program updated to allow small negative drive and turn speeds to 
        accomodate the new faster motors.  These also require firmware 2.1 for use.

        Fixed bug with BasicGUI so that clicking a disabled button now does nothing.
        Fixed the command1 example in BasicGUI so that it matches the Beginner's
        Guide document. Call stop on exit. Improved autoscrolling.

        Diagnostic program displays rover code version number in title bar.

        The kill method in WallFollowAction is now functional.

        Replaced TrackWindow with Vision. Vision supports motion detection 
        and line following as well as color tracking.

Version 5.1 beta introduced May 7, 2004
        Introduced BasicGUI as a graphical user interface for people wishing
        to program a PER. Examples is a duplicate of BasicGUI with sample programs.

        Added functions to Rover to allow saving images, scheduling execution, 
        and taking time lapse pictures. Added functions to RoverController to
        enable color tracking, motion detection, brightness sensing, and accessing
        the drive and turn calibration stored on the rover. Added getRecentImage
        and getImageUpdateTime methods to the Action interface. Added 
	SmartWanderAction, DanceAction, DetectMotionAction, CreepAction, and 
	WallFollowAction. Note that the DetectMotionAction and WallFollowAction will 
	not work util version 2.0.0 or later of the Stargate code is released and 
	installed on the robot.

        Added TrackWindow (and supporting file TrackFinder) as an easy way for
        people to test color tracking.

        Moved Filter and ImagePreview from exhibit.GUI package to rover package.

        Added an optional JTextArea to Log.

        Added new RoverState constants NOT_CONNECTED and RESOURCE_CONFLICT.
        Removed constants that are no longer used.

Version 5.0 introduced March 4 2004
        Note That version 5.0 was internally labeled 4.4 so the version number
        displayed and the number listed in the log files will say version 4.4
        when it should read 5.0.

	New file system organization. 

	Updated Exhibit batch files to correspond with new file system organization. 
	Kiosk desktops should be updated with the new files. 

	Version number is now recorded in the log files. Log files for the Exhibit
	program now include the word exhibit in the file name.

        Fixed bug introduced in version 4.3 in which motion events were consumed in 
        MissionCentral and MissionCentral would always timeout when the time had 
        expired regardless of mouse motion. Also added functionality so that clicks
        and motion over the panorama and map will reach the timeout motion listener
        and prevent timeouts.

        Panorama test windows are now disposed of when closed by the user so that
        memory can be reclaimed.

Version 4.4 introduced February 4 2004
	Fixed update issue which caused the rover's actions to lag behind the
	rover status displayed on screen.

	Fixed bug in Calibration software that caused incorrect values to be 
	displayed when loading a calibration file from a rover on which the tilt 
	servo's range had been changed. Calibration will now work correctly for old 
	and new tilt servos.

	Removed unused GUI files.

Version 4.3 introduced January 16 2004
	PER4.3 introduces a popup menu triggered by a right click in Mission
	Central. The four menu choices are "Navigate and find rock", "Navigate
	only", "Turn only", and "Take new panorama." Selecting one of these options
	will skip the final countdown and go directly back to taking a new
	panorama. The mission clock continues to run until a mission is completed
	through the Go button (or a timeout).

Version 4.2b introduced January 13 2004
	The panorama tilt angle is now adjustable on the expert tab of the Exhibit

	Also fixed a bug. If there was an obstacle during approach rock, it used to 
	think it was an error and now assumes it is the rock.

	Also, this updates rover firmware to Cereb12 for cerebellum and Exhibit11
	for Stayton. The latter slows down drive motors to 80%PWM so that at stall 
	the gears don't break. 	The former slows down servo motion (steering and 
	tilt) significantly and remembers previously commanded position, to reduce 
	force and wear on the tilt servo in particular.
