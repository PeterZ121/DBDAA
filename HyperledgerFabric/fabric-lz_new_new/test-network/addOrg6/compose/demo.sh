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
COMPOSE_FILE="compose-org${ORG_NUM}.yaml"

# 创建 compose 文件的基本结构
cat > $COMPOSE_FILE << EOF
version: '3.7'

volumes:
EOF

# 生成 volume 部分
for ((i=0; i<PEER_COUNT; i++)); do
    echo "  peer${i}.org${ORG_NUM}.example.com:" >> $COMPOSE_FILE
done

cat >> $COMPOSE_FILE << EOF

networks:
  test:
    name: fabric_test

services:
EOF

# 生成 peer 节点的配置
for ((i=0; i<PEER_COUNT; i++)); do
  PORT1=$((10001 + i * 10 + ORG_NUM * 1000))
  PORT2=$((10002 + i * 10 + ORG_NUM * 1000))

  cat >> $COMPOSE_FILE << EOF

  peer${i}.org${ORG_NUM}.example.com:
    container_name: peer${i}.org${ORG_NUM}.example.com
    image: hyperledger/fabric-peer:latest
    labels:
      service: hyperledger-fabric
    environment:
      - FABRIC_CFG_PATH=/etc/hyperledger/peercfg
      # Generic peer variables
      - CORE_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=true
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      # Peer specific variables
      - CORE_PEER_ID=peer${i}.org${ORG_NUM}.example.com
      - CORE_PEER_ADDRESS=peer${i}.org${ORG_NUM}.example.com:${PORT1}
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_PEER_LISTENADDRESS=0.0.0.0:${PORT1}
      - CORE_PEER_GOSSIP_USELEADERELECTION=false
      - CORE_PEER_GOSSIP_ORGLEADER=false
      - CORE_PEER_GOSSIP_SKIPHANDSHAKE=false
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org1.example.com:7051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer${i}.org${ORG_NUM}.example.com:${PORT1}
      - CORE_PEER_LOCALMSPID=Org${ORG_NUM}MSP
      - CORE_METRICS_PROVIDER=prometheus
    volumes:
      - ../../organizations/peerOrganizations/org${ORG_NUM}.example.com/peers/peer${i}.org${ORG_NUM}.example.com:/etc/hyperledger/fabric
      - peer${i}.org${ORG_NUM}.example.com:/var/hyperledger/production
    working_dir: /opt/gopath/src/github.com/hyperledger/fabric/peer
    command: peer node start
    ports:
      - ${PORT1}:${PORT1}
    networks:
      - test
EOF
done

echo "docker-compose file $COMPOSE_FILE has been generated for Org${ORG_NUM} with ${PEER_COUNT} peers."

