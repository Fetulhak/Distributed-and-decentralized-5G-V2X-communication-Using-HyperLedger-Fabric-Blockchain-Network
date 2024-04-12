#!/bin/bash


# installChaincode PEER ORG
function installChaincode() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode queryinstalled --output json | jq -r 'try (.installed_chaincodes[].package_id)' | grep ^${PACKAGE_ID}$ >&log.txt
  if test $? -ne 0; then
    peer lifecycle chaincode install ${CC_NAME}.tar.gz >&log.txt
    res=$?
  fi
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode installation on peer0.org${ORG} has failed"
  successln "Chaincode is installed on peer0.org${ORG}"
}

# queryInstalled PEER ORG
function queryInstalled() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode queryinstalled --output json | jq -r 'try (.installed_chaincodes[].package_id)' | grep ^${PACKAGE_ID}$ >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Query installed on peer0.org${ORG} has failed"
  successln "Query installed successful on peer0.org${ORG} on channel"
}

# approveForMyOrg VERSION PEER ORG
function approveForMyOrg() {
  ORG=$1
  setGlobals $ORG
  set -x
  peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --package-id ${PACKAGE_ID} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode definition approved on peer0.org${ORG} on channel '$CHANNEL_NAME' failed"
  successln "Chaincode definition approved on peer0.org${ORG} on channel '$CHANNEL_NAME'"
}

# checkCommitReadiness VERSION PEER ORG
function checkCommitReadiness() {
  ORG=$1
  shift 1
  setGlobals $ORG
  infoln "Checking the commit readiness of the chaincode definition on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
  local rc=1
  local COUNTER=1
  # continue to poll
  # we either get a successful response, or reach MAX RETRY
  while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
    sleep $DELAY
    infoln "Attempting to check the commit readiness of the chaincode definition on peer0.org${ORG}, Retry after $DELAY seconds."
    set -x
    peer lifecycle chaincode checkcommitreadiness --channelID $CHANNEL_NAME --name ${CC_NAME} --version ${CC_VERSION} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} --output json >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    let rc=0
    for var in "$@"; do
      grep "$var" log.txt &>/dev/null || let rc=1
    done
    COUNTER=$(expr $COUNTER + 1)
  done
  cat log.txt
  if test $rc -eq 0; then
    infoln "Checking the commit readiness of the chaincode definition successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
  else
    fatalln "After $MAX_RETRY attempts, Check commit readiness result on peer0.org${ORG} is INVALID!"
  fi
}

# commitChaincodeDefinition VERSION PEER ORG (PEER ORG)...
function commitChaincodeDefinition() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x
  peer lifecycle chaincode commit -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" --channelID $CHANNEL_NAME --name ${CC_NAME} "${PEER_CONN_PARMS[@]}" --version ${CC_VERSION} --sequence ${CC_SEQUENCE} ${INIT_REQUIRED} ${CC_END_POLICY} ${CC_COLL_CONFIG} >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Chaincode definition commit failed on peer0.org${ORG} on channel '$CHANNEL_NAME' failed"
  successln "Chaincode definition committed on channel '$CHANNEL_NAME'"
}

# queryCommitted ORG
function queryCommitted() {
  ORG=$1
  setGlobals $ORG
  EXPECTED_RESULT="Version: ${CC_VERSION}, Sequence: ${CC_SEQUENCE}, Endorsement Plugin: escc, Validation Plugin: vscc"
  infoln "Querying chaincode definition on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
  local rc=1
  local COUNTER=1
  # continue to poll
  # we either get a successful response, or reach MAX RETRY
  while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
    sleep $DELAY
    infoln "Attempting to Query committed status on peer0.org${ORG}, Retry after $DELAY seconds."
    set -x
    peer lifecycle chaincode querycommitted --channelID $CHANNEL_NAME --name ${CC_NAME} >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    test $res -eq 0 && VALUE=$(cat log.txt | grep -o '^Version: '$CC_VERSION', Sequence: [0-9]*, Endorsement Plugin: escc, Validation Plugin: vscc')
    test "$VALUE" = "$EXPECTED_RESULT" && let rc=0
    COUNTER=$(expr $COUNTER + 1)
  done
  cat log.txt
  if test $rc -eq 0; then
    successln "Query chaincode definition successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
  else
    fatalln "After $MAX_RETRY attempts, Query chaincode definition result on peer0.org${ORG} is INVALID!"
  fi
}

