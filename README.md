# 工程简介——上传功能
#### 样例： localhost:8080/
## 项目说明
#### 功能：文件、视频上传、转码
#### 特点：切片异步、多线程、高效m3u8转码
#### 效率说明： 1G文件切片上传5-10秒，1G视频转码10S左右，视频切片上传20S左右（以上数据全取决于个人带宽和服务器）

## 接口说明
### 普通文件上传
普通文件上传并存储于OSS or MinIO等文件服务器

|接口|请求方式|参数|说明|
|----|----|----|----|
|/uploadFile|POST|file|文件上传|

### 大文件上传-切片
1、前端进行切片后调用uploadSlices进行异步上传至服务器本地

2、上传结束后调用uploadMerge进行文件合并

3、文件合并后，后端调用MinIO（自动切片）or OSS（手动切片）完成上传


|接口|请求方式|参数|说明|
|----|----|----|----|
|/uploadSlice|POST|file、guid、chunk|切片文件+随机id+切片数|
|/uploadMerge|POST or GET|guid、fileName|随机id+文件名|


### 视频上传转m3u8格式
1、前端进行视频整个上传

2、后端接收视频后调用ffmpeg，将文件转为m3u8格式，后存储于本地

3、多线程上传到MinIO or OSS

|接口|请求方式|参数|说明|
|----|----|----|----|
|/uploadVideo|POST|file|视频文件|

### 视频切片上传转m3u8
1、视频文件前端切片调用切片接口接收uploadSlice

2、上传结束后调用视频文件合并接口进行文件合并

3、文件合并结束后调用ffmpeg进行切片

4、且片后多线程上传至MinIO or OSS


|接口|请求方式|参数|说明|
|----|----|----|----|
|/uploadSlice|POST|file、guid、chunk|切片文件+随机id+切片数|
|/uploadVideoMerge|POST|guid、fileName|切片id+视频文件名|



