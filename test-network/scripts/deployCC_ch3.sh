#!/bin/bash

source scripts/utils.sh

CHANNEL_NAME=${1:-"mychannel"}
CC_NAME=${2}
CC_SRC_PATH=${3}
CC_SRC_LANGUAGE=${4}
CC_VERSION=${5:-"1.0"}
CC_SEQUENCE=${6:-"1"}
CC_INIT_FCN=${7:-"NA"}
CC_END_POLICY=${8:-"NA"}
CC_COLL_CONFIG=${9:-"NA"}
DELAY=${10:-"3"}
MAX_RETRY=${11:-"5"}
VERBOSE=${12:-"false"}

println "executing with the following"
println "- CHANNEL_NAME: ${C_GREEN}${CHANNEL_NAME}${C_RESET}"
println "- CC_NAME: ${C_GREEN}${CC_NAME}${C_RESET}"
println "- CC_SRC_PATH: ${C_GREEN}${CC_SRC_PATH}${C_RESET}"
println "- CC_SRC_LANGUAGE: ${C_GREEN}${CC_SRC_LANGUAGE}${C_RESET}"
println "- CC_VERSION: ${C_GREEN}${CC_VERSION}${C_RESET}"
println "- CC_SEQUENCE: ${C_GREEN}${CC_SEQUENCE}${C_RESET}"
println "- CC_END_POLICY: ${C_GREEN}${CC_END_POLICY}${C_RESET}"
println "- CC_COLL_CONFIG: ${C_GREEN}${CC_COLL_CONFIG}${C_RESET}"
println "- CC_INIT_FCN: ${C_GREEN}${CC_INIT_FCN}${C_RESET}"
println "- DELAY: ${C_GREEN}${DELAY}${C_RESET}"
println "- MAX_RETRY: ${C_GREEN}${MAX_RETRY}${C_RESET}"
println "- VERBOSE: ${C_GREEN}${VERBOSE}${C_RESET}"

FABRIC_CFG_PATH=$PWD/../config/

#User has not provided a name
if [ -z "$CC_NAME" ] || [ "$CC_NAME" = "NA" ]; then
  fatalln "No chaincode name was provided. Valid call example: ./network.sh deployCC -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

# User has not provided a path
elif [ -z "$CC_SRC_PATH" ] || [ "$CC_SRC_PATH" = "NA" ]; then
  fatalln "No chaincode path was provided. Valid call example: ./network.sh deployCC -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

# User has not provided a language
elif [ -z "$CC_SRC_LANGUAGE" ] || [ "$CC_SRC_LANGUAGE" = "NA" ]; then
  fatalln "No chaincode language was provided. Valid call example: ./network.sh deployCC -ccn basic -ccp ../asset-transfer-basic/chaincode-go -ccl go"

## Make sure that the path to the chaincode exists
elif [ ! -d "$CC_SRC_PATH" ] && [ ! -f "$CC_SRC_PATH" ]; then
  fatalln "Path to chaincode does not exist. Please provide different path."
fi

CC_SRC_LANGUAGE=$(echo "$CC_SRC_LANGUAGE" | tr [:upper:] [:lower:])

# do some language specific preparation to the chaincode before packaging
if [ "$CC_SRC_LANGUAGE" = "go" ]; then
  CC_RUNTIME_LANGUAGE=golang

  infoln "Vendoring Go dependencies at $CC_SRC_PATH"
  pushd $CC_SRC_PATH
  GO111MODULE=on go mod vendor
  popd
  successln "Finished vendoring Go dependencies"

elif [ "$CC_SRC_LANGUAGE" = "java" ]; then
  CC_RUNTIME_LANGUAGE=java

  rm -rf $CC_SRC_PATH/build/install/
  infoln "Compiling Java code..."
  pushd $CC_SRC_PATH
  ./gradlew installDist
  ./gradlew copyJarToBin build
  popd
  successln "Finished compiling Java code"
  CC_SRC_PATH=$CC_SRC_PATH/build/install/$CC_NAME
  

elif [ "$CC_SRC_LANGUAGE" = "javascript" ]; then
  CC_RUNTIME_LANGUAGE=node

elif [ "$CC_SRC_LANGUAGE" = "typescript" ]; then
  CC_RUNTIME_LANGUAGE=node

  infoln "Compiling TypeScript code into JavaScript..."
  pushd $CC_SRC_PATH
  npm install
  npm run build
  popd
  successln "Finished compiling TypeScript code into JavaScript"

else
  fatalln "The chaincode language ${CC_SRC_LANGUAGE} is not supported by this script. Supported chaincode languages are: go, java, javascript, and typescript"
  exit 1
