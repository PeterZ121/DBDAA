#!/bin/bash

./network.sh down
./network.sh up
./network.sh createChannel -c channel1


./network.sh deployCC -c channel1 -ccn basic -ccp ../asset-transfer-basic/chaincode-go/ -ccl go 


./addOrgToChannel.sh 3 3



./deployChainCode.sh 3 3





