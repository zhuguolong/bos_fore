package cn.itcast.bosfore.action;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

import cn.itcast.bos.entity.bsae.Area;
import cn.itcast.bos.entity.take_delivery.Order;
import cn.itcast.bos.service.take_delivery.IOrderService;
import cn.itcast.crm.service.Customer;

@Controller
@Scope("prototype")
@Namespace("/")
@ParentPackage("struts-default")
@Results(@Result(name="success", type="redirect", location="/success.html"))
public class orderAction extends ActionSupport implements ModelDriven<Order> {

    private Order order = new Order();
    @Override
    public Order getModel() {
        return order;
    }
    
    @Autowired
    private IOrderService orderProxy;
    
    /**
     * 接收收件人，发件人区域字符串信息     如：河北省/石家庄市/桥东区
     */
    private String sendAreaInfo;
    private String recAreaInfo;
    
    public void setSendAreaInfo(String sendAreaInfo) {
        this.sendAreaInfo = sendAreaInfo;
    }
    
    public void setRecAreaInfo(String recAreaInfo) {
        this.recAreaInfo = recAreaInfo;
    }

    /**
     * 保存客户预定快递是输入的信息
     * @Description TODO
     * @return
     */
    @Action("rderAction_save")
    public String save(){
        //设置发件人的省市区信息
        if(StringUtils.isNoneBlank(sendAreaInfo)){
            String[] str = sendAreaInfo.split("/");
            String province = str[0];   //省
            String city = str[1];       //市
            String district = str[2];   //区
            Area sendArea = new Area(province, city, district);
            order.setSendArea(sendArea);
        }
        
        //设置收件人的省市区信息
        if(StringUtils.isNotBlank(recAreaInfo)){
            String[] str = recAreaInfo.split("/");
            String province = str[0];
            String city = str[1];
            String district = str[2];
            Area recArea = new Area(province, city, district);
            order.setRecArea(recArea);
        }
        
        /**
         * 用户登录时将customer存入session中了，现在将用户信息取出来，是为了获得用户id值
         * 如果没有登录，页可以预定快递员上门收件
         */
        Customer customer = (Customer) ServletActionContext.getRequest().getSession().getAttribute("customer");
        if(customer != null){
            order.setCustomer_id(customer.getId());
        }
        
        //调用bos后台管理系统，保存用户预定订单信息
        orderProxy.save(order);
        
        return SUCCESS;
    }

}
