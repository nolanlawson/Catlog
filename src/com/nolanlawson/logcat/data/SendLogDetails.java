package com.nolanlawson.logcat.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SendLogDetails {

	private String subject;
	private String body;
	private List<File> attachments = new ArrayList<File>();
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public List<File> getAttachments() {
		return attachments;
	}
	public void addAttachments(File attachment) {
		this.attachments.add(attachment);
	}
}
