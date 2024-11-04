/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"crypto/x509"
	"fmt"
	"net"
	"os"
	"path"
	"time"
	"sync"
	"math/big"
	"encoding/hex"
	"crypto/rand"

	fPF "fullPeerFunc"

	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-gateway/pkg/identity"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

func main() {
	// Generate dynamic paths and variables
	mspID,_,certPath, keyPath, tlsCertPath, peerEndpoint,gatewayPeer := fPF.GenerateDynamicPaths(1, 0)

	// Create a gRPC client connection to the Gateway server
	clientConnection := newGrpcConnection( peerEndpoint,tlsCertPath,gatewayPeer)
	defer clientConnection.Close()

	// Create a client identity for this Gateway connection using an X.509 certificate
	id := newIdentity(mspID, certPath)
	// Create a function that generates a digital signature from a message digest using a private key
	sign := newSign(keyPath)

	// Create a Gateway connection for a specific client identity
	gw, err := client.Connect(
		id,
		client.WithSign(sign),
		client.WithClientConnection(clientConnection),
		// Default timeouts for different gRPC calls
		client.WithEvaluateTimeout(5*time.Second),
		client.WithEndorseTimeout(15*time.Second),
		client.WithSubmitTimeout(5*time.Second),
		client.WithCommitStatusTimeout(1*time.Minute),
	)
	if err != nil {
		panic(err)
	}
	defer gw.Close()

	// Override default values for chaincode and channel name as they may differ in testing contexts
	chaincodeName := "basic"
	if ccname := os.Getenv("CHAINCODE_NAME"); ccname != "" {
		chaincodeName = ccname
	}

	channelName := "channel2"
	if cname := os.Getenv("CHANNEL_NAME"); cname != "" {
		channelName = cname
	}

	network := gw.GetNetwork(channelName)
	contract := network.GetContract(chaincodeName)


	
	// 假设同时交易的用户数 x
	x := 50
	i:=0
	for {
	// 随机生成 MDID
		//DDID := generateRandomBigInt(10) // 生成一个最多 10 位数的随机数
		ADID := generateRandomBigInt(10) // 生成一个最多 10 位数的随机数
		hm := generateRandomBigInt(10) // 生成一个最多 10 位数的随机数
		// 随机生成 pk（分为两个大数）
		pk1 := generateRandomBigInt(50)  // 生成一个大数
		pk2 := generateRandomBigInt(50)  // 生成另一个大数
		pk := fmt.Sprintf("%s,%s", pk1, pk2)

		/*// 测试提交交易的 TPS
		tpsUpload := testBlockchainTPS(func() {
			fPF.UploadDeviceInfo(contract, DDID, pk)
		}, x)
		fmt.Printf("Tested TPS for UploadRealUserInfo with %d concurrent users: %.2f TPS\n", x, tpsUpload)	*/
		// 测试提交操作的 TPS
		tpsQuery := testBlockchainTPS(func() {
			fPF.UploadAnonUserInfo(contract, ADID,pk,hm)
		}, x)
		fmt.Printf("Tested TPS for QueryFromRUIT with %d concurrent users: %.2f TPS\n", x, tpsQuery)
		i++
		if i == 10 {
			break
		}
	}
	
		// Start the server and listen on port 7080
	listener, err := net.Listen("tcp", ":9080")
	if err != nil {
		fmt.Println("Error listening:", err)
		return
	}
	// Call ApplyBecomeNotary function (needs implementation)
	fmt.Println("Server listening on :9080")
	
	for {
		// Accept client connections
		conn, err := listener.Accept()
		if err != nil {
			fmt.Println("Error accepting connection:", err)
			continue
		}

		// Start a goroutine to handle the client connection
		go fPF.HandleConnection(conn, contract)
	}
}

func generateRandomBigInt(maxValue int64) string {
	n, _ := rand.Int(rand.Reader, big.NewInt(maxValue))
	return n.String()
}

func generateRandomHex(size int) string {
	bytes := make([]byte, size)
	_, _ = rand.Read(bytes)
	return hex.EncodeToString(bytes)
}

func testBlockchainTPS(operation func(), concurrentUsers int) float64 {
	var wg sync.WaitGroup

	// 用于记录开始和结束时间
	startTime := time.Now()

	// 模拟 concurrentUsers 个并发交易
	for i := 0; i < concurrentUsers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()

			// 执行传入的操作（提交或查询）
			operation()
		}()
	}

	// 等待所有 goroutine 完成
	wg.Wait()

	// 记录结束时间
	endTime := time.Now()
	elapsed := endTime.Sub(startTime).Seconds()

	// 计算并返回 TPS（交易/秒）
	tps := float64(concurrentUsers) / elapsed
	return tps
}

// newGrpcConnection creates a gRPC connection to the Gateway server.
func newGrpcConnection(peerEndpoint string,tlsCertPath string,gatewayPeer string) *grpc.ClientConn {
	certificate, err := loadCertificate(tlsCertPath)
	if err != nil {
		panic(err)
	}

	certPool := x509.NewCertPool()
	certPool.AddCert(certificate)
	transportCredentials := credentials.NewClientTLSFromCert(certPool, gatewayPeer)

	connection, err := grpc.Dial(peerEndpoint, grpc.WithTransportCredentials(transportCredentials))
	if err != nil {
		panic(fmt.Errorf("failed to create gRPC connection: %w", err))
	}

	return connection
}

// newIdentity creates a client identity for this Gateway connection using an X.509 certificate.
func newIdentity(mspID string, certPath string) *identity.X509Identity {
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

// loadCertificate reads and parses an X.509 certificate from a file.
func loadCertificate(filename string) (*x509.Certificate, error) {
	certificatePEM, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file '%s': %w", filename, err)
	}
	return identity.CertificateFromPEM(certificatePEM)
}

// newSign creates a function that generates a digital signature from a message digest using a private key.
func newSign(keyPath string) identity.Sign {
	// Read the private key file from the keystore directory
	files, err := os.ReadDir(keyPath)
	if err != nil {
		panic(fmt.Errorf("failed to read private key directory '%s': %w", keyPath, err))
	}
	if len(files) == 0 {
		panic(fmt.Errorf("no private key file found in directory '%s'", keyPath))
	}
	privateKeyPEM, err := os.ReadFile(path.Join(keyPath, files[0].Name()))
	if err != nil {
		panic(fmt.Errorf("failed to read private key file: %w", err))
	}

	privateKey, err := identity.PrivateKeyFromPEM(privateKeyPEM)
	if err != nil {
		panic(err)
	}

	sign, err := identity.NewPrivateKeySign(privateKey)
	if err != nil {
		panic(err)
	}

	return sign
}

// ApplyBecomeNotary is a placeholder for a function that applies to become a notary.
// You need to implement this function according to your requirements.
func ApplyBecomeNotary(contract *client.Contract) {
	// Implementation goes here
	fmt.Println("Applying to become a notary...")
	// For example, submit a transaction to the ledger
	_, err := contract.SubmitTransaction("BecomeNotary")
	if err != nil {
		fmt.Printf("Failed to apply to become a notary: %v\n", err)
		return
	}
	fmt.Println("Successfully applied to become a notary.")
}

// measureTime 用于多次调用函数并计算平均执行时间
func measureTime(f func(), iterations int) time.Duration {
	var totalTime time.Duration
	for i := 0; i < iterations; i++ {
		start := time.Now()
		f()
		totalTime += time.Since(start)
	}
	return totalTime / time.Duration(iterations)
}

