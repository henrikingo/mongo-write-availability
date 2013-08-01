import com.mongodb.*;
import java.net.UnknownHostException;

import java.util.Random;


public class mongoAccounting {
    // Settings, yes you can edit these before compiling
    private static String   hostname       = "mongos.us.public";
    private static int      port           = 27017;
    private static String   dbName         = "avtest";
    private static String   collectionName = "acctBasic";
    //private static String[] usernames      = {"alice",  "bob",     "carol",  "dave",    "eve",    "fred",    "gary",   "harry"   };
    //private static String[] continents     = {"Europe", "America", "Europe", "America", "Europe", "America", "Europe", "America" };
    private static String[] usernames      = {"bob",     "dave",    "fred",    "harry"   };
    private static String[] continents     = {"America", "America", "America", "America" };
    private static int      maxInserts     = 1000;
    // End of user serviceable parts

    // Utilities that we'll use
    private static Random rand = new Random();

    
    public static void main (String[] args) {
        MongoClient conn;
        DB db;
        DBCollection coll;

        long startTimeTotal = System.nanoTime();
        long endTimeTotal;
        long durationTotal;

        try {
            conn = new MongoClient(hostname, port);
            db = conn.getDB(dbName);
            coll = db.getCollection(collectionName);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            // Give up if connection failed
            return;
        }

        long startTime = System.nanoTime();
        long endTime;
        long duration;
        for (int i = 0; i < maxInserts; i++) {
            DBObject doc = insertAcct(coll);
            endTime = System.nanoTime();
            duration = endTime - startTime;
            if( doc != null ) {
                System.out.printf("Duration:\t%d ms\t\tContent: %s %s %d %d\n", duration/1000000, doc.get("continent"), doc.get("username"), doc.get("bytes"), doc.get("seconds") );
            } else {
                System.out.printf("Duration:\t%d ms\t\tContent: null\n", duration/1000000 );
            }
            System.out.flush();
            startTime = System.nanoTime();
        }
        conn.close();        
        endTimeTotal = System.nanoTime();
        durationTotal = endTimeTotal - startTimeTotal;
        System.out.printf("Total Duration:\t%d sec\t\tInserts: %d\n", durationTotal/1000000000,  maxInserts);
    }

    private static DBObject insertAcct( DBCollection coll ) {
        // Create an accounting object to be inserted
        DBObject doc = null;
        try {
            doc = new BasicDBObject();
            int n   = rand.nextInt( usernames.length );
            doc.put( "username",   usernames[n] );
            doc.put( "continent",  continents[n] );
            doc.put( "bytes",      getRandomBytes() );
            doc.put( "seconds",    getRandomSeconds() );
            coll.insert(doc);
        } catch (MongoException e) {
            e.printStackTrace();
        }   
        return doc;     
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
