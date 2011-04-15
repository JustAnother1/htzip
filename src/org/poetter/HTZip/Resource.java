/*
 * Copyright (C) 2008  Lars Pötter <Lars_Poetter@gmx.de>
 * All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/> 
 * 
 */

/**
 * Resource.java
 */
package org.poetter.HTZip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 
 * A Resource represents the content requested by the Browser. 
 * This is actually a sort of InputStream 
 */
public class Resource
{
    // Resource is a...
    // File in the local File System
    private File        f        = null;
    // File in a local ZIP File
    private ZipFile     zf       = null;
    private ZipEntry    ze       = null;
    // Input Stream
    private InputStream in       = null;
    // .. at the Servers Location
    private String      Location = null;
    
    /** Resource is a file 
     * 
     */
    public Resource(File f, String Location)
    {
        this.f = f;
        this.Location = Location;
    }
    
    /** Resource is an Input Stream
     * 
     */
    public Resource(InputStream in, String Location)
    {
        this.in = in;
        this.Location = Location;
    }
    
    /** Resource is a File in a Zip Archive
     * 
     */
    public Resource(ZipFile zf, ZipEntry ze, String Location)
    {
        this.zf = zf;
        this.ze = ze;
        this.Location = Location;
    }
    
    /**
     * How many Bytes are in this Resource ?
     * 
     * @return Size of this Resource in Bytes
     */
    public long getSize()
    {
        // File
        if(null != f)
        {
            return f.length();
        }
        
        // File in ZipArchive
        if(null != zf)
        {
            if(null != ze)
            {
                return ze.getSize();
            }
        }
        
        // Input Stream 
        if(null != in)
        {
            try
            {
                return in.available();
            }
            catch ( IOException e )
            {                
            }
        }

        return 0;
    }
    
    /** get Data of this Resource
     * 
     * @return Input Stream of this Resource
     */
    public InputStream getStream()
    {
        // File
        if(null != f)
        {
            try
            {
                return new FileInputStream(f);
            } 
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
                return null;
            }
        }
        
        // File in ZipArchive
        if(null != zf)
        {
            if(null != ze)
            {
                try
                {
                    return zf.getInputStream(ze);
                } 
                catch (IOException e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        
        // InputStream
        if(null != in)
        {
            return in;
        }
        
        // Should never happen
        return null;
    }

    /**
     * Description of Resource for Directory Listing
     * 
     * @return Description of this Resource
     */
    public String getDescription()
    {
        if(null != f)
        {           
            String TimeDate = "";
            SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
            TimeDate = df.format(new Date(f.lastModified()));
            String FileName = f.getName();
            if(true == f.isDirectory())
            {
                FileName = FileName + "/";
            }
            if(true == (FileName.toLowerCase()).endsWith(".zip"))
            {
                FileName = FileName + "/";
            }
            return("   <tr>\n" +
                   "    <td><a href='" + FileName + "'>" + FileName + "</a></td> " +
                       "<td>" + TimeDate + "</td> " +
                       "<td align='right'>" + f.length() + "</td> " +
                       "<td></td>\n" +
                   "   </tr>\n");
        }
        
        
        if((null != zf) && (null != ze))
        {
            String TimeDate = "";
            SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
            TimeDate = df.format(new Date(ze.getTime()));
            
            
            String res =  ("   <tr>\n" +
                           "    <td><A HREF='" + ze.getName() + "'>" + ze.getName() + "</A></td> " +
                               "<td>" + TimeDate + "</td> " +
                               "<td align='right'>" + ze.getSize() + "</td>");
            if(null != ze.getComment())
            {
                res = res + " <td>" + ze.getComment() + "</td>";            
            }
            else
            {
                res = res + " <td></td>";
            }
            res = res + "\n" +
                           "   </tr>";
            return res;
        }
        
        return null;
    }
    
    /** get Position of this Resource in server tree
     * 
     * @return Server Location of this Resource
     */
    public String getLocation()
    {
        return Location;
    }

}
