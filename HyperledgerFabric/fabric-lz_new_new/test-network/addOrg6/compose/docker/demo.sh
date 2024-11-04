#!/bin/bash

# 检查是否提供了两个参数
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <orgX> <number_of_peers>"
    exit 1
fi

# 读取参数
ORG_NUM=$1
PEER_COUNT=$2

# 生成 docker-compose 文件名
COMPOSE_FILE="docker-compose-org${ORG_NUM}.yaml"

# 创建 compose 文件的基本结构
cat > $COMPOSE_FILE << EOF
version: '3.7'

networks:
  test:
    name: fabric_test

services:
EOF

# 生成 peer 节点的配置
for ((i=0; i<PEER_COUNT; i++)); do
  cat >> $COMPOSE_FILE << EOF

  peer${i}.org${ORG_NUM}.example.com:
    container_name: peer${i}.org${ORG_NUM}.example.com
    image: hyperledger/fabric-peer:latest
    labels:
      service: hyperledger-fabric
    environment:
      # Generic peer variables
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_test
    volumes:
      - ./docker/peercfg:/etc/hyperledger/peercfg
      - \${DOCKER_SOCK}/:/host/var/run/docker.sock
EOF
done

echo "docker-compose file $COMPOSE_FILE has been generated for Org${ORG_NUM} with ${PEER_COUNT} peers."

