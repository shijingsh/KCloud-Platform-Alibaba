#  学习备注


###  todo list
- auth2
- 限流配置研究
- 流量监控显示空
- flowable 流程研究
- oss 研究
- smartsql


###  打包
mvn clean install -DskipTests
mvn package -Pdev
mvn package -Ptest
### docker 命令

docker stop laokou-gateway
docker start laokou-gateway
docker restart laokou-gateway

docker kill $(docker ps -a -q) 杀死所有实例
docker rm $(docker ps -a -q)   删除所有实例

docker images –q # 查看所用镜像的 id
docker rmi 镜像 id # 删除指定本地镜像
docker rmi `docker images -q` # 删除所有本地镜像

docker start laokou-gateway
docker start laokou-auth
docker start laokou-monitor
docker start laokou-admin
docker start laokou-flowable


docker exec -it laokou-gateway /bin/bash

docker logs -f laokou-gateway

docker exec -it laokou-gateway tail -f ./app.log
docker exec -it laokou-auth tail -f ./app.log
docker exec -it laokou-monitor tail -f ./app.log
docker exec -it laokou-admin tail -f ./app.log
docker exec -it laokou-flowable tail -f ./app.log


# 网络



docker network create example_default
docker network connect example_default laokou-gateway
docker network connect example_default laokou-monitor
docker network connect example_default laokou-auth
docker network connect example_default laokou-admin
docker network connect example_default laokou-flowable

docker network ls 或 docker network list  查看docker网络列表
docker network inspect example_default 显示一个或多个网络上的详细信息


### 构建、运行容器

docker build -t='laokou-gateway' .
docker build -t laokou-gateway:1.1 .

docker run  -it --name=laokou-gateway   laokou-gateway:1.1  /bin/bash
docker run -it -d --name=laokou-gateway -p 5555:5555    laokou-gateway:1.1
docker run -it -d --name=laokou-gateway --net=host   laokou-gateway:1.1
docker run -it --name=laokou-gateway -p 5555:5555 -v /docker/KCloud-Platform-Alibaba/laokou-cloud/laokou-gateway/docker:/app.log   laokou-gateway:1.1


docker start laokou-gateway
docker exec -it laokou-gateway /bin/bash

docker cp 2447319091fe:/app.log /opt
docker cp 2447319091fe:/app.log /docker/KCloud-Platform-Alibaba/laokou-cloud/laokou-gateway/docker

docker start laokou-auth
docker build -t laokou-auth:1.1 .
docker run -it --name=laokou-auth -p 1111:1111 laokou-auth:1.1
docker run -it -d --name=laokou-auth -p 1111:1111 laokou-auth:1.1  守护进程
docker run -it -d --name=laokou-auth --net=host laokou-auth:1.1  
docker run -it --name=laokou-auth -p 1111:1111 -v /docker/KCloud-Platform-Alibaba/laokou-service/laokou-auth/laokou-auth-server/docker:/user   laokou-auth:1.1   
docker cp 792dd791ee72:/app.log /docker/KCloud-Platform-Alibaba/laokou-service/laokou-auth/laokou-auth-server/docker

docker build -t laokou-admin:1.1 .
docker run -it  -d --name=laokou-admin -p 9999:9999 laokou-admin:1.1
docker run -it  -d --name=laokou-admin --net=host laokou-admin:1.1
docker exec -it laokou-admin tail -f ./app.log

docker build -t laokou-monitor:1.1 .
docker run -it  -d --name=laokou-monitor -p 5000:5000 laokou-monitor:1.1
docker run -it  -d --name=laokou-monitor --net=host laokou-monitor:1.1
docker exec -it laokou-monitor tail -f ./app.log

docker build -t='laokou-flowable' .
docker build -t laokou-flowable:1.1 .

docker run  -it --name=laokou-flowable   laokou-flowable:1.1  /bin/bash
docker run -it -d --name=laokou-flowable -p 10005:10005    laokou-flowable:1.1
docker run -it -d --name=laokou-flowable --net=host    laokou-flowable:1.1
docker run -it --name=laokou-flowable -p 10005:10005 -v /docker/KCloud-Platform-Alibaba/laokou-cloud/laokou-flowable/docker:/app.log   laokou-flowable:1.1


### 网络模式

1、host 模式
#创建容器web 1，指定网络模式为 host
#因为是host模式，所有宿主机和容器共享ip和端口
docker run -d --name web1 --net=host nginx


2、Container模式
这个模式指定新创建的容器和已经存在的一个容器共享一个Network Namespace，而不是和宿主机共享。

docker run -itd --name test1 centos:7 /bin/bash
docker run -itd --name test2 --net=container:test1 centos:7 /bin/bash

3、none模式

使用none 模式，docker 容器有自己的network Namespace ，但是并不为Docker 容器进行任何网络配置。也就是说，这个Docker 容器没有网卡，ip， 路由等信息。
这种类型没有办法联网，但是封闭的网络能很好的保证容器的安全性
特殊情况下才会用到，一般不用

4、bridge模式
bridge模式是docker的默认网络模式，不写 – net参数，就是bridge模式。

器使用独立network Namespace，并连接到docker0虚拟网卡。通过docker0网桥以及iptables nat表配置与宿主机通信，
此模式会为每一个容器分配Network Namespace、设置IP等，并将一个主机上的 Docker 容器连接到一个虚拟网桥上

使用 docker run -p 时，docker实际是在iptables做了DNAT规则，实现端口转发功能。可以使用iptables -t nat -vnL 查看。