/*Authors: Advaith Auron Suresh and Praveen Ganapathy*/
/*Source File: DistanceVector.java*/

/*Imports*/
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class DistanceVector {
	
	static int TotalNoOfAdjNodes; // to store number of adjacent nodes
	static boolean flag = true;
	static int number = 0; //Total number of rounds
	private static long OriginalTimeStamp; //To keep track of file modification
	private static File file;
    static ArrayList<String> adjacentNodes;// List of adjacent nodes
	static HashMap<String,Double> adjacentCosts;// Adjacent node costs obtained from input file
        static HashMap<String,Integer> nodePortNos;// To keep track of port numbers
	static HashMap<String,Double> LowestCost;// Storing smallest cost for each node
	static HashMap<String,String> nextHops;// Keep track of next hop
        static ArrayList<String> AllNodes;// Stores all the nodes in the network
	
        /*Function to check if the file has been modified*/
        private static boolean FileModified() 
        {
            
            long ModifiedTimeStamp = file.lastModified();
            /*File is updated*/
            if( ModifiedTimeStamp != OriginalTimeStamp ) 
            {
              OriginalTimeStamp = ModifiedTimeStamp;
              return true;
            }
            //No, file is not updated
            return false;
	}
        
	public static void main(String[] args) 
        {
		String nodeName;
		DatagramSocket nodeSocket;
		adjacentNodes=new ArrayList<>();
		LowestCost = new HashMap<>();
		adjacentCosts = new HashMap<>();
		nextHops =new HashMap<>();
		AllNodes= new ArrayList<>();
		nodePortNos = new HashMap<>();
                /*Checks number of input parameters*/
                if(args.length==3)
		{
                    try
                    {
                        InetAddress serverName = InetAddress.getByName("localhost");
                        int portNo = Integer.parseInt(args[0]);
                        String path = args[1];
                        nodeSocket=new DatagramSocket(portNo);
                        nodeName = args[2];
                        FileInputStream infile=new FileInputStream(path);
                        file=new File(path);
                        OriginalTimeStamp=file.lastModified();
                        AllNodes.add(nodeName);
                        String portDetails = nodeName + " "+portNo+"\n";
                        Path filePath = Paths.get("PortNumber.txt");
                        /*Create an temporary file for keeping track of port Nos*/
                        if (!Files.exists(filePath)) 
                        {
                            Files.createFile(filePath);
                        }
                        Files.write(filePath, portDetails.getBytes(), StandardOpenOption.APPEND);
                        BufferedReader bufRead = new BufferedReader(new InputStreamReader(infile));
                        String line;
                        // Getting number of Adjacent nodes
                        if((line =bufRead.readLine() )!= null)
                        {
                                TotalNoOfAdjNodes = Integer.parseInt(line);
                        }
                        /* Obtain cost of adjacent nodes from current node from input file*/
                        for (int iterator=0;iterator<TotalNoOfAdjNodes;iterator++)
                        {
                            if((line=bufRead.readLine())!=null)
                            {
                                String[] nodeInfo =line.split(" ");
                                String node = nodeInfo[0];
                                double cost = Double.parseDouble(nodeInfo[1]);
                                /*Appending adjacent nodes list*/
                                adjacentNodes.add(node);
                                /*Adding adjacent nodes and cost*/
                                adjacentCosts.put(node,cost);
                            }
                        }

                        /* Update linkcosts and next hops for every node */
                        for (int iterator=0;iterator<AllNodes.size();iterator++)
                        {
                                if(adjacentCosts.containsKey(AllNodes.get(iterator)))
                                {
                                    LowestCost.put(AllNodes.get(iterator),adjacentCosts.get(AllNodes.get(iterator)));
                                    nextHops.put(AllNodes.get(iterator),AllNodes.get(iterator));
                                }
                                else if(!AllNodes.get(iterator).equalsIgnoreCase(nodeName))
                                {
                                    LowestCost.put(AllNodes.get(iterator),Double.MAX_VALUE);
                                    nextHops.put(AllNodes.get(iterator), "Undiscovered Node");
                                }
                                else if(AllNodes.get(iterator).equalsIgnoreCase(nodeName))
                                {
                                    LowestCost.put(nodeName, 0.0);
                                    nextHops.put(AllNodes.get(iterator), "Source");
                                }
                        }

                        byte[] sendData;
                        byte[] receiveData = new byte[1024];
                        if(Files.exists(filePath))
                        {
                            FileInputStream fis=new FileInputStream(filePath.toString());
                            BufferedReader buffreader = new BufferedReader(new InputStreamReader(fis));
                            String Inline;
                            while((Inline = buffreader.readLine() )!=null)
                            {
                                String[] nodesPortDetails = Inline.split(" ");
                                String otherNodeName = nodesPortDetails[0];
                                nodePortNos.put(otherNodeName, Integer.parseInt(nodesPortDetails[1]));
                                if(!AllNodes.contains(otherNodeName))
                                {
                                        AllNodes.add(otherNodeName);
                                }
                            }

                        }
                        System.out.println();
                        System.out.println("%----------------------------------------------------------------------------------------------------%");
                        System.out.println();
                        System.out.println("> output number "+number);
                        LowestCost.keySet().forEach((n) -> {
                            System.out.println("shortest path "+nodeName+"-"+n+": the next hop is "+nextHops.get(n)+" and the cost is "+LowestCost.get(n));
                        });
                        System.out.println();
                        System.out.println("%----------------------------------------------------------------------------------------------------%");
                        System.out.println();
                        for(int iterator=0;iterator<adjacentNodes.size();iterator++)
                        {
                            String neighbourNode = adjacentNodes.get(iterator);
                            int neighbourPort = 0;
                            if(nodePortNos.get(neighbourNode)!=null)
                                    neighbourPort = nodePortNos.get(neighbourNode);
                            /*Send link costs if the port number is available and valid*/
                            if(neighbourPort>0)
                            {
                                number = 1;
                                sendData = LowestCost.toString().getBytes();
                                DatagramPacket packet = new DatagramPacket(sendData,sendData.length,serverName,neighbourPort);
                                nodeSocket.send(packet);
                            }
                        }
                        /*Define a timer to trigger an event every 15 seconds*/
                        javax.swing.Timer time =new javax.swing.Timer(15000, (ActionEvent evt) -> {
                            /* Continually read nodes and port numbers from input file */
                            if(Files.exists(filePath))
                            {
                                FileInputStream fileIn = null;
                                try 
                                {
                                    fileIn = new FileInputStream(filePath.toString());
                                } 
                                catch (FileNotFoundException e) {}
                                BufferedReader buffer = new BufferedReader(new InputStreamReader(fileIn));
                                String lineContents;
                                try 
                                {
                                    while((lineContents = buffer.readLine() )!=null)
                                    {
                                        String[] nodesPortDetails = lineContents.split(" ");
                                        String otherNodeName = nodesPortDetails[0];
                                        nodePortNos.put(otherNodeName, Integer.parseInt(nodesPortDetails[1]));
                                        if(!AllNodes.contains(otherNodeName))
                                        {
                                            AllNodes.add(otherNodeName);
                                        }
                                    }
                                } 
                                catch (NumberFormatException | IOException e) {}
                            }


                            /* Update lowest cost and next hops after each trigger */
                            for (int iterator=0;iterator<AllNodes.size();iterator++)
                            {
                                if(adjacentCosts.containsKey(AllNodes.get(iterator))&&!LowestCost.containsKey(AllNodes.get(iterator))&&!nextHops.containsKey(AllNodes.get(iterator)))
                                {
                                    LowestCost.put(AllNodes.get(iterator),adjacentCosts.get(AllNodes.get(iterator)));
                                    nextHops.put(AllNodes.get(iterator),AllNodes.get(iterator));
                                }
                                else if(!AllNodes.get(iterator).equalsIgnoreCase(nodeName)&&!LowestCost.containsKey(AllNodes.get(iterator))&&!nextHops.containsKey(AllNodes.get(iterator)))
                                {
                                    LowestCost.put(AllNodes.get(iterator),Double.MAX_VALUE);
                                    nextHops.put(AllNodes.get(iterator), "Undiscovered Node");
                                }
                                else if(AllNodes.get(iterator).equalsIgnoreCase(nodeName))
                                {
                                    LowestCost.put(nodeName, 0.0);
                                    nextHops.put(AllNodes.get(iterator), "Source");
                                }
                            }
                            System.out.println();
                            System.out.println("%----------------------------------------------------------------------------------------------------%");
                            System.out.println();
                            System.out.println("> output number "+number);
                            LowestCost.keySet().forEach((n) -> {
                                System.out.println("shortest path "+nodeName+"-"+n+": the next hop is "+nextHops.get(n)+" and the cost is "+LowestCost.get(n));
                           });
                            System.out.println();
                            System.out.println("%----------------------------------------------------------------------------------------------------%");
                            System.out.println();
                            number++;
                            for(int iterator=0;iterator<adjacentNodes.size();iterator++)
                            {
                                String neighbourNode = adjacentNodes.get(iterator);
                                int neighbourPort = 0;
                                if(nodePortNos.get(neighbourNode)!=null)
                                    neighbourPort = nodePortNos.get(neighbourNode);
                                byte[] senddata;
                                if(neighbourPort > 0)
                                {
                                    senddata = LowestCost.toString().getBytes();
                                    DatagramPacket packet = new DatagramPacket(senddata,senddata.length,serverName,neighbourPort);
                                    try 
                                    {
                                        nodeSocket.send(packet);
                                    } 
                                    catch (IOException e) {}
                                }
                            }
                            /*Restart timer at the end of the event*/
                            ((javax.swing.Timer)evt.getSource()).restart();
                        });
                        /*Start timer*/
                        time.start();
                        /*Continually loops to receive data from other ports*/
                        while(true)
                        {
                            if(flag == true)
                            {
                                flag = false;
                                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                                nodeSocket.receive(receivedPacket);
                                String message = new String(receivedPacket.getData());
                                String src="";
                                /*Regex used to split received data*/
                                Pattern p = Pattern.compile("[\\=\\, ]++");
                                String[] split = p.split(message);
                                HashMap<String,Double >receivedMap = new HashMap<>();
                                for ( int iterator=0; (iterator+1)< split.length; iterator+=2 )
                                {
                                    String key;
                                    double value;
                                    if(split[iterator].contains("{"))
                                    {
                                        key = split[iterator].substring(1);
                                    }
                                    else
                                    {
                                        key = split[iterator];

                                    }
                                    if(split[iterator+1].contains("}"))
                                    {
                                        value = Double.parseDouble(split[iterator+1].split("}")[0]);
                                        if(value==0.0)
                                        {
                                                src = key;
                                        }
                                        receivedMap.put(key, value);
                                        break;
                                    }
                                    else
                                    {
                                        value = Double.parseDouble(split[iterator+1]);
                                        if(value==0.0)
                                        {
                                                src = key;
                                        }

                                        receivedMap.put(key, value);
                                    }
                                }

                                /*Reparse modified file*/
                                if(FileModified())
                                {
                                    FileInputStream fstr=new FileInputStream(path);
                                    BufferedReader buff = new BufferedReader(new InputStreamReader(fstr));
                                    String reLine;
                                    // Getting number of Adjacent nodes
                                    if((reLine = buff.readLine())!= null)
                                    {
                                            TotalNoOfAdjNodes = Integer.parseInt(reLine);
                                    }
                                    for (int iterator=0;iterator<TotalNoOfAdjNodes;iterator++)
                                    {
                                        if((reLine=buff.readLine())!=null)
                                        {
                                            String[] nodeInfo =reLine.split(" ");
                                            String node = nodeInfo[0];
                                            double cost = Double.parseDouble(nodeInfo[1]);
                                            adjacentNodes.add(node);
                                            if(cost != adjacentCosts.get(node))
                                            {
                                                adjacentCosts.put(node, cost);
                                                System.out.println();
                                                nextHops.keySet().stream().filter((nextnode) -> (node.equalsIgnoreCase(nextHops.get(nextnode)))).map((nextnode) -> {
                                                    System.out.println();
                                                return nextnode;
                                                }).forEachOrdered((nextnode) -> {
                                                    LowestCost.put(nextnode,Double.MAX_VALUE);
                                                });
                                            }
                                        }
                                    }
                                }
                                /* Update costs */
                                if(AllNodes.contains(src))
                                {
                                    for (String nodename : LowestCost.keySet()) 
                                    {
                                        if(LowestCost.containsKey(nodename)&& adjacentCosts.containsKey(src)&&nextHops.containsKey(nodename)&& receivedMap.containsKey(nodename))
                                        {
                                            if(!nodename.equalsIgnoreCase(nodeName))
                                            {
                                                if(nextHops.get(nodename).equals(src))
                                                {
                                                    LowestCost.put(nodename,(adjacentCosts.get(src)+receivedMap.get(nodename)));
                                                }
                                                if(LowestCost.get(nodename)>(adjacentCosts.get(src)+receivedMap.get(nodename)))
                                                {
                                                    LowestCost.put(nodename, (adjacentCosts.get(src)+receivedMap.get(nodename)));
                                                    nextHops.put(nodename, src);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            flag = true;
                        }
                    }
                    catch(NumberFormatException ex)
                    {
                        System.out.println("Parsing Number "+ex);
                    } catch (SocketException e) 
                    {
                        System.out.println("Socket Creation Error "+e);	
                    } catch (FileNotFoundException e) 
                    {
                        System.out.println("File Not Found "+e);
                    } 
                    catch (IOException e) 
                    {
                        System.out.println("IO Exception "+e);
                    }
                    finally
                    {
                        Path filePath = Paths.get("PortNumber.txt");
                        if (Files.exists(filePath)) 
                        {
                            try 
                            {
                                Files.delete(filePath);
                            } 
                            catch (IOException e) {}
                        }
                    }
                }
	}
	

}
