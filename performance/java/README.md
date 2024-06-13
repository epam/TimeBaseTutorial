# Timebase Tutorial

### About these samples
   
   Timebase performance test utility. It uses external data file to generate sample data and then run QQL on test and measure estimated time
  
### Requirements

* Java 11


### How to build

`./gradlew shadowJar`

it will create a "fat" jar for the application located in `build/libs/timebase-performance-1.0.1-all.jar`


### How to run 

1. Start TimeBase Server on port 8011

2. `java -cp ./build/libs/timebase-performance-1.0.1-all.jar com.epam.deltix.samples.timebase.performance.Runner -dataFile order_book_depth-ce.qsmsg.gz -stream order-book-tests -spaces 5 -qqlFile qql.txt`

This execution will use data file `order_book_depth-ce.qsmsg.gz` to create sample stream `order-book-tests` contains 5 `spaces` with 40m records for the different symbols.     
After it run all queries (each line) defined in `qql.txt` file.



### Program Arguments
```
Usage:
      -timebase dxtick://localhost:8011 -dataFile order_book_depth-ce.qsmsg.gz -stream order_book_depth -spaces 10 -qqlFile qql.txt
Parameters Definition:
      -timebase <value>  - timebase connection url
      -dataFile <path>   - path to qsmsg file contains sample data. if file is not provided, then only tests will be run
      -stream <name>     - stream to create. default is 'order-book-test'
      -spaces <value>    - number of spaces in the stream to create. default = 10
      -qqlFile <path>    - file contains qql queries to test performance
```	  