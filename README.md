### 网上预约挂号系统：

**使用技术**：SpringBoot、Spring Cloud、Mybatis-Plus、MySQL、Redis、MongoDB、RabbitMQ、Docker、Swagger2、Vue、Element-UI、Axios等

**项目描述**：基于微服务架构开发，该项目可以和多家医院建立合作关系，间接使医院对外开放网络预约挂号服务，让群众随时随地都能预约挂号。本系统共计有9个微服务模块，需要相互调用的微服务都注册在 Spring Cloud Alibaba Nacos 中，配合 Spring Cloud Feign 和Spring Cloud Gateway 就可以使不同的微服务模块之间跨域调用，从而实现系统功能。

**项目亮点**：

- 使用了当前流行的NoSQL技术，使用Redis缓存数据，使用MongoDB实现高并发读写

- 系统支持手机号登录和微信登录

- 整合了腾讯云短信服务，用户在支付、退款成功后都会有相应的短信提醒

- 使用阿里云OSS实现文件上传功能，还整合了定时任务，配合短信服务实现了就医提醒功能

- 整合微信登录、微信支付，还增加了微信退款功能

- 整合消息中间件RabbitMQ提高订单的并发量

  

### 系统功能模块

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/003226_0c52c778_7713259.png "image-20220617233056669.png")

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/003245_8f58a363_7713259.png "image-20220617233112347.png")

### 前台首页

![1003](E:\学习\网上开源项目-论文\尚医通\README.assets\1003.png)

前台首页的上半部分 ↑ 和下半部分  ↓ ![1004](E:\学习\网上开源项目-论文\尚医通\README.assets\1004.png)

### **后台首页**

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234156809.png" alt="image-20220617234156809" style="zoom: 67%;" />

### 数据库总览

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234231579.png" alt="image-20220617234231579" style="zoom:67%;" />

### **业务流程图**

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\1002.png" alt="1002" style="zoom: 67%;" />

### 手机号登录

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234430469.png" alt="image-20220617234430469" style="zoom:50%;" />

### 微信登录

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234450222.png" alt="image-20220617234450222" style="zoom: 60%;" />

### 查看科室排班

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234554677.png" alt="image-20220617234554677" style="zoom: 67%;" />

### 预约下单

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234647141.png" alt="image-20220617234647141" style="zoom:67%;" />

### 微信支付

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234712286.png" alt="image-20220617234712286" style="zoom:67%;" />

### 支付成功的短信提示

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234743413.png" alt="image-20220617234743413" style="zoom:67%;" />

### 取消预约

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234816852.png" alt="image-20220617234816852" style="zoom:67%;" />

### 取消预约成功，发送短信提醒并退款

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617234855700.png" alt="image-20220617234855700" style="zoom:67%;" />

### 预约统计

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220617235006319.png" alt="image-20220617235006319" style="zoom: 67%;" />

### 就医提醒

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220618000059182.png" alt="image-20220618000059182" style="zoom:67%;" />

### 更新了的内容：

相对于最初版本，更新了：

1. **医院锁定/取消锁定功能：**被锁定的医院无法上传医院的相关信息，如科室、排班信息。
2. **医院上线/下线功能：**若医院处于下线状态，则该医院无法在前台系统被用户看到或查询到。
3. **优化了短信发送的时机：**改为付款或退款成功后才发送短信。
4. **锁定/取消锁定用户功能：**用户被锁定后无法在前台系统登录。

<img src="E:\学习\网上开源项目-论文\尚医通\README.assets\image-20220618000810487.png" alt="image-20220618000810487" style="zoom:67%;" />