package cn.itcast.bosfore.utils;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

public class MailUtils {
	private static String smtp_host = "smtp.126.com"; 
	private static String username = "itcast_java@126.com"; 
	private static String password = "bos123";  //授权码    密码：itcast123

	private static String from = "itcast_java@126.com"; // 发件人，使用当前账户
	//激活邮件中激活地址
	public static String activeUrl = "http://localhost:8084/bos_fore/customerAction_activeMail";

	
	/**
	  * @Description:    发送邮件
	  * @param subject   邮件主题
	  * @param content   内容
	  * @param to        收件人地址
	  * @return void
	 */
	public static void sendMail(String subject, String content, String to) {
		Properties props = new Properties();
		props.setProperty("mail.smtp.host", smtp_host);   
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.smtp.auth", "true");
		Session session = Session.getInstance(props);
		Message message = new MimeMessage(session);
		try {
			message.setFrom(new InternetAddress(from));  //设置发件人
			//设置发送方式：TO：立即发送，  CC：抄送     BCC：密送
			message.setRecipient(RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject);
			message.setContent(content, "text/html;charset=utf-8");
			Transport transport = session.getTransport();
			transport.connect(smtp_host, username, password);
			//发送邮件
			transport.sendMessage(message, message.getAllRecipients());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("邮件发送失败...");
		}
	}

	public static void main(String[] args) {
		sendMail("测试邮件", "你好，传智播客", "itcast_search@163.com");
	}
}