function chaincodeInvokeInit() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x
  fcn_call='{"function":"'init'","Args":["enrolCC", "validCC"]}'
  infoln "invoke fcn call:${fcn_call}"
  #peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}" --isInit -c ${fcn_call} >&log.txt
  peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}" -c  "${fcn_call}"  >&log.txt
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Invoke execution on $PEERS failed "
  successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
}

function chaincodeInvoke() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x


  # start=$(date +%s.%N)
  # for i in {1..10}; do
  # fcn_call='{"function":"'retrieveValidationResponse'","Args":["0102030405060708","5602030785060713", "'$i'"]}' 
  # #fcn_call='{"function":"'retrieveValidationResponse'","Args":["0882030405060708","5992030785060713", "'$((i+1000))'"]}'
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 10 validation response retrival was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..50}; do
  # fcn_call='{"function":"'retrieveValidationResponse'","Args":["0102030405060708","5602030785060713", "'$((i+10))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 50 validation response retrival was $ds"
  # #sleep 5
  
  
  start=$(date +%s.%N)
  for i in {1..100}; do
  fcn_call='{"function":"'retrieveValidationResponse'","Args":["0102030405060708","5602030785060713", "'$((i+60))'"]}' 
  peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  end=$(date +%s.%N)
  dt=$(echo "$end - $start" | bc)
  dd=$(echo "$dt/86400" | bc)
  dt2=$(echo "$dt-86400*$dd" | bc)
  dh=$(echo "$dt2/3600" | bc)
  dt3=$(echo "$dt2-3600*$dh" | bc)
  dm=$(echo "$dt3/60" | bc)
  ds=$(echo "$dt3-60*$dm" | bc)
  echo "Runtime in days was $dd"
  echo "Runtime in hours was $dh"
  echo "Runtime in minutes was $dm"
  echo "Runtime in seconds for 100 validation response retrival was $ds"
  #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..250}; do
  # fcn_call='{"function":"'retrieveValidationResponse'","Args":["0102030405060708","5602030785060713", "'$((i+160))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 250 validation response retrival was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..500}; do
  # fcn_call='{"function":"'retrieveValidationResponse'","Args":["0102030405060708","5602030785060713", "'$((i+410))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 500 validation response retrival was $ds"
  # #sleep 5 
  
  
  
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Invoke execution on $PEERS failed "
  successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
}


function chaincodeInvokeEx() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x


  # start=$(date +%s.%N)
  # for i in {1..10}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$i'", "'$((i+10))'", "'$i'"]}'
  # #fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0882030405060708","5992030785060713", "'$((i+1000))'", "'$((i+50))'", "'$((i+1000))'"]}' 
   
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 10 authorixation info upload tx was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..50}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+10))'", "'$((i+20))'", "'$((i+10))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 50 authorization info upload tx was $ds"
  # #sleep 5
  
  
  start=$(date +%s.%N)
  for i in {1..100}; do
  fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+60))'", "'$((i+80))'", "'$((i+60))'"]}' 
  peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  end=$(date +%s.%N)
  dt=$(echo "$end - $start" | bc)
  dd=$(echo "$dt/86400" | bc)
  dt2=$(echo "$dt-86400*$dd" | bc)
  dh=$(echo "$dt2/3600" | bc)
  dt3=$(echo "$dt2-3600*$dh" | bc)
  dm=$(echo "$dt3/60" | bc)
  ds=$(echo "$dt3-60*$dm" | bc)
  echo "Runtime in days was $dd"
  echo "Runtime in hours was $dh"
  echo "Runtime in minutes was $dm"
  echo "Runtime in seconds for 100 authorization info upload tx was $ds"
  #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..250}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+160))'", "'$((i+180))'", "'$((i+160))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 250 authorization info upload tx was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..500}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+410))'", "'$((i+430))'", "'$((i+410))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 500 authorization info upload tx was $ds"
  # #sleep 5
  
  
  
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Invoke execution on $PEERS failed "
  successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
}


