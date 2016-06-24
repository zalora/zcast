# Hazelcast + Server Side Compression = ZCast

## Why Open Source?

- ~~Public repositories are for free :grin:~~
- We wanted to share a working use case for an interceptor in Hazelcast  

## Usage

You're not really supposed to run a pre-alpha-preview-too-dangerous-to-even-run-on-staging piece of software, but if you do,
don't say we didn't warn you ;-)

### Build

`mvn package` should create a fat-jar with all the dependencies

### Run

In previous versions, compressions was only turned on for the default map. As we're now distributing the data among a couple of maps, we 
made it configurable. Just pass in a comma separated list of maps (Including the `hz_memcache_` prefix) via the flag `compressed.maps`:

`java -server -Xmx6G -Dcompressed.maps=hz_memcache_map1,hz_memcache_map2 -jar target/zcast-all-0.1.x.jar`

## License

The lonely interceptor is available under the Apache 2 License. Please see the License file for more information

## Credits

We're using the following fantastic libraries for this project:
- [Hazelcast](https://github.com/hazelcast/hazelcast) - the open source in-memory data grid
- [Guava](https://github.com/google/guava) - Google's core libs
- [LZ4](https://github.com/jpountz/lz4-java) - a super-fast compression algo to transparently compress Hazelcast's content
