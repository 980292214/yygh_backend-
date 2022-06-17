package test;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class OssTest {

    public static void main(String[] args) {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";
        String accessKeyId = "";
        String accessKeySecret = "";
        String bucketName = "yygh-osstest01";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建存储空间。
        ossClient.createBucket(bucketName);

        // 关闭OSSClient。
        ossClient.shutdown();
    }
}
