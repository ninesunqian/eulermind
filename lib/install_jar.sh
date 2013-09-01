mvn_install_jar ()
{
    mvn install:install-file -DgroupId=$1 -DartifactId=$2 -Dversion=$3 -Dpackaging=jar -Dfile=$4
}

mvn_install_jar commons-configuration commons-configuration 1.6 commons-configuration-1.6.jar

mvn_install_jar org.codehaus.groovy groovy 1.8.9 groovy-1.8.9.jar
mvn_install_jar org.openrdf.sesame sesame-model 2.6.10 sesame-model-2.6.10.jar
mvn_install_jar org.openrdf.sesame sesame-query 2.6.10 sesame-query-2.6.10.jar
mvn_install_jar org.openrdf.sesame sesame-rio-api 2.6.10 sesame-rio-api-2.6.10.jar
mvn_install_jar org.openrdf.sesame sesame-sail-api 2.6.10 sesame-sail-api-2.6.10.jar
mvn_install_jar org.iq80.snappy snappy 0.3 snappy-0.3.jar
mvn_install_jar antlr antlr 2.7.7 antlr-2.7.7.jar
mvn_install_jar asm asm 3.2 asm-3.2.jar
mvn_install_jar org.hibernate.javax.persistence hibernate-jpa-2.0-api 1.0.0.Final hibernate-jpa-2.0-api-1.0.0.Final.jar
mvn_install_jar net.java.dev.jna jna 3.5.2 jna-3.5.2.jar
mvn_install_jar org.javassist javassist 3.16.1-GA javassist-3.16.1-GA.jar

mvn_install_jar jline jline 0.9.94 jline-0.9.94.jar
mvn_install_jar javax.mail mail 1.4.5 mail-1.4.5.jar
mvn_install_jar net.java.dev.jna platform 3.5.2 platform-3.5.2.jar


mvn_install_jar com.orientechnologies orient-commons 1.5.0 orient-commons-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-client  1.5.0 orientdb-client-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-core 1.5.0  orientdb-core-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-distributed 1.5.0  orientdb-distributed-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-enterprise 1.5.0  orientdb-enterprise-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-graphdb 1.5.0  orientdb-graphdb-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-nativeos 1.5.0  orientdb-nativeos-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-object 1.5.0  orientdb-object-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-server 1.5.0  orientdb-server-1.5.0.jar
mvn_install_jar com.orientechnologies orientdb-tools 1.5.0  orientdb-tools-1.5.0.jar

mvn_install_jar com.tinkerpop pipes-2.4.0 pipes 2.4.0-SNAPSHOT.jar
mvn_install_jar com.tinkerpop blueprints-core 2.4.0 blueprints-core-2.4.0-SNAPSHOT.jar
mvn_install_jar com.tinkerpop blueprints-orient-graph 2.4.0 blueprints-orient-graph-2.4.0-SNAPSHOT.jar
mvn_install_jar com.tinkerpop gremlin-groovy 2.4.0 gremlin-groovy-2.4.0-SNAPSHOT.jar
mvn_install_jar com.tinkerpop gremlin-java 2.4.0 gremlin-java-2.4.0-SNAPSHOT.jar

mvn_install_jar com.hazelcast hazelcast 3.0-SNAPSHOT hazelcast-3.0-SNAPSHOT.jar
mvn_install_jar org.fusesource.jansi jansi 1.5 jansi-1.5.jar
mvn_install_jar org.apache.lucene lucene 1.4.3 lucene-1.4.3.jar
