FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/eclipse-temurin:21-jdk-alpine

# 直接使用 Alpine 社区仓库的 Node.js

COPY crush.jar /crush.jar

ENTRYPOINT ["java", "-jar", "/crush.jar"]
