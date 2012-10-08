/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.UoR.secureAjax;


import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * 
 *
 * @author Hope Ton
 * @version $Id: UUIDKeyGenerator.java,v 1.1 2004/05/30 12:00:24 marsj Exp $
 */
public class UUIDgenerator 
{

	SecureRandom seeder;
	private String midValue;

	public UUIDgenerator()
		throws Exception
	{
		StringBuffer buffer = new StringBuffer(16);
		byte addr[] = InetAddress.getLocalHost().getAddress();
		buffer.append(toHex(toInt(addr), 8));
		buffer.append(toHex(System.identityHashCode(this), 8));
		midValue = buffer.toString();
		seeder = new SecureRandom();
		int node = seeder.nextInt();
	}
	
	/* (non-Javadoc)
	 * @see net.java.simple.access.persistence.id.KeyGenerator#generateKey()
	 */
	public Object generateKey() 
	{
		StringBuffer buffer = new StringBuffer(32);
		buffer.append(toHex((int)(System.currentTimeMillis() & -1L), 8));
		buffer.append(midValue);
		buffer.append(toHex(seeder.nextInt(), 8));
		return buffer.toString();
	}
	
	/**
	 * 
	 * @param value
	 * @param length
	 * @return
	 */
	private String toHex(int value, int length)
	{
		char hexDigits[] = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
			'A', 'B', 'C', 'D', 'E', 'F'
		};
		StringBuffer buffer = new StringBuffer(length);
		int shift = length - 1 << 2;
		for(int i = -1; ++i < length;)
		{
			buffer.append(hexDigits[value >> shift & 0xf]);
			value <<= 4;
		}

		return buffer.toString();
	}
	
	/**
	 * 
	 * @param bytes
	 * @return
	 */
	private static int toInt(byte bytes[])
	{
		int value = 0;
		for(int i = -1; ++i < bytes.length;)
		{
			value <<= 8;
			int b = bytes[i] & 0xff;
			value |= b;
		}

		return value;
	}

	/* (non-Javadoc)
	 * @see net.java.simple.access.persistence.id.KeyGenerator#getLogicName()
	 */
	public String getLogicName() {
		
		return "UUIDKeyGenerator";
	}

}
