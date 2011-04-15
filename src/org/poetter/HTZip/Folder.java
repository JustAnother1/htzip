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
 * Folder.java
 */
package org.poetter.HTZip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 */
public class Folder
{
    private static final String HEADING = 
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n" +
        "<HTML>\n" +
        " <HEAD>\n" +
        "  <TITLE>Index</TITLE>\n" +
        " </HEAD>\n" +
        " <BODY>\n" +
        "  <H1>Index</H1>\n" +
        "  <table border='0'>\n" +
        "   <colgroup>\n" +
        "    <col>\n" +
        "    <col>\n" +
        "    <col align='right'>\n" +
        "    <col>\n" +
        "   </colgroup>\n" +
        "   <tr>\n" +
        "    <td>Name</td> <td>Last modified</td> <td align='right'>Size</td> <td>Description</td>\n" +
        "   </tr>\n";

    private static final String FOOTING = 
        "  </table><HR>\n" +
        " <ADDRESS>HTZip</ADDRESS>\n" + 
        " </BODY>\n" +
        "</HTML>";

    private ConcurrentHashMap<String, Resource> Entries;
    private String ServerRoot;
    private ConcurrentHashMap<String, ZipFile> ZipFiles;
    
    /**
     * 
     */
    public Folder(String PathToRoot)
    {
        Entries    = new ConcurrentHashMap<String, Resource>();
        ServerRoot = PathToRoot;
        ZipFiles   = new ConcurrentHashMap<String, ZipFile>();
    }

    /**
     * 
     * @param FileName
     * @return requested Resource
     */
    public Resource readFile(String FileName)
    {        
        Resource Result = null;
        
        FileName = ServerRoot + FileName;
        
        // Get Directory ?
        if(true == FileName.endsWith("/"))
        {            
            Result = getFile(FileName + "index.html");
            if(null == Result)
            {
                // Folder requested -> File Listing
                // create a Index.html
                Result = getListng(FileName);  
            }
            else
            {
                // Return index File  
            }          
        }
        else
        {
            // get File 
            Result = getFile(FileName);
        }
        return Result;               
    }
    
    /**
     * 
     * @param FileName
     * @param Res
     */
    private void addEntry(String FileName, Resource Res)
    {
        if((null != FileName) && (null != Res))
        {
            Entries.put(FileName, Res);
        }
    }
    
    /**
     * @param FileName Name of Requested File
     * @return InputStream of that File
     */
    private Resource getFile(String FileName)
    {
        Resource res = Entries.get(FileName);
        if(null == res)
        {
            // File not registered yet
            // Search for it
            File root = new File(FileName);
            if((true == root.exists()) && (true == root.canRead()))
            {
                // The File is a Folder
                if(true == root.isDirectory())
                {
                    // We do not deliver Directories as Files !                    
                }
                else
                {
                    // OK
                    res = new Resource(root, FileName);
                    addEntry(FileName, res);
                }
            }
            else
            {
                // The file does not exist !
                // So perhaps it it a File inside a ZIP File
                if(true == FileName.contains(".zip/"))
                {
                    // Yes we search inside a ZIP File
                    String ZipFilePath = FileName.substring(0, FileName.indexOf(".zip/") + 4);
                    ZipFile zf;
                    try
                    {
                        if(true == ZipFiles.containsKey(ZipFilePath))
                        {
                            // Found Zip File in Cache
                            zf = ZipFiles.get(ZipFilePath);
                        }
                        else
                        {                         
                            // Scanning new Zip File
                            try
                            {
                                zf = new ZipFile(ZipFilePath);
                            }
                            catch(ZipException e)
                            {
                                System.err.println("Zipfile can not be read !");
                                zf = null;
                            }
                            if(null != zf)
                            {
                                ZipFiles.put(ZipFilePath, zf);
                            }
                        }
                        if(null != zf)
                        {
                            String help = FileName.substring(ZipFilePath.length() + 1);
                            if(0 < help.length())
                            {
                                ZipEntry ze = zf.getEntry(help);
                                if(null != ze)
                                {
                                    res = new Resource(zf, ze, FileName);
                                    addEntry(FileName,  res);
                                    // Found new file in Zip
                                }
                                else
                                {
                                    // File not found in Zip File
                                }
                            }
                            else
                            {                                
                                // No Filename of File in Zip
                            }
                        }
                    } 
                    catch (IOException e1)
                    {
                        System.err.println("IOException ZipFile Search");            
                        e1.printStackTrace();
                        res = null;
                    }              
                }
                else
                {
                    // Not in ZIP File -> File Not Found                    
                }
            }
        }
        else
        {
            // Found File in Cache
        }
        return res;
    }

