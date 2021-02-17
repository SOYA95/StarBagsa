
package winterschoolone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Delivery", url="${api.url.Delivery}")
public interface DeliveryService {

    @RequestMapping(method= RequestMethod.DELETE, path="/deliveries")
    public void deliveryCancel(@RequestBody Delivery delivery);

}