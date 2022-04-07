package com.fy.DataCollectionsUtils;

import java.util.ArrayList;
import java.util.List;


public class ParsedRespond {

	//响应body里的内容
	private String code;
	//deviceStatusList里有多个对象
	private List<ContentInfo> deviceStatusList = new ArrayList<ContentInfo>();
	private String desc;
	
	public ParsedRespond() {}
	public ParsedRespond(String code, List<ContentInfo> deviceStatusList, String desc) {
		this.code = code;
		this.deviceStatusList = deviceStatusList;
		this.desc = desc;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public List<ContentInfo> getDeviceStatusList() {
		return deviceStatusList;
	}
	
	public void setDeviceStatusList(List<ContentInfo> deviceStatusList) {
		this.deviceStatusList = deviceStatusList;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	@Override
	public String toString() {
		return "ParsedRespond\n"
				+ "[code=" + code + ",\n"
					+ "status_list=" + deviceStatusList + ",\n"
					+ "desc=" + desc + "]\n";
	}
}
