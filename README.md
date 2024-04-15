# Distributed-and-decentralized-5G-V2X-communication-Using-HyperLedger-Fabric-Blockchain-Network
A Blockchain based 5G V2X communication platform in which the main entities in 5G V2X communication including the 5G core network, the Public Key Infrastructure, the ITS-Station (vehicles, pedestrians, road side units and other authorities on the road) are integrated in a decentralized and interoperable manner. The blockchain network used is the Hyperledger Fabric supported by the Hyperledger Composer Framework.

###  About the project
- This project is implemented using existing Fabric sample Test network samples from Hyperledger Fabric git repository with my own customization logic
- Created four channels, 10 organizations, a single orderer with each organization having a single peer node
- Developed fast and efficient chaincode written in Java to manage, process, and then store V2X related information on 5G V2X communication in the ledger
- Used the internal CouchDB database for world state which allows for quick data retrieval and operation


# Architecture of proposed System
<p align="center">
<img src="docs/fig1.png" height="300">
</p>

<p align="center">

## 🥇 How to use this repo:

### 🔖 Requirements

-Hyperledger Fabric [ v2.5.4 ]
- Docker and docker-compose
- Java


### 🛠 Run the proposed four channel and 10 Organization HLF model
- Download and install all the requirments mentioned above and replace the modified or customized files in the fabric samples of original hyperledger fabric repo with the ones provided in this repo
- Under the asset-transfer-basic folder replace the chaincode java folder one by one for the chaincodes provided on this repo for enrollment information, authorization information, V2X information, intermediary information and trust list information
- Under the test-network forlder of the orginal HLF repo replace the files and folder provided on this repo as well
  
