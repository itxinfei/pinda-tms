# 这里是品达物流 需要的中间件
# mysql
docker run -id --name mysql \
-v /mnt/mysql:/var/lib/mysql \
-p 3306:3306 \
-e MYSQL_ROOT_PASSWORD=123456 mysql:5.7

# canal_mysql
docker run -id --name canal_mysql \
-v /mnt/canal_mysql:/var/lib/mysql \
-p 13306:3306 \
-e MYSQL_ROOT_PASSWORD=123456 mysql:5.7

# nacos
docker run --name nacos -d -p 8848:8848 --privileged=true -e MODE=standalone nacos/nacos-server:1.4.2

# druid
wget https://archive.apache.org/dist/incubator/druid/0.15.0-incubating/apache-druid-0.15.0-incubating-bin.tar.gz

# redis
docker run --restart=always --log-opt max-size=100m --log-opt max-file=2 -p 6379:6379 --name myredis -v /home/redis/myredis/myredis.conf:/etc/redis/redis.conf -v /home/redis/myredis/data:/data -d redis redis-server /etc/redis/redis.conf  --appendonly yes  --requirepass pinda

# zipkin
java -jar zipkin-server-2.12.2-exec.jar --STORAGE_TYPE=mysql --MYSQL_HOST=127.0.0.1 --MYSQL_TCP_PORT=3306 --MYSQL_DB=zipkin --MYSQL_USER=root --MYSQL_PASS=root --RABBIT_ADDRESSES=192.168.1.80:5672 --RABBIT_USER=admin --RABBIT_PASSWORD=admin --RABBIT_VIRTUAL_HOST=/

# seata
docker run -d --name seata -p 8091:8091 seataio/seata-server:latest

# fastdfs
docker run -dti --network=host --name tracker -v /var/fdfs/tracker:/var/fdfs delron/fastdfs tracker
bind_addr=${101.43.184.54}
vim storage.conf
tracker_server=${101.43.184.54}:22122
vim client.conf
tracker_server=${101.43.184.54}:22122
docker run -dti --network=host --name storage -e TRACKER_SERVER=101.43.184.54:22122 -v /var/fdfs/storage:/var/fdfs delron/fastdfs storage

# kafka
docker run -id --name kafka -p 9092:9092 -e KAFKA_BROKER_ID=0 -e KAFKA_ZOOKEEPER_CONNECT=101.43.184.54:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://101.43.184.54:9092 -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 -v /etc/localtime:/etc/localtime wurstmeister/kafka