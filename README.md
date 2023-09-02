
# PMTiles Java Reader

Small library to retrieve tiles from a PMTiles format file. The library attempts to be reasonably efficient and stay compatible with the Java runtimes back to Android 4.1 at the same time.


## Usage

        try (Reader reader = new Reader(new File("a_file"))) {
            byte[] tile = reader.getTile(19, 1, 1);
            ...  
        }
    
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
