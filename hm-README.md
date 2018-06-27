- merge fixes by cherry picking
- build with tests 
```
./gradlew hibernate-envers:build --exclude-task checkstyleMain
```
- patch release version in gradle/version.properties
- patch build.gradle in envers dir in order to reference releases and not relative modules for all compile dependencies
- build without tests
```         
./gradlew hibernate-envers:build --exclude-task checkstyleMain -x test 
```
- publish to local maven

``` 
./gradlew hibernate-envers:publishToMavenLocal
```
- check pom of envers artifact. The patch version is not allowed in dependencies and only allowed for the artifact
- commit, build, push... to git repo
- upload envers artifact manually to nexus 
