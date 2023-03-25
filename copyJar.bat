

@echo off 
        md "distJar/laokou-admin-server"
        md "distJar/laokou-auth-server"
        md "distJar/laokou-gateway"
        md "distJar/laokou-monitor"
        md "distJar/laokou-flowable"

copy laokou-cloud\laokou-gateway\target\laokou-gateway.jar distJar\laokou-gateway\
copy laokou-cloud\laokou-monitor\target\laokou-monitor.jar distJar\laokou-monitor\
copy laokou-service\laokou-auth\laokou-auth-server\target\laokou-auth-server.jar distJar\laokou-auth-server\
copy laokou-service\laokou-admin\laokou-admin-server\target\laokou-admin-server.jar distJar\laokou-admin-server\
copy laokou-service\laokou-module\laokou-flowable\laokou-flowable-server\target\laokou-flowable-server.jar distJar\laokou-flowable\

pause