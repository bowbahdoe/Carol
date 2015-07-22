//
//  Time.java
//  Trikebot
//
//  Created by Andrés Santiago Pérez-Bergquist on Wed Jun 19 2002.
//  Copyright (c) 2002. All rights reserved.
//

package PER.rover.control;

import java.util.*;

/** Contains just two methods - <code>current</code> and <code>sleep</code>. 
 * <code>current</code> returns the current time in milliseconds, and
 * <code>sleep</code> sleeps a number of milliseconds.
 *
 *@author Andrés Santiago Pérez-Bergquist
 */


public class Time extends java.lang.Object
{
	/* Returns the current time in milliseconds */
	public static long Current()
	{
        return (new Date()).getTime();
    }

	/* Pauses for <code>msecs</code> milliseconds */
	public static void Sleep(long msecs)
	{
		try { Thread.sleep(msecs); } catch (Exception e) {}
		return;
	}
}
