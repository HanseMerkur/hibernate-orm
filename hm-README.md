- merge fixes by cherry picking
- path build.gradle in envers dir in order to reference releases and not relative modules
- commit, build, push...
```
./gradlew hibernate-envers:build
./gradlew hibernate-envers:publishToMavenLocal
```
