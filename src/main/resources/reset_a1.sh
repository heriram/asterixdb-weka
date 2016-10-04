#!/bin/bash
# Use > 1 to consume two arguments per pass in the loop (e.g. each
# argument has a corresponding value to go with it).
# Use > 0 to consume one or more arguments per pass in the loop (e.g.
# some arguments don't have a corresponding value to go with it such
# as in the --default example).
usage()
{
cat << EOF
usage: $0 [restart|stop] options

This script run re-install a new instance of asterix and optionally
install a new external library set

OPTIONS:
   -n name	       Name of Asterix instance
   -i      	       Optional - used to install external library (optional)
   -m managixdir   Managix directory
   -s lib source   Directory of source code for the external library
EOF
}
RESTART=false
STOP=false
INSTALL_LIB=false
SOURCE=/Users/thormartin/asterix-machine-learning
REMOVE=false

if [[ $# <  1 ]]
then
	usage
	exit 1
fi

while [[ $# > 0 ]]
do
key="$1"

case $key in
    restart)
    RESTART=true
    ;;
    stop)
    STOP=true
    ;;
    remove)
    REMOVE=true
    ;;
    -n|--name)
    INSTANCE_NAME="$2"
    if [ -z $INSTANCE_NAME ]
    then
    	$INSTANCE_NAME="a1"
    	shift
    fi
    shift # past argument
    ;;
    -i|--install)
    INSTALL_LIB=true
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
    -s|--asterixsrc)
    SOURCE="$2"
    if [ -z $SOURCE ]
    then
    	usage
    	exit 1
    fi
    shift # past argument
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

if [ ${RESTART} = true ]
then
${MANAGIX_HOME}/bin/managix stop -n ${INSTANCE_NAME}
${MANAGIX_HOME}/bin/managix start -n ${INSTANCE_NAME}
exit 0
fi

if [ ${STOP} = true ]
then
${MANAGIX_HOME}/bin/managix stop -n ${INSTANCE_NAME}
exit 0
fi

if [ ${REMOVE} = true ]
then
${MANAGIX_HOME}/bin/managix stop -n ${INSTANCE_NAME}
${MANAGIX_HOME}/bin/managix delete -n ${INSTANCE_NAME}
${MANAGIX_HOME}/bin/managix shutdown
exit 0
fi

#echo INSTANCE_NAME  = "${INSTANCE_NAME}"
#echo MANAGIX_HOME PATH     = "${MANAGIX_HOME}"
#echo SOURCE PATH    = "${SOURCE}"
#echo INSTALL_LIB = "${INSTALL_LIB}"

echo "Resetting asterix instance  ${INSTANCE_NAME}"
${MANAGIX_HOME}/bin/managix stop -n ${INSTANCE_NAME}
${MANAGIX_HOME}/bin/managix delete -n ${INSTANCE_NAME}
${MANAGIX_HOME}/bin/managix create -n ${INSTANCE_NAME} -c ${MANAGIX_HOME}/clusters/local/local.xml

if [ $INSTALL_LIB = true ]
   then
 	echo "Installing new libs (Adapters and exterenal functions)"
	${MANAGIX_HOME}/bin/managix stop -n ${INSTANCE_NAME}
	${MANAGIX_HOME}/bin/managix install -n ${INSTANCE_NAME} -d feeds -l mllib -p ${SOURCE}/target/asterix-external-lib-zip-binary-assembly.zip
	${MANAGIX_HOME}/bin/managix start -n ${INSTANCE_NAME}
	echo "Copying necessary resource files and directory"
	echo "Copy ${SOURCE}/src/main/resources/config.properties to ${MANAGIX_HOME}/clusters/local/working_dir/"
	cp ${SOURCE}/src/main/resources/config.properties ${MANAGIX_HOME}/clusters/local/working_dir/
fi

