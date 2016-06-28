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
made it configurable. Have a look at the hazelcast.demo.xml file, the property `zcast.maps` holds the list of compressed maps.

`java -server -Xmx6G -jar target/zcast-all-0.1.x.jar`

### Logging

We're huge fans of the Bunyan project, so we also added a logback layout for it. It's not a real project
like [Punyan](https://www.github.com/zalora/punyan), so don't expect too much, but it produces compatible output. If you 
have recommendations how to improve it, you're welcome to send us pull requests!

You can configure everything via the hazelcast.xml file again, the demo file includes the settings, too:
- Set the property `hazelcast.logging.type` to `slf4j`
- Configure the file path to the logfile by setting the property `zcast.logging.file` to whatever you want 

## License

The lonely interceptor is available under the Apache 2 License. Please see the License file for more information

## Credits

We're using the following fantastic libraries for this project:
- [Hazelcast](https://github.com/hazelcast/hazelcast) - the open source in-memory data grid
- [Guava](https://github.com/google/guava) - Google's core libs
- [LZ4](https://github.com/jpountz/lz4-java) - a super-fast compression algo to transparently compress Hazelcast's content
