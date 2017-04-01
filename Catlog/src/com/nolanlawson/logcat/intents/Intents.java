package com.nolanlawson.logcat.intents;

public class Intents {

    // NB. ACTION_LAUNCH and ACTION_SEND_EMAIL must match specifications in the manifest

	public static final String ACTION_LAUNCH = "com.nolanlawson.logcat.intents.LAUNCH";
	public static final String EXTRA_FILTER = "filter";
	public static final String EXTRA_LEVEL = "level";


    public static final String ACTION_SEND_EMAIL = "com.nolanlawson.logcat.intents.SEND_EMAIL";

    // String array of email addresses, at least one must be specified
    public static final String EXTRA_MAIL_RECIPIENTS = "recipients";

    // Service can be "gmail" or "email" or something else that uniquely identifies the service
    public static final String EXTRA_MAIL_SERVICE = "service";

    public static final String EXTRA_MAIL_SUBJECT = "subject";  // String, optional

    public static final String EXTRA_MAIL_DEVICE_INFO = "device_info";  // Boolean

    public static final String EXTRA_MAIL_ATTACHMENT = "attachment";  // Boolean, false = in-body
}
