package com.beifeng.etl.util;

import com.beifeng.etl.util.UserAgentUtil.UserAgentInfo;

public class TestUserAgentUtil {
	public static void main(String[] args) {
		String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36";
		userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; GWX:QUALIFIED; rv:11.0) like Gecko";
		userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36";
		UserAgentInfo info = UserAgentUtil.analyticUserAgent(userAgent);
		System.out.println(info);
	}
}
