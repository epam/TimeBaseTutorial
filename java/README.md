# Timebase Tutorial

### About these samples

### Requirements

* Java 11

### Build

To build these samples, run:

```
gradlew clean build 
```

You can also build and run these samples from IntelliJ/IDEA Community Edition

### Run

* Start Timebase Docker image
  ```  
   docker run --rm -d -p 8011:8011 --name=timebase-server --ulimit nofile=65536:65536 finos/timebase-ce-server:latest
   ```
* Run any sample

