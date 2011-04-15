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
 * Main Class of HTZIP 
 * HTZIP is a extremely simple Web Server that serves Files and 
 * Files from within ZIP Archives
 */
package org.poetter.HTZip;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public class HTZip extends Thread
{
    private Folder root;
    private int Port;
    private InetAddress Address;
    private int backlog;
    
    private final static int  CORE_POOL_SIZE   = 5;
    private final static int  MAX_POOL_SIZE    = 500;
    private final static long KEEP_ALIVE_NANOS = 100;
    
	/**
	 * 
	 */
	public HTZip(String Folder, InetAddress Address, int Port, int backlog) 
	{
        // create HTTP Root 
	    System.out.println( "Starting HTZip on " + Address + ":" + Port + " with Folder (" 
	              + Folder + ") and backlog of " + backlog + " !");
        this.root = new Folder(Folder);
        this.Port    = Port;
        this.Address = Address;
        this.backlog = backlog;
	}

    
    
    /**
     * 
     * @see java.lang.Thread#run()
     */
    public void run()
    {
        // Start Thread Pool
        ThreadPoolExecutor  tpe = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_NANOS,
                java.util.concurrent.TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
                );
        
        // Start Server
        try
        {
            ServerSocket proxySocket = new ServerSocket(Port, backlog, Address);            
            while(false == interrupted())
            {
                try
                {
                    final Socket client = proxySocket.accept();
                    System.out.println( "Accepted Connection from " + client.getRemoteSocketAddress() );
                    ConnectionThread ct = new ConnectionThread(root, client); 
                    tpe.execute(ct);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    System.err.println("Accept failed");
                }
            }
        } 
        catch (BindException e)
        {
            System.err.println("Can not Bind on " + Address + ":" + Port + " !");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }      
        
    }
    

    /**
     * @param args
     */
    public static void main(String[] args) 
    {
        
        if(1 != args.length)
        {
            // Parameter is missing !
            System.out.println("Usage : HTZip Config.xml");
            System.exit(1);
        }
        
        File configfile = new File(args[0]);
        
        if(    (true == configfile.exists()) 
            && (true == configfile.canRead()) 
            && (true == configfile.isFile())  )
        {        
            // Parse configuration
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(configfile);
                
                NodeList nl = doc.getElementsByTagName("HTZipConfig");
                
                if(null == nl)
                {
                    System.out.println("ERROR : Could not find <HTZipConfig> Tag !");
                    System.exit(1);
                }
                    
                Node n = nl.item(0);
                
                if(null == n)
                {
                    System.out.println("ERROR : Could not get <HTZipConfig> Tag");
                    System.exit(1);
                }
                
                if(false == n.hasChildNodes())
                {
                    System.out.println("ERROR : Could not find Configuration Tags !");
                    System.exit(1);
                }
                    
                String Host = "127.0.0.1";
                String RootFolder = ".";
                String Port = "";
                
                NodeList params = n.getChildNodes();
                for(int i = 0; i < params.getLength(); i++)
                {
                    Node pa = params.item(i);
                    
                    if(true == (pa.getNodeName()).equals("IP"))
                    {
                        Host = pa.getTextContent();
                    }
                    
                    if(true == (pa.getNodeName()).equals("Folder"))
                    {
                        RootFolder = pa.getTextContent();
                    }
                    
                    if(true == (pa.getNodeName()).equals("Port"))
                    {
                        Port = pa.getTextContent();
                    }
                    
                }
                
                int port = (new Integer(Port)).intValue();                
                
                // Start ServerSocket
                HTZip server;
                server = new HTZip(RootFolder, InetAddress.getByName(Host), port, 0);
                server.start();
            } 
            catch(NumberFormatException e)
            {
                System.err.println("ERROR : Port is not a valid Number !");
            }
            catch (ParserConfigurationException e1)
            {
                System.err.println("ERROR : " + args[0] + " : Parser Configuration Exception");
                e1.printStackTrace();
            } 
            catch (SAXException e)
            {
                System.err.println("ERROR : " + args[0] + " : SAX Exception");
                e.printStackTrace();
            }
            catch (UnknownHostException e)
            {
                System.err.println("ERROR : " + args[0] + " : Unknown Host Exception");
                e.printStackTrace();
            }
            catch (IOException e)
            {
                System.err.println("ERROR : " + args[0] + " : IO Exception");
                e.printStackTrace();
            }
            
        }
        else
        {
            // Config File not readable !
            System.out.println("ERROR : Could not read " + args[0] + " !");
        }
        
    }
}
