import com.mongodb.*;

/**
 * A wrapper around DBCollection that will retry failed writes to another shard.
 *
 * The purpose of this class is to achieve higher write availability in a
 * sharded MongoDB cluster by way of eventually consistent writes. 
 *
 * 2 fields are added to each document before passing the write to
 * DBCollection for actual writing: _shardAffinity and _fallback.
 *
 * _shardAffinity is an integer calculated by hashing a field in the shard 
 * key. This key should be provided by the user, and is called "shard 
 * affinity key". The default shard affinity key is _id. The purpose of 
 * _shardAffinity is to designate a "default" 
 * shard for each document written. It is constant for same values of the
 * specified "shard affinity key" and during normal operation of the cluster
 * causes all writes for that key to hit the same shard.
 *
 * _fallback is an integer that starts at 0. If a write operation fails,
 * it is incremented by one and the write operation is retried. The DBA
 * should configure MongoDB tag ranges such that this retry will hit another
 * shard than the one that just failed. (Example below.)
 *
 * To specify how many fallback writes can be attempted, call
 * setFallbackShards().
 *
 * <h2>Example:</h2>
 * 
 * Assume we are storing the following documents:
 *
 * { username : "hingo", firstname : "Henrik", lastname : "Ingo" }
 *
 * And we would shard it by username:
 *
 * sh.shardCollection( "somedb.somecollection", { username : 1 } )
 *
 * We want to set shard affinity from the username key. Assume also that our 
 * cluster has 3 shards. We would now use FallbackShardsCollection like this:
 *
 * MongoClient conn = new MongoClient(hostname, port);
 * DB db = conn.getDB(dbName);
 * DBCollection realCollection = db.getCollection(collectionName);
 * FallbackShardsCollection coll = new FallbackShardsCollection( realCollection );
 * coll.setShardAffinityKey( "username" );
 * coll.setFallbackShards( 2 );
 * coll.insert( doc ); // doc = { username : "hingo", firstname : "Henrik", lastname : "Ingo" }
 *
 * Since we are using FallbackShardsCollection, we will in fact be storing the 
 * following document into MongoDB:
 *
 * { username : "hingo", firstname : "Henrik", lastname : "Ingo", _shardAffinity : 0, _fallback : 0 }
 *
 * ...where _fallback is incremented each time a write operation failed: 0, 1, 2.
 *
 * It is the DBA's responsibility to configure shard key, tags and ranges to 
 * achieve the desired effect. In this case, we would need to do the following:
 *
 * sh.shardCollection( "somedb.somecollection", { _shardAffinity : 1, _fallback : 1, username : 1 } )
 * sh.addShardTag( "shard1", "shard1" );
 * sh.addShardTag( "shard2", "shard2" );
 * sh.addShardTag( "shard3", "shard3" );
 *
 * // shard1 plus 2 and 3 for fallbacks
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 0, _fallback : 0 },
 *                 { _shardAffinity : 0, _fallback : 1 },
 *                 "shard1" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 0, _fallback : 1 },
 *                 { _shardAffinity : 0, _fallback : 2 },
 *                 "shard2" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 0, _fallback : 2 },
 *                 { _shardAffinity : 0, _fallback : 3 },
 *                 "shard3" )
 *
 * // shard2 plus 3 and 1 for fallbacks
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 1, _fallback : 0 },
 *                 { _shardAffinity : 1, _fallback : 1 },
 *                 "shard2" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 1, _fallback : 1 },
 *                 { _shardAffinity : 1, _fallback : 2 },
 *                 "shard3" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 1, _fallback : 2 },
 *                 { _shardAffinity : 1, _fallback : 3 },
 *                 "shard1" )
 *
 * // shard3 plus 1 and 2 for fallbacks
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 2, _fallback : 0 },
 *                 { _shardAffinity : 2, _fallback : 1 },
 *                 "shard3" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 2, _fallback : 1 },
 *                 { _shardAffinity : 2, _fallback : 2 },
 *                 "shard1" )
 * sh.addTagRange( "somedb.somecollection", 
 *                 { _shardAffinity : 2, _fallback : 2 },
 *                 { _shardAffinity : 2, _fallback : 3 },
 *                 "shard2" )
 *
 * @todo: Currently only inserts are supported. Also support upserts. (Deletes cannot be supported in a eventually consistent way.)
 * @todo: Currently the "shard affinity key" provided must be a String, and also its values must be all Strings.
 * @todo: when more official, should live in a com.mongodb.* namespace
 * @todo: add some copyright license
 */


public class FallbackShardsCollection {

    private DBCollection _collection;
    private int          _fallbackShards = 1; 
    private String       _shardAffinityKey = "_id";
    
    public FallbackShardsCollection(){
    }
    
    public FallbackShardsCollection( DBCollection collection ) {
        this._collection = collection;
    }

    public FallbackShardsCollection( DBCollection collection, String shardAffinityKey ) {
        this._collection = collection;
        this._shardAffinityKey = shardAffinityKey;
    }
    
    public FallbackShardsCollection( DBCollection collection, String shardAffinityKey, int fallbackShards ) {
        this._collection = collection;
        this._shardAffinityKey = shardAffinityKey;
        this._fallbackShards = fallbackShards;
    }
        
    public void setCollection( DBCollection collection ) {
        this._collection = collection;
    }
    
    public DBCollection getCollection() {
        return this._collection;
    }

    public void setFallbackShards( int fallbackShards ){
        this._fallbackShards = fallbackShards;
    }

    public int getFallbackShards(){
        return this._fallbackShards;
    }
    
    public void setShardAffinityKey( String keyName ){
        this._shardAffinityKey = keyName;
    }
    
    public String getShardAffinityKey(){
        return this._shardAffinityKey;
    }
    
    public WriteResult insert ( DBObject doc ) 
                              throws MongoException {
        // Use _shardAffinityKey and _fallbackShards to add "routing" information to doc
        doc.put( "_shardAffinity", getShardAffinity( doc ) );
        doc.put( "_fallback", 0 );
        for( int i=0; i <= _fallbackShards; i++ ) {
           try {
              // if this succeeds, just return
              return _collection.insert(doc);
           } catch (MongoException e) {
               // if not, then we silently retry on the next fallback shard
               doc.put( "_fallback", i+1 );
               // and if there are no more shards to try, we give up and throw the exception to caller
               if ( i+1 > _fallbackShards ) {
                   throw e;
               }
           }
        }
        // This should never happen, compiler requires it
        System.out.println( "FallbackShardsCollection.insert() Error: THIS SHOULD NEVER HAPPEN!" );
        return null;
    }    

    /**
     * Compute the value of _shardAffinity, using the value previously specified shardAffinityKey in the DBObject doc given as a parameter.
     *
     * @note: This is used internally, but there is no harm in exposing it as a public method.
     */
    public int getShardAffinity( DBObject doc ){
        // This is just a simple hash:
        // Take last character and divide by number of shards available to rotate on.
        // Note that total number of shards available is one greater than "fallback" shards.

        // TODO: Support other types, at least numbers.
        String s = (String)doc.get( _shardAffinityKey );
        return s.codePointAt( s.length() - 1 ) %  ( _fallbackShards + 1);
    }

}
