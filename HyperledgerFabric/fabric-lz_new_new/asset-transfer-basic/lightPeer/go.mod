module assetTransfer

go 1.21.1

toolchain go1.21.3

require (
	github.com/hyperledger/fabric-gateway v1.2.2
	github.com/hyperledger/fabric-protos-go-apiv2 v0.2.0
	google.golang.org/grpc v1.53.0
	notaryFunc v0.0.0
)

require (
	TC v0.0.0 // indirect
	github.com/golang/protobuf v1.5.2 // indirect
	github.com/miekg/pkcs11 v1.1.1 // indirect
	github.com/stretchr/testify v1.8.4 // indirect
	golang.org/x/net v0.17.0 // indirect
	golang.org/x/sys v0.13.0 // indirect
	golang.org/x/text v0.13.0 // indirect
	google.golang.org/genproto v0.0.0-20230216225411-c8e22ba71e44 // indirect
	google.golang.org/protobuf v1.31.0 // indirect
)

replace notaryFunc => ../notaryFunc

replace TC => ../package/TC
