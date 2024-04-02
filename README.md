# wi_auth_ms
---
## Ports
* 8080 - spring boot application
* 3000 - grafana
* 9090 - prometheus
* 27017 - mongo db
---
## Launch of the project
```
# 1. Clone the repository
git clone https://github.com/igor-IT/wi-auth-ms.git
# 2. Enter the project directory
cd wi-auth-ms
# 3. Launching
docker-compose up -d --build
# 4. Open http://localhost:8080/swagger-ui.html to get
all endpoints
```