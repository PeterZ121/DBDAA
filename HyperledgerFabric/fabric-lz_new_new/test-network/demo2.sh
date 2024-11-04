#!/bin/bash

# 检查是否提供了两个参数
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <orgX> <number_of_peers>"
    exit 1
fi

# 读取输入参数
ORG_NUM=$1
PEER_COUNT=$2

# Step 1: 启动网络并创建 channel2
./network.sh down
./network.sh up

./network.sh createChannel -c channel2

# Step 2: 部署基础链码到 channel1
./network.sh deployCC -c channel2 -ccn basic -ccp ../asset-transfer-basic/chaincode-go/ -ccl go

# Step 3: 添加新的组织（Org${ORG_NUM}）
cd ./addOrg6

# 在三个不同的 demo 脚本中传入 Org${ORG_NUM} 和 PEER_COUNT
./demo.sh $ORG_NUM $PEER_COUNT
cd ./compose
./demo.sh $ORG_NUM $PEER_COUNT
cd ./docker
./demo.sh $ORG_NUM $PEER_COUNT

# 返回根目录
cd ../..
cd ..

# Step 4: 将组织添加到 channel1 中
./addOrgToChannel2.sh 3 3
./addOrgToChannel2.sh 5 6
./addOrgToChannel2.sh $ORG_NUM $PEER_COUNT
./addOrgToChannel2.sh 7 50

# Step 5: 部署链码到新添加的组织节点
./deployChainCode2.sh 3 3
./deployChainCode2.sh 5 6
