package winterschoolone;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import winterschoolone.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    
    @Autowired
    DeliveryRepository deliverRepository;
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayed_(@Payload Payed payed){

    	if(payed.isMe()){
            System.out.println("##### listener  : " + payed.toJson());
            
            Delivery delivery = new Delivery();
            delivery.setMenuId(payed.getMenuId());
            delivery.setOrderId(payed.getOrderId());
            delivery.setQty(payed.getQty());
            delivery.setUserId(payed.getUserId());
            
            deliverRepository.save(delivery);
        }
    }

}
