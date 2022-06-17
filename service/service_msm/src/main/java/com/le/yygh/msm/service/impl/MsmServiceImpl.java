package com.le.yygh.msm.service.impl;

import com.alibaba.fastjson.JSONObject;

import com.le.yygh.msm.service.MsmService;
//import com.le.yygh.msm.utils.ConstantPropertiesUtils;
import com.le.yygh.vo.msm.MsmVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
//导入可选配置类
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;

// 导入对应SMS模块的client
import com.tencentcloudapi.sms.v20210111.SmsClient;

// 导入要请求接口对应的request response类
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {
    @Override
    public boolean send(String phone, String code) {//手机号登录的验证码
        //判断手机号是否为空
        if (StringUtils.isEmpty(phone)) {
            System.out.println("手机号为空，登录验证码发送失败！");
            return false;
        }
        //整合腾讯云短信服务
        try {
            /* 实例化一个认证对象，入参需要传入腾讯云账户密钥对secretId，secretKey。
             * SecretId、SecretKey 查询: https://console.cloud.tencent.com/cam/capi */
            Credential cred = new Credential("", "");
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setSignMethod("HmacSHA256");
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, "ap-guangzhou",clientProfile);
            SendSmsRequest req = new SendSmsRequest();
            // 应用 ID 可前往 [短信控制台](https://console.cloud.tencent.com/smsv2/app-manage) 查看
            String sdkAppId = "";
            req.setSmsSdkAppId(sdkAppId);
            // 签名信息可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-sign)
            String signName = "";
            req.setSignName(signName);
            // 模板 ID 可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-template)
            String templateId = "";
            req.setTemplateId(templateId);
            /* 模板参数: 模板参数的个数需要与 TemplateId 对应模板的变量个数保持一致，若无模板参数，则设置为空 */
            String[] templateParamSet = {code};
            req.setTemplateParamSet(templateParamSet);
            /* 下发手机号码，采用 E.164 标准，+[国家或地区码][手机号]
             * 示例如：+8613711112222， 其中前面有一个+号 ，86为国家码，13711112222为手机号，最多不要超过200个手机号 */
            String[] phoneNumberSet = {"+86"+phone};
            req.setPhoneNumberSet(phoneNumberSet);
            String sessionContext = "";
            req.setSessionContext(sessionContext);
            String extendCode = "";
            req.setExtendCode(extendCode);
            String senderid = "";
            req.setSenderId(senderid);
            SendSmsResponse res = client.SendSms(req);
            // 输出json格式的字符串回包
            System.out.println("Msm模块发送登录验证码短信"+SendSmsResponse.toJsonString(res));
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
        return true;//false？？？ todo
    }

    @Override
    //mq发送短信封装   无具体验证码
    public boolean send(MsmVo msmVo) {
        if(StringUtils.isEmpty(msmVo.getPhone())) {
            return false;
        }
        try {        //整合腾讯云短信服务
            /* 实例化一个认证对象，入参需要传入腾讯云账户密钥对secretId，secretKey。
             * SecretId、SecretKey 查询: https://console.cloud.tencent.com/cam/capi */
            Credential cred = new Credential("", "");
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setReqMethod("POST");
            httpProfile.setConnTimeout(60);
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setSignMethod("HmacSHA256");
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(cred, "ap-guangzhou",clientProfile);
            SendSmsRequest req = new SendSmsRequest();
            // 应用 ID 可前往 [短信控制台](https://console.cloud.tencent.com/smsv2/app-manage) 查看
            String sdkAppId = "";
            req.setSmsSdkAppId(sdkAppId);
            // 签名信息可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-sign)
            String signName = "";
            req.setSignName(signName);
            // 模板 ID 可前往 [国内短信](https://console.cloud.tencent.com/smsv2/csms-template)
            //String templateId = "1372409";//改为动态获取
            String templateId2 = msmVo.getTemplateCode();
            req.setTemplateId(templateId2);
            /* 模板参数: 模板参数的个数需要与 TemplateId 对应模板的变量个数保持一致，若无模板参数，则设置为空 */
            String[] templateParamSet = {};//-----------具体订单信息参数msmVo.getParam().get(key)
            req.setTemplateParamSet(templateParamSet);
            /* 下发手机号码，采用 E.164 标准，+[国家或地区码][手机号]
             * 示例如：+8613711112222， 其中前面有一个+号 ，86为国家码，13711112222为手机号，最多不要超过200个手机号 */
            String[] phoneNumberSet = {"+86"+msmVo.getPhone()};//得到电话--
            req.setPhoneNumberSet(phoneNumberSet);
            String sessionContext = "";
            req.setSessionContext(sessionContext);
            String extendCode = "";
            req.setExtendCode(extendCode);
            String senderid = "";
            req.setSenderId(senderid);
            SendSmsResponse res = client.SendSms(req);
            // 输出json格式的字符串回包
            System.out.println("Msm模块监听到 RabbitMQ 队列有内容，发送短信"+"短信模板："+msmVo.getTemplateCode()+SendSmsResponse.toJsonString(res));
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
        return true;//false?
    }

}
