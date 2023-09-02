
# PMTiles Java Reader

Small library to retrieve tiles from a PMTiles format file. The library attempts to be reasonably efficient and stay compatible with the Java runtimes back to Android 4.1 at the same time.


## Usage

        try (Reader reader = new Reader(new File("a_file"))) {
            byte[] tile = reader.getTile(19, 1, 1);
            ...  
        }
    
As caching all leaf directories (the root directory is always retained) can potentially exhaust the java heap, you can adjust the maximum number of leaf directories held in the cache with

    reader.setLeafDirectoryCacheSize(size);
    
the default value is currently 20.

## Limitations and other noteworthy points

- Directories cannot have more than Integer.MAX_VALUE entries and cannot be larger than the same both in compressed and de-compressed form, just as any other structures extracted from the file. 
- Tiles cannot be larger (compressed) than Integer.MAX_VALUE
- We do not attempt to de-compress tiles and leave that to the calling application
    
    reader.getTileCompression();
  
  will indicate the required de-compression method.
    
## Including in your project

You can either download the jar from github or add the following to your build.gradle

	...
	    repositories {
	        ...   
	        mavenCentral()
	        ...              
	    }
	...
	
	dependencies {
	    ...
	    implementation 'ch.poole.geo.pmtiles-reader:Reader:0.1.0'
	    ...
	}
