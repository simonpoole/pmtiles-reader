[![build status](https://github.com/simonpoole/pmtiles-reader/actions/workflows/javalib.yml/badge.svg)](https://github.com/simonpoole/pmtiles-reader/actions) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=alert_status)](https://sonarcloud.io/dashboard?id=simonpoole_pmtiles-reader) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=coverage)](https://sonarcloud.io/dashboard?id=simonpoole_pmtiles-reader) [![sonarcloud bugs](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=bugs)](https://sonarcloud.io/component_measures?id=simonpoole_pmtiles-reader&metric=bugs) [![sonarcloud maintainability](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=simonpoole_pmtiles-reader&metric=Maintainability) [![sonarcloud security](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=security_rating)](https://sonarcloud.io/component_measures?id=simonpoole_pmtiles-reader&metric=Security) [![sonarcloud reliability](https://sonarcloud.io/api/project_badges/measure?project=simonpoole_pmtiles-reader&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=simonpoole_pmtiles-reader&metric=Reliability)


# PMTiles Java Reader

Small library to retrieve tiles from a PMTiles format file. The library attempts to be reasonably efficient and stay compatible with the Android Java runtimes back to Android 4.1 at the same time.
See [PMTiles V3 specification](https://github.com/protomaps/PMTiles/blob/main/spec/v3/spec.md) for more information on the format.


## Usage

        try (Reader reader = new Reader(new File("a_file"))) {
            byte[] tile = reader.getTile(19, 1, 1);
            ...  
        }
    
As caching all leaf directories (the root directory is always retained) can potentially exhaust the java heap, you can adjust the maximum number of leaf directories held in the cache with

        reader.setLeafDirectoryCacheSize(size);
    
the default value is currently 20. If you have plenty of heap available increasing the value may improve performance a lot depending on the applications access patterns.

To read from a remote resource via HTTP range requests you need to provide a FileChannel that provides a wrapper around your HTTP implementation, a sample based on HttpURLConnection is included. A similar approach can be used for other HTTP implementations or other network protocols. [Vespucci 19.3](https://github.com/MarcusWolschon/osmeditor4android/blob/master/src/main/java/de/blau/android/util/OkHttpFileChannel.java) utilizes a similar OkHttp based version.

Example:

        try (Reader reader = new Reader(new HttpURLConnectionChannel("https://r2-public.protomaps.com/protomaps-sample-datasets/overture-pois.pmtiles"))) {
            byte[] tile = reader.getTile(19, 1, 1);
            ...  
        }
        
Detailed documentation can be found in the [JavaDoc](http://www.javadoc.io/doc/ch.poole.geo.pmtiles-reader/Reader/0.3.6).

## Limitations and other noteworthy points

- Directories cannot have more than Integer.MAX_VALUE entries and cannot be larger than the same both in compressed and de-compressed form, just as any other structures extracted from the file. 
- Tiles cannot be larger (compressed) than Integer.MAX_VALUE
- A substantial difference between accessing a conventional TMS/google/OSM style tile server and a remote PMTiles source is that the former requires essentially no local state, retrieving tiles
from a PMTiles source however is only efficient if the header and directories are at least partially cached. With other words if the underlying source changes you will likely no
longer be able to correctly access tiles without re-reading the meta data. The example HttpUrlConnectionChannel implements a simple mechanism based on the ETag header to detect this, but other 
mechanisms could be implemented too. 
- We only support GZip and Zstd compression of internal structures and metadata. Your application should catch UnsupportedOperationException to avoid crashing on files using something else. 
- We do not attempt to de-compress tiles and leave that to the calling application
    
    reader.getTileCompression();
  
  will indicate the required de-compression method. The advantage of this is that the library doesn't require any 3rd party runtime dependencies outside of the JRE naturally.

    
## Including in your project

Add the following to your build.gradle

	...
	    repositories {
	        ...   
	        mavenCentral()
	        ...              
	    }
	...
	
	dependencies {
	    ...
	    implementation 'ch.poole.geo.pmtiles-reader:Reader:0.3.6'
	    ...
	}
