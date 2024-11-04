#!/bin/bash
#
# Copyright IBM Corp All Rights Reserved
#
# SPDX-License-Identifier: Apache-2.0
#

# This script extends the Hyperledger Fabric test network by adding
# adding a third organization to the network
#

# prepending $PWD/../bin to PATH to ensure we are picking up the correct binaries
# this may be commented out to resolve installed version of tools if desired
../../bin/cryptogen generate --config=org4-crypto.yaml --output="../organizations"    

export FABRIC_CFG_PATH=$PWD
../../bin/configtxgen -printOrg Org4MSP > ../organizations/peerOrganizations/org4.example.com/org4.json

#需要修改addOrg4/compose/compose-org4.yaml 和 addOrg4/compose/docker/docker-compose-org4.yaml
docker-compose -f compose/compose-org4.yaml -f compose/docker/docker-compose-org4.yaml up -d   


cd ..
#由于 Org3 还不是频道的成员，因此我们需要以管理员身份运行 另一个组织获取通道配置。由于 Org1 是频道的成员，因此 Org1 管理员有权从排序服务获取通道配置。 发出以下命令以 Org1 管理员身份运行。

export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
export CORE_PEER_ADDRESS=localhost:7051

peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c channel1 --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"


#将配置转换为 JSON 并对其进行修剪
cd channel-artifacts

#现在我们将利用该工具解码此通道 配置块转换为JSON格式（可以由人类读取和修改）。 我们还必须剥离所有标头、元数据、创建者签名和 所以这与我们想要做出的改变无关。我们通过以下方式实现这一目标 工具的表示
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
jq ".data.data[0].payload.data.config" config_block.json > config.json

#添加 org3 加密材料
jq -s '.[0] * {"channel_group":{"groups":{"Application":{"groups": {"Org4MSP":.[1]}}}}}' config.json ../organizations/peerOrganizations/org4.example.com/org4.json > modified_config.json

configtxlator proto_encode --input config.json --type common.Config --output config.pb
configtxlator proto_encode --input modified_config.json --type common.Config --output modified_config.pb
configtxlator compute_update --channel_id channel2 --original config.pb --updated modified_config.pb --output org4_update.pb

configtxlator proto_decode --input org4_update.pb --type common.ConfigUpdate --output org4_update.json

echo '{"payload":{"header":{"channel_header":{"channel_id":"'channel1'", "type":2}},"data":{"config_update":'$(cat org4_update.json)'}}}' | jq . > org4_update_in_envelope.json

configtxlator proto_encode --input org4_update_in_envelope.json --type common.Envelope --output org4_update_in_envelope.pb


cd ..
peer channel signconfigtx -f channel-artifacts/org4_update_in_envelope.pb
#导出 Org2 环境变量：
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp
export CORE_PEER_ADDRESS=localhost:9051

peer channel update -f channel-artifacts/org4_update_in_envelope.pb -c channel1 -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

#将 Org4 加入频道
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org4MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org4.example.com/peers/peer0.org4.example.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org4.example.com/users/Admin@org4.example.com/msp
export CORE_PEER_ADDRESS=localhost:14051

peer channel fetch 0 channel-artifacts/channel1.block -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c channel1 --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

peer channel join -b channel-artifacts/channel1.block
