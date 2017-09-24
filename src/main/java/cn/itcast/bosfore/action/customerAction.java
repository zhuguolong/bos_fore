package cn.itcast.bosfore.action;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

import cn.itcast.bosfore.utils.MailUtils;
import cn.itcast.crm.service.Customer;
import cn.itcast.crm.service.impl.ICustomerService;

@Controller
@Scope("prototype")
@Namespace("/")
@ParentPackage("struts-default")
@Results({@Result(name="registsuccess",type="redirect",location="/signup-success.html"),
    @Result(name="registfail",type="redirect",location="/signup-fail.html"),
    @Result(name="index",type="redirect",location="/index.html"),
    @Result(name="login",type="redirect",location="/login.html")})
public class customerAction extends ActionSupport implements ModelDriven<Customer> {

    private Customer model = new Customer();
    @Override
    public Customer getModel() {
        return model;
    }
    
    @Autowired
    private ICustomerService crmProxy;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    

    /**
     * 给注册用户发送短信验证码
     * @Description TODO
     * @return
     */
    @Action("customerAction_sendMessage")
    public String sendMessage(){
        //随机生成4位验证码
        String checkCode = RandomStringUtils.randomNumeric(4);
        System.out.println(checkCode);
        //调用短信平台发送短信验证码
        String msg = "【传智播客】您本次的验证码为" + checkCode;
   
       /* SmsUtils.sendSmsByWebService(model.getTelephone(), msg);*/
        
        //将验证码保存到session中
        ServletActionContext.getRequest().getSession().setAttribute(model.getTelephone(), checkCode);
        return NONE;
    }
    
    
    private String checkCode;
    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    
    @Action("customerAction_regist")
    public String regist() {
        String realCheckCode = (String) ServletActionContext.getRequest().getSession().getAttribute(model.getTelephone());

        if (StringUtils.isNotBlank(checkCode) && checkCode.equals(realCheckCode)) {
            //TODO 将客户信息  远程提交CRM系统，在CRM中保存客户记录
            try {
                crmProxy.regist(model);
                
                //发送激活邮件
                String emailActiveCode = UUID.randomUUID().toString();
                String content = "【速运快递】尊敬的用户您好！欢迎您注册速运快递，请在24小时内完成激活<br/><a href='" + 
                        MailUtils.activeUrl + "?telephone=" + model.getTelephone() + "&emailActiveCode=" + emailActiveCode + "'>点击激活<a/>";
                //将随机生成的验证码存储在redis中
                redisTemplate.opsForValue().set(model.getTelephone(), emailActiveCode, 24, TimeUnit.HOURS);
                
                MailUtils.sendMail("速运快递-激活账户邮件", content, model.getEmail());
            } catch (Exception e) {
                e.printStackTrace();
                return "registfail";
            }
        }else{
            //给提示，输入验证码为空或者错误
        }
        return "registsuccess";
    }
    
    
    private String emailActiveCode;
    public void setEmailActiveCode(String emailActiveCode) {
        this.emailActiveCode = emailActiveCode;
    }
    /**
     * 客户点击激活邮箱连接激活邮箱，并且获取手机号
     * @Description TODO
     * @return
     * @throws IOException 
     */
    @Action("customerAction_activeMail")
    public String activeMail() throws IOException{
        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("text/html;charset=utf-8");
        
        if(StringUtils.isNotBlank(emailActiveCode)){
           //客户在24小时内的输入的验证码不为空，获取保存在redis中真实的激活码与客户激活码做不叫
            String realEmailActiveCode = redisTemplate.opsForValue().get(model.getTelephone());
            if(StringUtils.isNotBlank(realEmailActiveCode)){
                //激活码存在，即激活码没有过期
                if(emailActiveCode.equals(realEmailActiveCode)){
                    //客户输入的激活码和redis中保存的激活码相等，远程调用crm返回客户信息
                    Customer customer = crmProxy.findByTelephone(model.getTelephone());
                    Integer type = customer.getType();
                    if(type != null){
                        //客户已经注册，不能重复注册
                        response.getWriter().write("对不起，您已经注册过速运快递，请直接登录使用！");
                        return NONE;
                    }else{
                        //客户还未绑定邮箱，想在可以绑定邮箱
                        crmProxy.bind(customer.getId());
                        response.getWriter().write("账户激活成功，请登录后使用");
                        //登录成功后，将redis中的结合电话删除
                        //redisTemplate.delete(model.getTelephone());
                        return "registsuccess";
                    }
                }else{
                    //客户输入的激活码与redis中保存的激活码不相等
                    response.getWriter().write("对不起，您输入的激活码错误！");
                    return NONE;
                }
            }else{
                //24小时后，redis中的激活码已经自动删除
                response.getWriter().write("对不起，您输入的激活码已经过期！");
                return NONE;
            }
        }else{
            //客户输入的激活码为空
            response.getWriter().write("对不起，您输入的激活码为空！");
            return NONE;
        }
    }
    
    
    /**
     * @Description TODO 用户登录
     * @return
     */
    @Action("customerAction_login")
    public String login(){
        Customer customer = crmProxy.findByUsernameAndPassword(model.getUsername(), model.getPassword());
        if(customer != null){
            //用户名和密码输入正确，将客户想你想存入session中
            ServletActionContext.getRequest().getSession().setAttribute("customer", customer);
            return "index";
        }else{
            //用户名或密码输入错误
            return "login";
        }
    }
    
    
    
}
