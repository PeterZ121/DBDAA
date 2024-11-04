#!/bin/bash
#需要修改org4-crypto.yaml文件和add4/configtx文件
#需要修改addorg4/compose/compose-org4.yaml 和 ORG_PATH/compose/docker/docker-compose-org4.yaml

ORG_NUMBER=$1
PEER_NUMBER=$2

# 设置组织相关的路径
ORG_PATH="addOrg${ORG_NUMBER}"
ORG_NAME="org${ORG_NUMBER}"
ORG_MSP_ID="Org${ORG_NUMBER}MSP"

cd "${ORG_PATH}"     
# 为 "${ORG_NAME}" 对等方创建证书和密钥
../../bin/cryptogen generate --config="${ORG_NAME}"-crypto.yaml --output="../organizations"    

export FABRIC_CFG_PATH=$PWD
../../bin/configtxgen -printOrg "${ORG_MSP_ID}" > ../organizations/peerOrganizations/"${ORG_NAME}".example.com/"${ORG_NAME}".json

# 启动Docker容器
DOCKER_SOCK="/var/run/docker.sock"
echo $DOCKER_SOCK

DOCKER_SOCK=${DOCKER_SOCK} docker-compose -f compose/compose-"${ORG_NAME}".yaml -f compose/docker/docker-compose-"${ORG_NAME}".yaml up -d 2>&1

cd ..
# 获取通道配置
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
export CORE_PEER_ADDRESS=localhost:7051

peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c channel1 --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

# 将配置转换为 JSON 并对其进行修剪
cd channel-artifacts
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
jq ".data.data[0].payload.data.config" config_block.json > config.json

# 添加 org${ORG_NUMBER} 的配置
jq -s '.[0] * {"channel_group":{"groups":{"Application":{"groups": {"'"${ORG_MSP_ID}"'":.[1]}}}}}' config.json ../organizations/peerOrganizations/"${ORG_NAME}".example.com/"${ORG_NAME}".json > modified_config.json

configtxlator proto_encode --input config.json --type common.Config --output config.pb
configtxlator proto_encode --input modified_config.json --type common.Config --output modified_config.pb
configtxlator compute_update --channel_id channel1 --original config.pb --updated modified_config.pb --output "${ORG_NAME}"_update.pb

configtxlator proto_decode --input "${ORG_NAME}"_update.pb --type common.ConfigUpdate --output "${ORG_NAME}"_update.json

echo '{"payload":{"header":{"channel_header":{"channel_id":"'channel1'", "type":2}},"data":{"config_update":'$(cat "${ORG_NAME}"_update.json)'}}}' | jq . > "${ORG_NAME}"_update_in_envelope.json

configtxlator proto_encode --input "${ORG_NAME}"_update_in_envelope.json --type common.Envelope --output "${ORG_NAME}"_update_in_envelope.pb

cd ..
peer channel signconfigtx -f channel-artifacts/"${ORG_NAME}"_update_in_envelope.pb
peer channel update -f channel-artifacts/"${ORG_NAME}"_update_in_envelope.pb -c channel1 -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

# 将 ${PEER_NUMBER} 节点加入通道
for ((i=0; i<PEER_NUMBER; i++))
do
  export CORE_PEER_TLS_ENABLED=true
  export CORE_PEER_LOCALMSPID="${ORG_MSP_ID}"
  export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/"${ORG_NAME}".example.com/peers/peer"${i}"."${ORG_NAME}".example.com/tls/ca.crt
  export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/"${ORG_NAME}".example.com/users/Admin@"${ORG_NAME}".example.com/msp
  export CORE_PEER_ADDRESS=localhost:1"${ORG_NUMBER}$(printf "%02d" $((i+0)))1"

  peer channel fetch 0 channel-artifacts/channel1.block -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c channel1 --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
  peer channel join -b channel-artifacts/channel1.block

  echo "peer${i}.${ORG_NAME} 已加入 channel1"
done

# 更新锚点节点
peer channel fetch config channel-artifacts/config_block.pb -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com -c channel1 --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
cd channel-artifacts
configtxlator proto_decode --input config_block.pb --type common.Block --output config_block.json
jq ".data.data[0].payload.data.config" config_block.json > config.json

portNum="1${ORG_NUMBER}001"
jq --arg org "$ORG_NUMBER" --argjson port "$portNum" '.channel_group.groups.Application.groups[("Org" + $org + "MSP")].values += {"AnchorPeers":{"mod_policy": "Admins","value":{"anchor_peers": [{"host": ("peer0.org" + $org + ".example.com"),"port": $port}]},"version": "0"}}' config.json > modified_anchor_config.json

configtxlator proto_encode --input config.json --type common.Config --output config.pb
configtxlator proto_encode --input modified_anchor_config.json --type common.Config --output modified_anchor_config.pb
configtxlator compute_update --channel_id channel1 --original config.pb --updated modified_anchor_config.pb --output anchor_update.pb
configtxlator proto_decode --input anchor_update.pb --type common.ConfigUpdate --output anchor_update.json
echo '{"payload":{"header":{"channel_header":{"channel_id":"channel1", "type":2}},"data":{"config_update":'$(cat anchor_update.json)'}}}' | jq . > anchor_update_in_envelope.json
configtxlator proto_encode --input anchor_update_in_envelope.json --type common.Envelope --output anchor_update_in_envelope.pb
cd ..
peer channel update -f channel-artifacts/anchor_update_in_envelope.pb -c channel1 -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"

echo "完成组织 ${ORG_NAME} 所有 peer 节点加入通道和锚点更新"

