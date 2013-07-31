import com.mongodb.*;
import java.net.UnknownHostException;

import java.util.Random;

public class mongoAccounting {
    // Settings, yes you can edit these before compiling
    private static String   hostname       = "mongos.eu.public";
    private static int      port           = 27017;
    private static String   dbName         = "avtest";
    private static String   collectionName = "acctBasic";
    private static String[] usernames      = {"alice",  "bob",     "carol",  "dave",    "eve",    "fred",    "gary",   "harry"   };
    private static String[] continents     = {"Europe", "America", "Europe", "America", "Europe", "America", "Europe", "America" };
    private static int      maxInserts     = 1000;
    // End of user serviceable parts

    // Utilities that we'll use
    private static Random rand = new Random();

    
    public static void main (String[] args) {
        MongoClient conn;
        DB db;
        DBCollection coll;

        try {
            conn = new MongoClient(hostname, port);
            db = conn.getDB(dbName);
            coll = db.getCollection(collectionName);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            // Give up if connection failed
            return;
        }
        for (int i = 0; i < maxInserts; i++) {
            insertAcct(coll);
        }
        conn.close();        
    }

    private static void insertAcct( DBCollection coll ) {
        // Create an accounting object to be inserted
        try {
            DBObject doc = new BasicDBObject();
            int n   = rand.nextInt( usernames.length );
            doc.put( "username",   usernames[n] );
            doc.put( "continent",  continents[n] );
            doc.put( "bytes",      getRandomBytes() );
            doc.put( "seconds",    getRandomSeconds() );
            coll.insert(doc);
        } catch (MongoException e) {
            e.printStackTrace();
        }        
    }

    private static int getRandomBytes() {
        return rand.nextInt(1024*1024);
    }
    
    private static int getRandomSeconds() {
        return rand.nextInt(30)+30;
    }

    //public static void logMe(String format, Object... args) {
    //    System.out.println(Thread.currentThread() + String.format(format, args));
    //}
}