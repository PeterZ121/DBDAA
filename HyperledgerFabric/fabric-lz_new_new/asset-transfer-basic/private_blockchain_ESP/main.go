/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"crypto/x509"
	"fmt"
	"os"
	"path"
	"time"
	"sync"
	"math/big"
	"encoding/hex"
	"crypto/rand"
	"github.com/gin-gonic/gin"
	"net/http"

	fPF "fullPeerFunc"

	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-gateway/pkg/identity"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	

)


// Struct for request payloads
type UploadDeviceInfoRequest struct {
	DDID string `json:"DDID"`
	Pk   string `json:"Pk"`
}

type QueryFromDITRequest struct {
	DDID string `json:"DDID"`
}

type UploadRealUserInfoRequest struct {
	MDID       string `json:"MDID"`
	Pk         string `json:"Pk"`
	MerkleRoot string `json:"merkleRoot"`
}

type QueryFromRUITRequest struct {
	MDID string `json:"MDID"`
}

type UploadAnonUserInfoRequest struct {
	ADID string `json:"ADID"`
	Pk   string `json:"Pk"`
	HM   string `json:"hM"`
}

type QueryFromAUITRequest struct {
	ADID string `json:"ADID"`
}

func main() {
	contract1 := getContract(1,0,"channel1")
	contract2 := getContract(1,0,"channel2")
	// Initialize the Gin router
	router := gin.Default()

	// Define routes for each function
	router.POST("/consortium/uploadDeviceInfo", func(c *gin.Context) {
		var req UploadDeviceInfoRequest
		if err := c.BindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		fmt.Println(req.DDID)
		result := fPF.UploadDeviceInfo(contract2, req.DDID, req.Pk)
		c.JSON(http.StatusOK, gin.H{"result": result})
		fmt.Println(result)
	})

	router.POST("/consortium/queryFromDIT", func(c *gin.Context) {
		var req QueryFromDITRequest
		if err := c.BindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		pk := fPF.QueryFromDIT(contract2, req.DDID)
		c.JSON(http.StatusOK, gin.H{"result": pk})
		fmt.Println(pk)
	})

	router.POST("/private/uploadRealUserInfo", func(c *gin.Context) {
		var req UploadRealUserInfoRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		result := fPF.UploadRealUserInfo(contract1, req.MDID, req.Pk, req.MerkleRoot)
		c.JSON(http.StatusOK, gin.H{"result": result})
		fmt.Println(result)
	})

	router.POST("/private/queryFromRUIT", func(c *gin.Context) {
		var req QueryFromRUITRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		result := fPF.QueryFromRUIT(contract1, req.MDID)
		c.JSON(http.StatusOK, gin.H{"result": result})
		fmt.Println(result)
	})

	router.POST("/consortium/uploadAnonUserInfo", func(c *gin.Context) {
		var req UploadAnonUserInfoRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		result := fPF.UploadAnonUserInfo(contract2, req.ADID, req.Pk, req.HM)
		fmt.Println(result)
		c.JSON(http.StatusOK, gin.H{"result": result})
	})

	router.POST("/consortium/queryFromAUIT", func(c *gin.Context) {
		var req QueryFromAUITRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		fmt.Println(req)
		result := fPF.QueryFromAUIT(contract2, req.ADID)
		c.JSON(http.StatusOK, gin.H{"result": result})
	})

	// Start the server on port 7080
	err := router.Run(":7080")
	if err != nil {
		fmt.Println("Error starting server:", err)
	}
}

func getContract(org int,peer int, channelName string) *client.Contract {
	// Generate dynamic paths and variables
	mspID, _, certPath, keyPath, tlsCertPath, peerEndpoint, gatewayPeer := fPF.GenerateDynamicPaths(org, peer)

	// Create a gRPC client connection to the Gateway server
	clientConnection := newGrpcConnection(peerEndpoint, tlsCertPath, gatewayPeer)
//	defer clientConnection.Close()

	// Create a client identity for this Gateway connection using an X.509 certificate
	id := newIdentity(mspID, certPath)
	sign := newSign(keyPath)

	// Create a Gateway connection for a specific client identity
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
//	defer gw.Close()

	// Get the contract from the network
	chaincodeName := "basic"
	network := gw.GetNetwork(channelName)
	contract := network.GetContract(chaincodeName)
	return contract
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



/*
// testBlockchainTPS 测试区块链的 TPS
// 接受一个操作函数（如提交或查询操作）作为参数
func testBlockchainTPS(operation func(), concurrentUsers int) float64 {
	var wg sync.WaitGroup
	start := time.Now()

	// 模拟 concurrentUsers 个并发交易
	for i := 0; i < concurrentUsers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			// 执行传入的操作（提交或查询）
			operation()
		}()
	}

	// 等待所有操作完成
	wg.Wait()

	// 计算 TPS
	elapsed := time.Since(start).Seconds()
	tps := float64(concurrentUsers) / elapsed
	return tps
}
*/

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