function chaincodeInvokeExmore() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x


  # start=$(date +%s.%N)
  # for i in {1..10}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$i'", "'$((i+15))'", "'$i'"]}' 
  # #fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0882030405060708","5992030785060713", "'$((i+1000))'", "'$((i+105))'", "'$((i+1000))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 10 additional authorization info upload tx was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..50}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+10))'", "'$((i+25))'", "'$((i+10))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 50 additional authorization info upload tx was $ds"
  # #sleep 5
  
  start=$(date +%s.%N)
  for i in {1..100}; do
  fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+60))'", "'$((i+85))'", "'$((i+60))'"]}' 
  peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  end=$(date +%s.%N)
  dt=$(echo "$end - $start" | bc)
  dd=$(echo "$dt/86400" | bc)
  dt2=$(echo "$dt-86400*$dd" | bc)
  dh=$(echo "$dt2/3600" | bc)
  dt3=$(echo "$dt2-3600*$dh" | bc)
  dm=$(echo "$dt3/60" | bc)
  ds=$(echo "$dt3-60*$dm" | bc)
  echo "Runtime in days was $dd"
  echo "Runtime in hours was $dh"
  echo "Runtime in minutes was $dm"
  echo "Runtime in seconds for 50 validation request tx was $ds"
  #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..250}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+160))'", "'$((i+185))'", "'$((i+160))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 100 validation request tx was $ds"
  # #sleep 5
  
  # start=$(date +%s.%N)
  # for i in {1..500}; do
  # fcn_call='{"function":"'uploadAuthorizationInfo'","Args":["0102030405060708","5602030785060713", "'$((i+410))'", "'$((i+435))'", "'$((i+410))'"]}' 
  # peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  # end=$(date +%s.%N)
  # dt=$(echo "$end - $start" | bc)
  # dd=$(echo "$dt/86400" | bc)
  # dt2=$(echo "$dt-86400*$dd" | bc)
  # dh=$(echo "$dt2/3600" | bc)
  # dt3=$(echo "$dt2-3600*$dh" | bc)
  # dm=$(echo "$dt3/60" | bc)
  # ds=$(echo "$dt3-60*$dm" | bc)
  # echo "Runtime in days was $dd"
  # echo "Runtime in hours was $dh"
  # echo "Runtime in minutes was $dm"
  # echo "Runtime in seconds for 250 validation request tx was $ds"
  # #sleep 5
  
  
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Invoke execution on $PEERS failed "
  successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
}


function chaincodeInvokeRevoke() {
  parsePeerConnectionParameters $@
  res=$?
  verifyResult $res "Invoke transaction failed on channel '$CHANNEL_NAME' due to uneven number of peer and org parameters "

  # while 'peer chaincode' command can get the orderer endpoint from the
  # peer (if join was successful), let's supply it directly as we know
  # it using the "-o" option
  set -x
  
  

  start=$(date +%s.%N)
  for i in {1..250}; do
  fcn_call='{"function":"'initiateRevocation'","Args":["'$((i+180))'"]}' 
  peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "$ORDERER_CA" -C $CHANNEL_NAME -n ${CC_NAME} "${PEER_CONN_PARMS[@]}"  -c "${fcn_call}" >&log.txt; done
  
  end=$(date +%s.%N)
  dt=$(echo "$end - $start" | bc)
  dd=$(echo "$dt/86400" | bc)
  dt2=$(echo "$dt-86400*$dd" | bc)
  dh=$(echo "$dt2/3600" | bc)
  dt3=$(echo "$dt2-3600*$dh" | bc)
  dm=$(echo "$dt3/60" | bc)
  ds=$(echo "$dt3-60*$dm" | bc)
  echo "Runtime in days was $dd"
  echo "Runtime in hours was $dh"
  echo "Runtime in minutes was $dm"
  echo "Runtime in seconds for 50 revocation process tx was $ds"
  #sleep 5
  
  
  
  
  
  
  
  
  res=$?
  { set +x; } 2>/dev/null
  cat log.txt
  verifyResult $res "Invoke execution on $PEERS failed "
  successln "Invoke transaction successful on $PEERS on channel '$CHANNEL_NAME'"
}



function chaincodeQuery() {
  ORG=$1
  setGlobals $ORG
  infoln "Querying on peer0.org${ORG} on channel '$CHANNEL_NAME'..."
  local rc=1
  local COUNTER=1
  # continue to poll
  # we either get a successful response, or reach MAX RETRY
  while [ $rc -ne 0 -a $COUNTER -lt $MAX_RETRY ]; do
    sleep $DELAY
    infoln "Attempting to Query peer0.org${ORG}, Retry after $DELAY seconds."
    set -x
    peer chaincode query -C $CHANNEL_NAME -n ${CC_NAME} -c '{"Args":["org.hyperledger.fabric:GetMetadata"]}' >&log.txt
    res=$?
    { set +x; } 2>/dev/null
    let rc=$res
    COUNTER=$(expr $COUNTER + 1)
  done
  cat log.txt
  if test $rc -eq 0; then
    successln "Query successful on peer0.org${ORG} on channel '$CHANNEL_NAME'"
  else
    fatalln "After $MAX_RETRY attempts, Query result on peer0.org${ORG} is INVALID!"
  fi
}
