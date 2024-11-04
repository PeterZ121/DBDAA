#!/bin/bash

ORG_NUMBER=$1
PEER_NUMBER=$2

# 设置组织相关的路径和名称
ORG_PATH="addOrg${ORG_NUMBER}"
ORG_NAME="org${ORG_NUMBER}"
ORG_MSP_ID="Org${ORG_NUMBER}MSP"

# 设置基本环境变量
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="${ORG_MSP_ID}"
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/"${ORG_NAME}".example.com/users/Admin@"${ORG_NAME}".example.com/msp

# 遍历PEER_NUMBER
for ((i=0; i<PEER_NUMBER; i++))
do
  # 设置当前 peer 节点的端口
  PEER_PORT=$(printf "localhost:1${ORG_NUMBER}%02d1" $((i+0)))
  export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/"${ORG_NAME}".example.com/peers/peer"${i}"."${ORG_NAME}".example.com/tls/ca.crt
  # 配置当前 peer 节点的地址
  export CORE_PEER_ADDRESS="${PEER_PORT}"
  echo "正在为 $ORG_NAME 的 peer${i} 安装链码，端口为 ${PEER_PORT}..."

  # 安装链码
  peer lifecycle chaincode install basic.tar.gz

  echo "链码安装并批准完成：$ORG_NAME 的 peer${i}"
done  

  # 查询已安装链码并提取 Package ID
#  PACKAGE_ID=$(peer lifecycle chaincode queryinstalled --output json | jq -r '.installed_chaincodes[0].package_id')
  PACKAGE_ID=$(peer lifecycle chaincode queryinstalled --output json | jq -r '.installed_chaincodes[] | select(.label == "basic_1.0.1").package_id')

  # 将 Package ID 存储到环境变量
  export CC_PACKAGE_ID=$PACKAGE_ID
  
  # 批准链码定义
  peer lifecycle chaincode approveformyorg -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem" --channelID channel2 --name basic --version 1.0.1 --package-id $CC_PACKAGE_ID --sequence 1
  
  peer lifecycle chaincode querycommitted --channelID channel2 --name basic --cafile "${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem"
  
echo "所有 Peer 节点操作完成！"

