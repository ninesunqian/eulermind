#!/bin/bash
time=`date +"%F-%T"`
package_name="eulermind-${time//:/-}"
(cd ../ && mvn package)  && \
mkdir $package_name &&\
cp -r ../eulermind/target/jars/ $package_name && \
cp ../eulermind/target/eulermind-1.0-SNAPSHOT.jar $package_name/jars && \
cp start.sh $package_name/ && \
cp start.bat $package_name/ && \
tar zcvf ${package_name}.tar.gz ${package_name}


