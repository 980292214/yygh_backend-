//package com.le.yygh.msm.utils;
//
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ConstantPropertiesUtils implements InitializingBean {//当容器启动时
//    @Value("${tencent.sms.sdkAppId}")
//    private String sdkAppId;
//    @Value("${tencent.sms.signName}")
//    private String signName;
//    @Value("${tencent.sms.templateId.login}")
//    private String login;
//    @Value("${tencent.sms.templateId.payment}")
//    private String payment;
//    @Value("${tencent.sms.templateId.refund}")
//    private String refund;
//    @Value("${tencent.sms.templateId.remind}")
//    private String remind;
//    @Value("${tencent.sms.secretID}")
//    private String secretID;
//    @Value("${tencent.sms.secretKey}")
//    private String secretKey;
//
//    public static String SDK_APP_ID;
//    public static String SIGN_NAME;
//    public static String LOGIN;
//    public static String PAYMENT;
//    public static String REFUND;
//    public static String REMIND;
//    public static String SECRET_ID;
//    public static String SECRET_KEY;
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        SDK_APP_ID = sdkAppId;
//        SIGN_NAME = signName;
//        LOGIN = login;
//        PAYMENT = payment;
//        REFUND = refund;
//        REMIND = remind;
//        SECRET_ID = secretID;
//        SECRET_KEY = secretKey;
//
//    }
//}
