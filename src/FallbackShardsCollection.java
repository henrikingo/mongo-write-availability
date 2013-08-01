import com.mongodb.*;

public class FallbackShardsCollection {

    private DBCollection _collection;
    private int          _fallbackShards = 1; 
    private String       _shardAffinityKey;
    
    public FallbackShardsCollection(){
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
        for( int i=0; i < _fallbackShards; i++ ) {
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
        return null;
    }    

    public int getShardAffinity( DBObject doc ){
        // This is just a simple hash:
        // Take last character and divide by number of shards available to rotate on.
        // Note that total number of shards available is one greater than "fallback" shards.

        // TODO: Support other types, at least numbers.
        String s = (String)doc.get( _shardAffinityKey );
        return s.codePointAt( s.length() - 1 ) %  ( _fallbackShards + 1);
    }

}
