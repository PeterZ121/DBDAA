#!/bin/bash

./network.sh down
./network.sh up
./network.sh createChannel -c channel1
./network.sh createChannel -c channel2

./network.sh deployCC -c channel1 -ccn basic -ccp ../asset-transfer-basic/chaincode-go/ -ccl go 
./network.sh deployCC -c channel2 -ccn basic -ccp ../asset-transfer-basic/chaincode-go/ -ccl go 

./addOrgToChannel.sh 3 3
./addOrgToChannel2.sh 3 3
./addOrgToChannel2.sh 5 6
./addOrgToChannel2.sh 6 50
./addOrgToChannel2.sh 7 50


./deployChainCode.sh 3 3
./deployChainCode2.sh 3 3
./deployChainCode2.sh 5 6




