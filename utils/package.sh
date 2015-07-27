#!/bin/bash
time=`date +"%F"`
package_name="eulermind-${time//:/-}"
(cd ../ && git pull origin && mvn clean && mvn package)  && \
rm ${package_name}* -rf && \
mkdir $package_name &&\
cp -r ../eulermind/target/jars/ $package_name && \
cp ../eulermind/target/eulermind-1.0-SNAPSHOT.jar $package_name/jars && \
cp eulermind.sh $package_name/ && \
cp eulermind.bat $package_name/ && \
unix2dos $package_name/start.bat && \
zip -r ${package_name}.zip ${package_name}
