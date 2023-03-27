#  学习备注


###  todo list
- auth2
- 限流配置研究
- 流量监控显示空
- flowable 流程研究
- oss 研究

### docker 命令
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



docker exec -it laokou-gateway tail -f ./app.log
docker exec -it laokou-auth tail -f ./app.log
docker exec -it laokou-monitor tail -f ./app.log
docker exec -it laokou-admin tail -f ./app.log
docker exec -it laokou-flowable tail -f ./app.log