fi

INIT_REQUIRED="--init-required"
# check if the init fcn should be called
if [ "$CC_INIT_FCN" = "NA" ]; then
  INIT_REQUIRED=""
fi
if [ "$CC_END_POLICY" = "NA" ]; then
  CC_END_POLICY=""
else
  CC_END_POLICY="--signature-policy $CC_END_POLICY"
fi

if [ "$CC_COLL_CONFIG" = "NA" ]; then
  CC_COLL_CONFIG=""
else
  CC_COLL_CONFIG="--collections-config $CC_COLL_CONFIG"
fi

# import utils
. scripts/envVar.sh
. scripts/ccutils.sh

packageChaincode() {
  set -x
  
  #peer lifecycle chaincode package ${CC_NAME}.tar.gz --path ${CC_SRC_PATH} --lang ${CC_RUNTIME_LANGUAGE} --label ${CC_NAME}_${CC_VERSION} >&log.txt
  peer lifecycle chaincode package ${CC_NAME}.tar.gz --path ../asset-transfer-basic/chaincode-java/ --lang ${CC_RUNTIME_LANGUAGE} --label ${CC_NAME}_${CC_VERSION} >&log.txt
  #peer lifecycle chaincode package basic_6.tar.gz --path ../asset-transfer-basic/chaincode-java/ --lang java --label basic_6.0
  res=$?
  PACKAGE_ID=$(peer lifecycle chaincode calculatepackageid ${CC_NAME}.tar.gz)
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode packaging has failed"
  successln "Chaincode is packaged"
}

function checkPrereqs() {
  jq --version > /dev/null 2>&1

  if [[ $? -ne 0 ]]; then
    errorln "jq command not found..."
    errorln
    errorln "Follow the instructions in the Fabric docs to install the prereqs"
    errorln "https://hyperledger-fabric.readthedocs.io/en/latest/prereqs.html"
    exit 1
  fi
}

#check for prerequisites
checkPrereqs

## package the chaincode
packageChaincode

## Install chaincode on peer0.org1 and peer0.org2
infoln "Installing chaincode on peer0.org5..."
installChaincode 8
infoln "Install chaincode on peer0.org6..."
installChaincode 9
infoln "Install chaincode on peer0.org7..."
installChaincode 10

## query whether the chaincode is installed
queryInstalled 8

## approve the definition for org5
approveForMyOrg 8

## check whether the chaincode definition is ready to be committed
## expect org5 to have approved and org6 and org7 not to
checkCommitReadiness 8 "\"Org8MSP\": true" "\"Org9MSP\": false" "\"Org10MSP\": false"
checkCommitReadiness 9 "\"Org8MSP\": true" "\"Org9MSP\": false" "\"Org10MSP\": false"
checkCommitReadiness 10 "\"Org8MSP\": true" "\"Org9MSP\": false" "\"Org10MSP\": false"



## now approve also for org6
approveForMyOrg 9

## check whether the chaincode definition is ready to be committed
## expect them both org5 and org6 to have approved and org7 not
checkCommitReadiness 8 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": false"
checkCommitReadiness 9 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": false"
checkCommitReadiness 10 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": false"


## now approve also for org7
approveForMyOrg 10

## check whether the chaincode definition is ready to be committed
## expect them all three org5 and org6 and org7 to have approved 
checkCommitReadiness 8 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": true"
checkCommitReadiness 9 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": true"
checkCommitReadiness 10 "\"Org8MSP\": true" "\"Org9MSP\": true" "\"Org10MSP\": true"


## now that we know for sure all orgs have approved, commit the definition
commitChaincodeDefinition 8 9 10

## query on both orgs to see that the definition committed successfully
queryCommitted 8
queryCommitted 9
queryCommitted 10



## Invoke the chaincode - this does require that the chaincode have the 'initLedger'
## method defined
#if [ "$CC_INIT_FCN" = "NA" ]; then
  #infoln "Chaincode initialization is not required"
#else
  #chaincodeInvokeInit 6 9
#fi

chaincodeInvokeInit 8 9 10

# why should I have to sleep here? and for how long should i sleep. why the init transaction not performed with the speed of no need to sleep
sleep 3

chaincodeInvoke 8 9 10

sleep 10

chaincodeInvokeEx 8 9 10

sleep 20

chaincodeInvokeExmore 8 9 10

sleep 10

chaincodeInvokeRevoke 8 9 10

exit 0

sleep 10

#chaincodeInvokeRevoke 8 9 10

exit 0
