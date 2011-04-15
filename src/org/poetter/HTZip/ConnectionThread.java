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
 * ConnectionThread.java
 */
package org.poetter.HTZip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;


/** A Connection to a browser
 * 
 */
public class ConnectionThread implements Runnable
{
    private Folder root;
    private Socket Connection;
    
    /** 
     * 
     */
    public ConnectionThread(Folder root, Socket Connection)
    {
        this.Connection = Connection;
        this.root = root;
    }
    
    /** Resource was not found
     * 
     * @param Ressource
     */
    private void send404for(String Resource)
    {
        // Resource does not exist send 404
        String Body = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
        "<html><head>\r\n" + 
        "<title>404 Not Found</title>\r\n" + 
        "</head><body>\r\n" + 
        "<h1>Not Found</h1>\r\n" + 
        "<p>The requested URL " + Resource + " was not found on this server.</p>\r\n" + 
        "<hr>\r\n" +
        "<address>HTZip</address>\r\n" + 
        "</body></html>";
        
        String StatusLine = "HTTP/1.1 404 File not Found\r\n";
        
        String ResponseHeader = "Content-Type: text/html; charset=UTF-8\r\n" +
        "Content-Length: " + Body.length() + "\r\n";
                          
        OutputStream out;
        try
        {                
            out = Connection.getOutputStream();
            out.write((StatusLine).getBytes());
            out.write((ResponseHeader).getBytes());
            out.write(("\r\n").getBytes());
            out.write((Body).getBytes());
            out.flush();
        } 
        catch (IOException e)
        {
            System.err.println("IOException !");
        }         
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        // Init
        try
        {
            Connection.setKeepAlive(true);
            Connection.setSoLinger( true, 30 );
        }
        catch (SocketException e)
        {
            System.err.println("Linger Exception");
        }        
        
        String    RequestedRessource = null;
        Resource res                = null;
        
        boolean isActive = true;
        do
        {       
            RequestedRessource = parseRequest();
            if(null != RequestedRessource)
            {
                res = null;
                if(0 < RequestedRessource.length())
                {
                    res = root.readFile(RequestedRessource);
                }
                
                // Check if requested Location exists
                InputStream ips = null;
                if(null != res)
                {
                    ips = res.getStream();
                    if(null != ips)
                    {
                        sendRessource(ips, res.getSize());
                    }
                    else
                    { 
                        root.removeRessource(res);
                        send404for(RequestedRessource);           
                    }
                }
                else
                {
                    send404for(RequestedRessource);     
                }
            }
            else
            {
                isActive = false;
            }
        } while(true == isActive);
        
        try
        {
            Connection.close();
        }
        catch ( IOException e )
        {
            System.err.println("Connection close Exception");
        }
    }
    
    private String parseRequest()
    {
        String RequestedRessource = null;
        String line;
        try
        {            
            InputStream in = Connection.getInputStream();            
            // Read a line from in 
            int i = -1;                
            StringBuffer sb = new StringBuffer();
            do{
                i = in.read();
                if(-1 != i)
                {
                    char c = (char)i;
                    if((c == '\n') || (c == '\r'))
                    {
                        // found end of Line
                        if(0 < sb.length())
                        {
                            line = sb.toString();
                            sb = sb.delete(0, sb.length());                            
                            if(true == line.startsWith("GET "))
                            {
                                // Found Get !
                                RequestedRessource = line.substring(4, (line.lastIndexOf("HTTP")-1));
                                break;
                            }
                        } 
                        else
                        {
                            // Empty Line
                        }
                    }
                    else
                    {
                        sb.append(c);
                    }
                }
            }while(i != -1);
        } 
        catch (IOException e1)
        {
            try
            {
                Connection.close();
            }
            catch ( IOException e )
            {
            }
        }
        return RequestedRessource;
    }
    
    private void sendRessource(InputStream res, long size)
    {
        try
        {
            String StatusLine = "HTTP/1.1 200 OK\r\n";
            String ResponseHeader = "Content-Type: text/html\r\n" +
            		                "charset=UTF-8\r\n" +
                                    "Content-Length: " + size + "\r\n";
            
            OutputStream out;
            try
            {              
                // Send HTTP Header
                out = Connection.getOutputStream();                    
                out.write(StatusLine.getBytes());
                out.write(ResponseHeader.getBytes());
                out.write("\r\n".getBytes());
                // Send the File
                byte[] buffer = new byte[4096];
                int i;
                do
                {
                    i = res.read(buffer);
                    if(0 < i)
                    {
                        out.write(buffer, 0, i);
                    }
                }while(-1 != i);
                res.close();
                out.flush();
            } 
            catch(SocketException se)
            {
                System.err.println("Socket Exception !");
            }
            catch (IOException io)
            {
                System.err.println("IOException !");
            }
        } 
        catch(Exception e)
        {
            System.err.println("Exception !");
        }
    }

}
