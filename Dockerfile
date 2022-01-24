#指定基础镜像，在其上进行定制
FROM java:8

#维护者信息
MAINTAINER wangshilei <sirwsl@163.com>
VOLUME /tmp

COPY target/uploadWheel-1.jar upload-wheel.jar
COPY ffmpeg-linux/ffmpeg  /ffmpeg
RUN bash -c "touch /upload-wheel.jar" &&\
  cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
  echo "Asia/Shanghai" >> /etc/timezone
EXPOSE 8080

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/upload-wheel.jar" ]
