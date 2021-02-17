package winterschoolone;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface DeliveryRepository extends PagingAndSortingRepository<Delivery, Long>{

	List<Delivery> findByOrderId(Long orderId);
}