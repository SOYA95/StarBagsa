
package winterschoolone;

public class DeliveryCancelled extends AbstractEvent {

    private Long id;
	private Long orderId;
    private String userId;
    private String menuId;
    private Integer qty;
    private String cancelYN;

    
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


	public String getCancelYN() {
		return cancelYN;
	}


	public void setCancelYN(String cancelYN) {
		this.cancelYN = cancelYN;
	}

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
