package main

import (
    "fmt"
    "os"
    "time"
    "crypto/x509"
    "errors"
    "path"

    "github.com/hyperledger/fabric-gateway/pkg/client"
    "github.com/hyperledger/fabric-gateway/pkg/identity"
    "google.golang.org/grpc"
    "google.golang.org/grpc/credentials"
)

const (
    mspID        = "Org6MSP" // 轻节点的MSP ID
    cryptoPath   = "../../test-network/organizations/peerOrganizations/org6.example.com"
    certPath     = cryptoPath + "/users/User1@org6.example.com/msp/signcerts/User1@org6.example.com-cert.pem"
    keyPath      = cryptoPath + "/users/User1@org6.example.com/msp/keystore/"
    tlsCertPath  = cryptoPath + "/peers/peer0.org6.example.com/tls/ca.crt"
    peerEndpoint = "localhost:9051" // 全功能节点的地址
    gatewayPeer  = "peer0.org2.example.com"
)

func main() {
    // 创建轻节点的 gRPC 连接，连接到全功能节点
    clientConnection := newGrpcConnection()
    defer clientConnection.Close()

    // 使用轻节点的身份和签名
    id := newIdentity()
    sign := newSign()

    // 创建网关连接
    gw, err := client.Connect(
        id,
        client.WithSign(sign),
        client.WithClientConnection(clientConnection),
        client.WithEvaluateTimeout(5*time.Second),
        client.WithEndorseTimeout(15*time.Second),
        client.WithSubmitTimeout(5*time.Second),
        client.WithCommitStatusTimeout(1*time.Minute),
    )
    if err != nil {
        panic(err)
    }
    defer gw.Close()

    // 使用轻节点身份查询链码
    network := gw.GetNetwork("channel2")
    contract := network.GetContract("basic")

    result, err := contract.EvaluateTransaction("GetAllAssets")
    if err != nil {
        panic(fmt.Errorf("Failed to evaluate transaction: %w", err))
    }

    fmt.Printf("Query Result: %s\n", string(result))
}

// 创建 gRPC 连接函数
func newGrpcConnection() *grpc.ClientConn {
    cert, err := os.ReadFile(tlsCertPath)
    if err != nil {
        panic(fmt.Errorf("Failed to read TLS certificate: %w", err))
    }
    certPool := x509.NewCertPool()
    if !certPool.AppendCertsFromPEM(cert) {
        panic(errors.New("Failed to add certificate to pool"))
    }

    creds := credentials.NewClientTLSFromCert(certPool, gatewayPeer)

    connection, err := grpc.Dial(peerEndpoint, grpc.WithTransportCredentials(creds))
    if err != nil {
        panic(fmt.Errorf("Failed to create gRPC connection: %w", err))
    }

    return connection
}

// 使用轻节点身份
func newIdentity() *identity.X509Identity {
	certificate, err := loadCertificate(certPath)
	if err != nil {
		panic(err)
	}

	id, err := identity.NewX509Identity(mspID, certificate)
	if err != nil {
		panic(err)
	}

	return id
}

func loadCertificate(filename string) (*x509.Certificate, error) {
	certificatePEM, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file: %w", err)
	}
	return identity.CertificateFromPEM(certificatePEM)
}


// 使用轻节点的签名
func newSign() identity.Sign {
    files, err := os.ReadDir(keyPath)
    if err != nil {
        panic(fmt.Errorf("Failed to read private key directory: %w", err))
    }
    keyFile := path.Join(keyPath, files[0].Name())
    privateKeyPEM, err := os.ReadFile(keyFile)
    if err != nil {
        panic(fmt.Errorf("Failed to read private key: %w", err))
    }

    privateKey, err := identity.PrivateKeyFromPEM(privateKeyPEM)
    if err != nil {
        panic(fmt.Errorf("Failed to parse private key: %w", err))
    }

    sign, err := identity.NewPrivateKeySign(privateKey)
    if err != nil {
        panic(fmt.Errorf("Failed to create sign function: %w", err))
    }

    return sign
}

