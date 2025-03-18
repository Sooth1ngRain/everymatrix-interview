# Run the code
Execute run.cmd 
`java -jar bettingstake.jar`

Configuration can be set through appconfig.properties

# Requirement analysis

## HttpServer

### Solution selection

- Use com.sun.net.httpserver.HttpServer as a light-weighted server
- Customize thread-pool with ThreadPoolExecutor and set pool size properly to handle massive simultaneous requests
- Customize dispatch procedure inside http handler to support path variable.
- Implement annotation @Route mimicking spring's @RequestMapping to simplify
  definition of controller route handler.

### Relevant configuration

- server port
- Core pool-size , max pool-size
- Incoming request acquire thread-pool thread timeout
- Response timeout

### Implement Class

com.everymatrix.server.CustomHttpServer

## Session Management

Provide a sliding-refresh session manager

### Solution selection

- Session expire strategy: lazy expire + schedule delete
- Session field: sessionKey , customerId, latestAccessTime(If latestAccessTime + config[timeout] <
  currentTime then session expired)
- data structure: ConcurrentHashMap<sessionKey , SessionObject> for session store

### Configuration Options

- Session timeout duration

### Implement Class

com.everymatrix.service.SessionManager

## Bet-Offer Service

Persist top N stakes of Bet-Offer in memory with thread-safe code, and provide o(1) time-complexity query  

### Post stake solution

- Function define: void placeStake(betofferId , stake, customerId),
- When a stake is added, TreeSet quickly identified as if it is the highest by the customer, if it is ,trigger an action
  to update the **HighStakeCache**.
- Use customerId to lock the procedure and guarantee thread-safe

### Query high stake with HighStakeCache
- ConcurrentHashMap<Integer, ConcurrentSkipListSet<StakeEntry>> Key: betOfferId,Value: stake-customer pair. Store the top 20 stakes distinct by customer
- Insert the stake into TreeSet<StakeEntry> and pop the minimal stake if the customer-stake pair is not already in the cache, if it exists, keep the higher stake and remove the other .


### Implement Class
com.everymatrix.service.BetOfferService

# Performance Testing

## Test Case
200 Thread simulate 100 customerId(customerId = ThreadNumber % 100) , get session and post random stake (1-10000) on one particular bet-offer-id, Loop 10000 times in 10 minutes.

script: ./Test Plan.jmx
## Test Result
![img.png](img.png)


