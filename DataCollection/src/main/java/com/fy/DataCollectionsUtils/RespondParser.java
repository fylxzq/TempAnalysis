package com.fy.DataCollectionsUtils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;


public class RespondParser {
	
	public static ParsedRespond parse(String respond) {
		String respond_body = StringUtils.substringAfter(respond, "body=");  // 提取响应报文体
		return (JSON.parseObject(respond_body, ParsedRespond.class));  // 将JSON解析成Java对象
	}
}
