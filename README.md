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
![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004252_e6abd617_7713259.png "前台功能模块png.png")

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004322_186acba5_7713259.png "后台功能模块.png")

### 前台首页

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004337_1bb68a94_7713259.png "1003.png")

前台首页的上半部分 ↑ 和下半部分  ↓ 
![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004403_ad74ac7e_7713259.png "1004.png")

### **后台首页**

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004421_5ed0861c_7713259.png "1005.png")

### 数据库总览

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004452_c0aa60d2_7713259.png "1006.png")

### **业务流程图**

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004510_2a7523b2_7713259.png "1002.png")

### 手机号登录

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004528_e18f54ac_7713259.png "1014.png")

### 微信登录

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004626_5f9b8395_7713259.png "1010.png")

### 查看科室排班

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004646_464516a5_7713259.png "1023.png")

### 预约下单

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/004721_28731f11_7713259.png "1024.png")

### 微信支付

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005017_f7bb1cc6_7713259.png "1026.png")

### 支付成功的短信提示

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005047_0f9fe992_7713259.png "1027.png")

### 取消预约

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005108_1ba11d1b_7713259.png "1028.png")

### 取消预约成功，发送短信提醒并退款

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005126_2595c085_7713259.png "1029.png")

### 预约统计

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005140_c9bb7958_7713259.png "1032.png")

### 就医提醒

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005150_5e1fda16_7713259.png "1034.png")

### 更新了的内容：

相对于最初版本，更新了：

1. **医院锁定/取消锁定功能：**被锁定的医院无法上传医院的相关信息，如科室、排班信息。
2. **医院上线/下线功能：**若医院处于下线状态，则该医院无法在前台系统被用户看到或查询到。
3. **优化了短信发送的时机：**改为付款或退款成功后才发送短信。
4. **锁定/取消锁定用户功能：**用户被锁定后无法在前台系统登录。

![输入图片说明](https://images.gitee.com/uploads/images/2022/0618/005201_f6e14bd9_7713259.png "该用户已被锁定.png")