    /**
     * 
     * @param Location
     * @return requested Resource
     */
    private Resource getListng(String FileName)
    {
        Resource res = Entries.get(FileName);
        if(null == res)
        {
            if(true == FileName.contains(".zip/"))
            {
                ZipFile zf = null;
                String ZipFilePath = FileName.substring(0, FileName.indexOf(".zip/") + 4);
                String help = null;
                if(FileName.length() > (ZipFilePath.length() + 1))
                {
                    help = FileName.substring(ZipFilePath.length() + 1);
                }

                if(true == ZipFiles.containsKey(ZipFilePath))
                {
                    // Found Zip File in Cache
                    zf = ZipFiles.get(ZipFilePath);
                }
                else
                {                         
                    // Scanning new Zip File
                    try
                    {
                        zf = new ZipFile(ZipFilePath);
                    }
                    catch(ZipException e)
                    {
                        System.err.println("ZipException : Zipfile can not be read !");
                        zf = null;
                    }
                    catch (IOException e)
                    {
                        System.err.println("IOException : Zipfile can not be read !");
                        zf = null;
                    }
                    if(null != zf)
                    {
                        ZipFiles.put(ZipFilePath, zf);
                    }
                }
                if(null != zf)
                {
                    // Create Listing
                    StringBuffer sb = new StringBuffer();
                    Vector<String> folders = new Vector<String>();
                    Enumeration< ? extends ZipEntry> e = zf.entries();
                    while(e.hasMoreElements())
                    {
                        ZipEntry curKey = e.nextElement();
                        if(null == help)
                        {
                            String curName = ZipFilePath + "/" + curKey.getName();
                            Resource r = new Resource(zf, curKey, curName);                                    
                            sb.append(r.getDescription());
                            addEntry(curName, r);                            
                        }
                        else
                        {
                            if(true == (curKey.getName()).startsWith(help))
                            {
                                // Entry is in the 
                                String rem = (curKey.getName()).substring(help.length());
                                if(false == rem.contains("/"))
                                {
                                    String curName = ZipFilePath + "/" + curKey.getName();
                                    Resource r = new Resource(zf, curKey, curName);                                    
                                    sb.append(r.getDescription());
                                    addEntry(curName, r);
                                }
                                else
                                {
                                    // Sub Directories
                                    rem = rem.substring(0, rem.indexOf('/'));
                                    if( false == folders.contains(rem))
                                    {
                                        folders.add(rem);
                                    }
                                }
                            }
                        }
                    }
                    if(false == folders.isEmpty())
                    {
                        StringBuffer fsb = new StringBuffer();
                        for(int i = 0; i < folders.size(); i++)
                        {
                            fsb.append("   <tr>\n" +
                                       "    <td><a href='" + folders.elementAt(i) + "'>" + folders.elementAt(i) + "</a></td> " +
                                       "<td> </td> " +
                                       "<td align='right'> </td> " +
                                       "<td></td>\n" +
                                       "   </tr>\n");                            
                        }
                        fsb.append(sb.toString());
                        sb = fsb;
                    }
                    byte[] b = (HEADING + sb.toString() + FOOTING).getBytes();
                    ByteArrayInputStream ba = new ByteArrayInputStream(b); 
                    res = new Resource(ba, FileName);
                    addEntry(FileName, res);  
                }
                else
                {
                    // Zipfile can not be found - Path is invalid
                }
            }
            else
            {
                // Normal Folder
                File root = new File(FileName);
                if((true == root.exists()) && (true == root.canRead()))
                {
                    // The File is a Folder
                    if(true == root.isDirectory())
                    {
                        // Create Listing
                        StringBuffer sb = new StringBuffer();
                        File[] far = root.listFiles();
                        for(int i = 0; i < far.length; i++)
                        {                            
                            String curName = FileName + far[i].getName();
                            Resource r = new Resource(far[i], curName);
                            sb.append(r.getDescription());
                            addEntry(curName, res);                            
                        }
                        byte[] b = (HEADING + sb.toString() + FOOTING).getBytes();
                        ByteArrayInputStream ba = new ByteArrayInputStream(b); 
                        res = new Resource(ba, FileName);
                        addEntry(FileName, res);        
                    }
                    else
                    {
                        // We do not deliver Files as Directories!   
                    }
                }
            }
        }
        return res;
    }

    /**
     * @param res
     */
    public void removeRessource(Resource res)
    {
        String Key = res.getLocation();
        Entries.remove(Key);
    }

}
