# bitshares_wallet

### Technical info 
1. The core structure of BitShares Wallet reuses the bitshares-core implementation. All communication protocols were developed from scratch. The communication part will be separated as a library in the future.
2. The graphenej part is from smartcoins-wallet, and is used to read the .bin file when logging in with wallet model
3. Now we get the quotes data at fixed time, and haven’t finished developing the protocol using websocket to let the server push the data to the clients.
4. The source contains the code of firebase and fabric, while we don’t submit these two parts. You can add them yourself.
5. fabric.properties -> This file stores fabric’s apiSecret, corresponding io.fabric.ApiKey of manifest
6. google-services.json -> This file stores the info when connecting firebase
 
 
### Product info
#### BitShares Wallet is an app for BitShares on Android devices. The main features are:
 
* **Wallet:**
1. Check your balance
2. Check the transaction records
3. Send your BTS/bitUSD or other properties to others using this wallet.
 
* **Quotes:**
 
1. Check the latest or history price of BTS
 
* **Exchange:**
 
1. Sell/Buy BTS easily
2. Check the SELL ORDER and BUY ORDER
3. Check your open orders
