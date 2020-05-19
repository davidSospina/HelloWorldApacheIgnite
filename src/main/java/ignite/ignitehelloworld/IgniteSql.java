/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ignite.ignitehelloworld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

/**
 *
 * @author Otro
 */
public class IgniteSql {
    
    public static void main(String[] args) throws IgniteException {
        // Preparing IgniteConfiguration using Java APIs
        IgniteConfiguration cfg = new IgniteConfiguration();

        // The node will be started as a client node.
        cfg.setClientMode(true);

        // Classes of custom Java logic will be transferred over the wire from this app.
        cfg.setPeerClassLoadingEnabled(true);
        
        // Node`s Ips
        Collection<String> ips = new ArrayList<>();
        ips.add("127.0.0.1");
        //ips.add("192.168.1.75");

        // Setting up an IP Finder to ensure the client can locate the servers.
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(ips);
        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(ipFinder));

        // Starting the node
        Ignite ignite = Ignition.start(cfg);

        // Create an IgniteCache and put some values in it.
        IgniteCache<Integer, String> cache = ignite.getOrCreateCache("myCache");
        // Create table based on PARTITIONED template with one backup.
        cache.query(new SqlFieldsQuery(
                "CREATE TABLE persona ( id INT PRIMARY KEY, nombre VARCHAR(20));")).getAll();
        
        /// Almacenando valores
        SqlFieldsQuery qry = new SqlFieldsQuery(
            "insert into persona (id, nombre) values (?, ?)");

        cache.query(qry.setArgs(1,"Andres"));
        cache.query(qry.setArgs(2,"David"));
//        cache.query(qry.setArgs(3,"Nombre3"));
//        cache.query(qry.setArgs(4,"Nombre4"));
        
        System.out.println(">> Created the cache and values.");

        // Executing custom Java compute task on server nodes.
        ignite.compute(ignite.cluster().forServers()).broadcast((IgniteRunnable) new RemoteTask());

        System.out.println(">> Compute task is executed, check for output on the server nodes.");

        System.out.println(">> Obteniendo valores nuevamente");
        System.out.println(cache.query(new SqlFieldsQuery(
         "SELECT * FROM persona;")).getAll());


        // Disconnect from the cluster.
        ignite.close();
    }

    /**
     * A compute tasks that prints out a node ID and some details about its OS
     * and JRE. Plus, the code shows how to access data stored in a cache from
     * the compute task.
     */
    private static class RemoteTask implements IgniteRunnable {

        @IgniteInstanceResource
        Ignite ignite;

        @Override
        public void run() {
            System.out.println(">> Executing the compute task");

            System.out.println(
                    "   Node ID: " + ignite.cluster().localNode().id() + "\n"
                    + "   OS: " + System.getProperty("os.name")
                    + "   JRE: " + System.getProperty("java.runtime.name"));

            IgniteCache<Integer, String> cache = ignite.cache("myCache");

            System.out.println(">> Hello world SQL");
        }
    }

    
    
}
