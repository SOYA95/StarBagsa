package winterschoolone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

import winterschoolone.external.Delivery;
import winterschoolone.external.DeliveryService;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String userId;
    private String menuId;
    private Integer qty;
    private String cancelYn;

    @PostPersist
    public void onPostPersist(){
        Payed payed = new Payed();
        BeanUtils.copyProperties(this, payed);
        payed.publishAfterCommit();
        
        try {
                Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
                e.printStackTrace();
        }
    }

//    @PostUpdate
//    public void onPostUpdate(){
//        Refunded refunded = new Refunded();
//        BeanUtils.copyProperties(this, refunded);
//        refunded.publishAfterCommit();
//        
//        //Following code causes dependency to external APIs
//        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
//
//        Delivery delivery = new Delivery();
//        delivery.setOrderId(this.getId());
//        delivery.setMenuId(this.menuId);
//        delivery.setQty(this.getQty());
//        delivery.setUserId(this.getUserId());
//        // mappings goes here
//        PaymentApplication.applicationContext.getBean(DeliveryService.class)
//        .deliveryCancel(delivery);
//
//
//    }

    @PostRemove
    public void onPostRemove(){
        Refunded refunded = new Refunded();
        BeanUtils.copyProperties(this, refunded);
        refunded.publishAfterCommit();
        
        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        Delivery delivery = new Delivery();
        delivery.setOrderId(this.getId());
        delivery.setMenuId(this.menuId);
        delivery.setQty(this.getQty());
        delivery.setUserId(this.getUserId());
        // mappings goes here
        PaymentApplication.applicationContext.getBean(DeliveryService.class)
        .deliveryCancel(delivery);


    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getMenuId() {
        return menuId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getCancelYn() {
        return cancelYn;
    }

    public void setCancelYn(String cancelYn) {
        this.cancelYn = cancelYn;
    }




}
