package com.le.yygh.cmn.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//可以在feignclient那个注解里，加一个path，作为统一的开头路径就行，不需要每个都加
@FeignClient(value = "service-cmn")
@Repository
public interface DictFeignClient {

    //根据dictcode和value查询
    @GetMapping("/admin/cmn/dict/getName/{dictCode}/{value}")//dictcode数据异常
    public String getName(@PathVariable("dictCode") String dictCode, @PathVariable("value") String value);

    //根据value查询
    @GetMapping("/admin/cmn/dict/getName/{value}")
    public String getName(@PathVariable("value") String value);
}
