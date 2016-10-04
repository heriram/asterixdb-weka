#!/bin/bash
usage()
{
cat << EOF
usage: $0 [restart|stop] options

This script run re-install a new copy of Asterix (managix)

OPTIONS:
   -o               Optional - run maven offline to avoid downloading "xml" metadata files (same as mvn -o)
   -c|--clean       Optional - run clean before building the package
   -m|--managixdir  Managix directory (default: ${MANAGIX_HOME})
   -s|--asterixsrc  Asterix source code directory
   -d|--asterixdev  Home directory for external feed adapters and UDF development
   -v|--version     Asterix version (default: ${VERSION})
   -n|--no-rebuild  Skip rebuilding

   Example: ./reinstall_asterix.sh -m /Users/username/asterix-mgnt -o -s /Users/username/Work/Asterix/incubator-asterixdb
      -d /Users/username/Work/Asterix/asterix-kba -v 0.8.9
EOF
}

ASTERIX_SOURCE=
ASTERIX_EXTERNAL_DEV_HOME=
ONLINE=
CLEAN=
VERSION=0.8.9
INSTANCE=a1
REBUILD=true

while [[ $# > 0 ]]
do
key="$1"

case $key in
    -o|--online)
    ONLINE="-o"
    #shift # past argument
    ;;
    -c|--clean)
    CLEAN="mvn clean &&"
    #shift # past argument
    ;;
    -m|--managixdir)
    MANAGIX_HOME="$2"
    if [ -z $MANAGIX_HOME ]
    then
    	usage
    	exit 1
    fi
    shift # past argument
    ;;
    -v|--version)
    VERSION="$2"
    if [ -z $VERSION ]
    then
    	usage
    	exit 1
    fi
    shift # past argument
    ;;
    -d|--asterixdev)
    ASTERIX_EXTERNAL_DEV_HOME="$2"
    if [ -z $ASTERIX_EXTERNAL_DEV_HOME ]
    then
    	usage
    	exit 1
    fi
    shift # past argument
    ;;
    -s|--asterixsrc)
    ASTERIX_SOURCE="$2"
    if [ -z $ASTERIX_SOURCE ]
    then
    	usage
    	exit 1
    fi
    shift # past argument
    ;;
	-n|--no-rebuild)
	REBUILD=false
	
	;;
    --default)
    DEFAULT=YES
    ;;
    *)
    	usage
    	exit 1
            # unknown option
    ;;
esac
shift # past argument or value
done

ASTERIX_EXTERNAL_DEV_RESOURCES=${ASTERIX_EXTERNAL_DEV_HOME}/src/main/resources

if [ $REBUILD == true ]
then
    echo ASTERIX_EXTERNAL_DEV_RESOURCES = ${ASTERIX_EXTERNAL_DEV_RESOURCES}
    echo ASTERIX_SOURCE = ${ASTERIX_SOURCE}
    echo "MAVEN = mvn clean && mvn install -DskipTests ${ONLINE}"
    echo VERSION = ${VERSION}
#exit 1

    echo "Cleaning up"
    rm -rf ${MANAGIX_HOME}/*
    rm -rf ${MANAGIX_HOME}/.*

    echo "Recompiling Asterix"
    cd ${ASTERIX_SOURCE}
    echo "Running: mvn clean && mvn install -DskipTests ${ONLINE}"
    mvn clean ${ONLINE}
    mvn install -DskipTests ${ONLINE}
fi

echo "Installing new Asterix Copy"
cp ${ASTERIX_SOURCE}/asterix-installer/target/asterix-installer-${VERSION}-SNAPSHOT-binary-assembly.zip ${MANAGIX_HOME}/
cd ${MANAGIX_HOME}
unzip asterix-installer-${VERSION}-SNAPSHOT-binary-assembly.zip
cp ${ASTERIX_EXTERNAL_DEV_RESOURCES}/reset_a1.sh ./
${MANAGIX_HOME}/bin/managix configure
cp ${ASTERIX_EXTERNAL_DEV_RESOURCES}/AsterixManagement/local.xml ${MANAGIX_HOME}/clusters/local/

echo "Cleaning up"
rm asterix-installer-${VERSION}-SNAPSHOT-binary-assembly.zip
