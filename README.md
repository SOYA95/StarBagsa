# sirenorder
# 서비스 시나리오
### 기능적 요구사항
1. 고객이 음료를 주문한다.
2. 고객이 결제를 한다.
3. 결제가 완료되면 주문내역을 매장으로 할당하고 배송시작한다. 
4. 고객이 주문을 취소할 수 있다.
5. 고객이 중간중간 주문상태를 조회한다.


### 비기능적 요구사항
1. 트랜잭션
    1. 주문시 배송도 되어야 한다. → Sync 호출
1. 장애격리
    1. 결제시스템에서 장애가 발생해도 주문은 받을 수 있어야한다 → Async (event-driven), Eventual Consistency
    1. 주문량이 많아 결재시스템 과중되면 잠시 주문을 받지 않고 잠시후에 하도록 유도한다 → Circuit breaker, fallback
1. 성능
    1. 고객이 주문상태를 SirenOrderHome에서 확인 할 수 있어야 한다. → CQRS 

# Event Storming 결과

![image](https://user-images.githubusercontent.com/66457249/108225936-c54ee780-717f-11eb-8da6-89991b655732.png)

# 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/66457249/108203259-9166c880-7165-11eb-94f3-b08cd32374ff.png)


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085 ( 8085추가), 8088 이다)
```
cd SirenOrder
mvn spring-boot:run  

cd Payment
mvn spring-boot:run

cd SirenOrderHome
mvn spring-boot:run 

cd Shop
mvn spring-boot:run  

cd gateway
mvn spring-boot:run  

cd Delivery
mvn spring-boot:run
```

## DDD 의 적용
msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.

Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

**Delivery 서비스의 Delivery.java**

```java 
package winterschoolone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	private Long orderId;
    private String userId;
    private String menuId;
    private Integer qty;
    private String cancelYn;
    private String deliveryYn;
    private String Status;

	@PostPersist
    public void onPostPersist(){
        DeliveryStarted deliveryStarted = new DeliveryStarted();
        BeanUtils.copyProperties(this, deliveryStarted);
        deliveryStarted.publishAfterCommit();
    }
	
    
    public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeliveryYn() {
		return deliveryYn;
	}

	public void setDeliveryYn(String deliveryYn) {
		this.deliveryYn = deliveryYn;
	}


}


```

**Delivery 서비스의 PolicyHandler.java**
```java
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
    public void wheneverRefunded_(@Payload Refunded refunded){

    	if(refunded.isMe()){
    		
    		Delivery delivery = new Delivery();
    	
    		delivery.setId(refunded.getId());
    		deliverRepository.delete(delivery);

            
        }
    }

}

```

- DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.  
  
- 원격 주문 -> Delivery 동작 후 결과

![image](https://user-images.githubusercontent.com/66457249/108206638-00462080-716a-11eb-8afe-5284e4779850.png)


# GateWay 적용
API GateWay를 통하여 마이크로 서비스들의 집입점을 통일할 수 있다.
다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: SirenOrder
          uri: http://localhost:8081
          predicates:
            - Path=/sirenOrders/** 
        - id: Payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: Shop
          uri: http://localhost:8083
          predicates:
            - Path=/shops/** 
        - id: SirenOrderHome
          uri: http://localhost:8084
          predicates:
            - Path= /sirenOrderHomes/**
        - id: Delivery
          uri: http://localhost:8085
          predicates:
            - Path= /deliveries/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/*
        - id: SirenOrder
          uri: http://SirenOrder:8080
          predicates:
            - Path=/sirenOrders/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: Shop
          uri: http://Shop:8080
          predicates:
            - Path=/shops/** 
        - id: SirenOrderHome
          uri: http://SirenOrderHome:8080
          predicates:
            - Path= /sirenOrderHomes/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```
8088로 호출해도, delivery 포트인 8085를 호출한다.
![image](https://user-images.githubusercontent.com/66457249/108208599-93805580-716c-11eb-8f5a-5f0f270c5f6a.png)

# CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서 View 역할은 SirenOrderHomes 서비스가 수행한다.

- 주문(ordered) 실행 후 SirenOrderHomes 화면

![image](https://user-images.githubusercontent.com/66457249/108207766-8747c880-716b-11eb-9861-7adcdd424bc7.png)

- 주문(OrderCancelled) 취소 후 SirenOrderHomes 화면

![image](https://user-images.githubusercontent.com/66457249/108207845-9fb7e300-716b-11eb-9871-1a52a226ee02.png)

위와 같이 주문을 하게되면 SirenOrder -> Payment -> Shop, Delivery ->  로 주문이 Assigend 되고

주문 취소가 되면 Status가 refunded로 Update 되는 것을 볼 수 있다.

또한 Correlation을 key를 활용하여 orderId를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

# 폴리글랏

Shop 서비스의 DB와 Delivery의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Shop의 pom.xml DB 설정 코드**

![증빙5](https://user-images.githubusercontent.com/53815271/107909600-e2c35c00-6f9b-11eb-8ec4-e8ef46c07949.png)

**Delivery의 pom.xml DB 설정 코드**

![증빙4](https://user-images.githubusercontent.com/53815271/107909551-d17a4f80-6f9b-11eb-8af2-71b4d0112206.png)

# 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 결제(Payment)취소->배송(Delivery)취소 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

**Payment 서비스 내 external.DeliveryService**
```java

package winterschoolone.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="Delivery", url="${api.url.Delivery}")
public interface DeliveryService {

    @RequestMapping(method= RequestMethod.POST, path="/deliveries")
    public void deliveryStart(@RequestBody Delivery delivery);    
	
}

```

**동작 확인**
- Delivery 서비스 중지하고, 결제 발생시 에러 발생

![image](https://user-images.githubusercontent.com/66457249/108227066-d8ae8280-7180-11eb-88e8-06d9c5fd3398.png)

- Delivery 서비스 재기동 후 정상동작 확인

![image](https://user-images.githubusercontent.com/66457249/108227638-6ee2a880-7181-11eb-9ab0-025a8547a101.png)

# 운영

# Deploy / Pipeline

- git에서 소스 가져오기
```
git clone https://github.com/SOYA95/starbagsa.git
```
- Build 하기
```
cd ..
cd Delivery
mvn package

cd /winterone
cd gateway
mvn package

cd ..
cd sirenorder
mvn package

cd ..
cd payment
mvn package

cd ..
cd shop
mvn package

cd ..
cd sirenorderhome
mvn package
```

- Docker Image Push/deploy/서비스생성
```
cd gateway
az acr build --registry skteam01 --image skuser05.azurecr.io/gateway:v1 .
kubectl create ns tutorial

kubectl create deploy gateway --image=skuser05.azurecr.io/gateway:v1 -n tutorial
kubectl expose deploy gateway --type=ClusterIP --port=8080 -n tutorial

cd ..
cd Delivery
az acr build --registry skteam01 --image skuser05.azurecr.io/delivery:v1 .

kubectl create deploy delivery --image=skuser05.azurecr.io/delivery:v1 -n tutorial
kubectl expose deploy delivery --type=ClusterIP --port=8080 -n tutorial

cd ..
cd shop
az acr build --registry skteam01 --image skuser05.azurecr.io/shop:v1 .

kubectl create deploy shop --image=skuser05.azurecr.io/shop:v1 -n tutorial
kubectl expose deploy shop --type=ClusterIP --port=8080 -n tutorial

cd ..
cd sirenorder
az acr build --registry skteam01 --image skuser05.azurecr.io/sirenorder:v1 .

kubectl create deploy sirenorder --image=skuser05.azurecr.io/sirenorder:v1 -n tutorial
kubectl expose deploy sirenorder --type=ClusterIP --port=8080 -n tutorial

cd ..
cd sirenorderhome
az acr build --registry skteam01 --image skteam01.azurecr.io/sirenorderhome:v1 .

kubectl create deploy sirenorderhome --image=skteam01.azurecr.io/sirenorderhome:v1 -n tutorial
kubectl expose deploy sirenorderhome --type=ClusterIP --port=8080 -n tutorial
```

- yml파일 이용한 deploy
```
cd ..
cd Payment
az acr build --registry user05 --image skuser05.azurecr.io/payment:v3 .
```
![image](https://user-images.githubusercontent.com/66457249/108291987-9021b400-71d6-11eb-9696-5c5e738d2f0c.png)

```
kubectl expose deploy payment --type=ClusterIP --port=8080 -n tutorial
```

- winterone/Payment/kubernetes/deployment.yml 파일 
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment
  namespace: tutorial
  labels:
    app: payment
spec:
  replicas: 1
  selector:
    matchLabels:
      app: payment
  template:
    metadata:
      labels:
        app: payment
    spec:
      containers:
        - name: payment
          image: skuser05.azurecr.io/payment:v3
          ports:
            - containerPort: 8080
          env:
            - name: configurl
              valueFrom:
                configMapKeyRef:
                  name: apiurl
                  key: url

```	  
- deploy 완료
![image](https://user-images.githubusercontent.com/66457249/108296345-b4808f00-71dc-11eb-8036-2587ff3fcc54.png)



# ConfigMap 
- 시스템별로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리

- Payment application.yml 파일에 ${configurl} 설정

```yaml
      feign:
        hystrix:
          enabled: true
      hystrix:
        command:
          default:
            execution.isolation.thread.timeoutInMilliseconds: 610
      api:
        url:
          Delivery: ${configurl}

```

- ConfigMap 사용(/Payment/src/main/java/winterschoolone/external/DeliveryService.java) 

```java

@FeignClient(name="Delivery", url="${api.url.Delivery}")
public interface DeliveryService {

    @RequestMapping(method= RequestMethod.POST, path="/deliveries")
    public void deliveryStart(@RequestBody Delivery delivery);    
	
}
```

- ConfigMap 생성

```
kubectl create configmap apiurl --from-literal=url=http://10.0.149.102:8080 -n tutorial
```
  ![image](https://user-images.githubusercontent.com/66457249/108236861-b4f03a00-718a-11eb-9037-78d11559d77e.png)


# 오토스케일 아웃

- 서킷 브레이커는 시스템을 안정되게 운영할 수 있게 해줬지만, 사용자의 요청이 급증하는 경우, 오토스케일 아웃이 필요하다.

>- 단, 부하가 제대로 걸리기 위해서, delivery 서비스의 리소스를 줄여서 재배포한다.(/Delivery/kubernetes/deployment.yml 수정)

```yaml
          resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```

- 다시 expose 해준다.
```
kubectl expose deploy delivery --type=ClusterIP --port=8080 -n tutorial
```
- delivery시스템에 replica를 자동으로 늘려줄 수 있도록 HPA를 설정한다. 설정은 CPU 사용량이 5%를 넘어서면 replica를 10개까지 늘려준다.

![image](https://user-images.githubusercontent.com/66457249/108304896-4e036d00-71ec-11eb-9ef2-abd85eeaf36e.png)
```
![image](https://user-images.githubusercontent.com/66457249/108304896-4e036d00-71ec-11eb-9ef2-abd85eeaf36e.png)


kubectl exec -it pod/siege -c siege -n tutorial -- /bin/bash
siege -c100 -t120S -r10 -v --content-type "application/json" 'http://10.0.157.158:8080/deliveries POST {"orderId": 111, "qty":10}'
```
![autoscale(hpa) 실행 및 부하발생]
![image](https://user-images.githubusercontent.com/66457249/108306358-4c877400-71ef-11eb-8787-99228960c685.png)
- 오토스케일 모니터링을 걸어 스케일 아웃이 자동으로 진행됨을 확인한다.
```
kubectl get all -n tutorial
```
![image](https://user-images.githubusercontent.com/66457249/108306229-10ecaa00-71ef-11eb-84dd-f57e30cc4082.png)

# 서킷 브레이킹

- 서킷 브레이킹 프레임워크의 선택 : Spring FeignClient + Hystrix 옵션을 사용하여 구현함
- Hystrix를 설정 : 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도
  유지되면 CB 회로가 닫히도록(요청을 빠르게 실패처리, 차단) 설정

- 동기 호출 주체인 SirenOrder에서 Hystrix 설정 
- Payment/src/main/resources/application.yml 파일
```yaml
feign:
  hystrix:
    enabled: true
hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 부하에 대한 지연시간 발생코드
- winterone/Payment\src\main\java\winterschoolone\Payment.java
``` java
    public void onPostPersist(){
        Payed payed = new Payed();  
        BeanUtils.copyProperties(this, payed);
        payed.publishAfterCommit();
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
	    } catch (InterruptedException e) {
	            e.printStackTrace();
	    }
```

- 부하 테스터 siege툴을 통한 서킷 브레이커 동작확인 :
  
  동시 사용자 100명, 60초 동안 실시 
```
siege -c100 -t60S -r10 -v --content-type "application/json" 'http://10.0.119.240:8080/payments
POST {"userId": "user10", "menuId": "menu10", "qty":10}'
```
- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 다시 처리되면서 Payments를 받기 시작

![증빙10](https://user-images.githubusercontent.com/77368578/107917672-a8fa5180-6fab-11eb-9864-69af16a94e5e.png)

# 무정지 배포

- 무정지 배포가 되지 않는 readiness 옵션을 제거 설정
winterone/Shop/kubernetes/deployment_n_readiness.yml
```yml
    spec:
      containers:
        - name: shop
          image: hispres.azurecr.io/shop:v1
          ports:
            - containerPort: 8080
#          readinessProbe:
#            httpGet:
#              path: '/actuator/health'
#              port: 8080
#            initialDelaySeconds: 10
#            timeoutSeconds: 2
#            periodSeconds: 5
#            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
- 무정지 배포가 되지 않아 Siege 결과 Availability가 100%가 되지 않음

![무정지배포(readiness 제외) 실행](https://user-images.githubusercontent.com/77368578/108004272-c0cbe700-7038-11eb-94c4-22a0785a7ebc.png)
![무정지배포(readiness 제외) 실행결과](https://user-images.githubusercontent.com/77368578/108004276-c295aa80-7038-11eb-9618-1c85fe0a2f53.png)

- 무정지 배포를 위한 readiness 옵션 설정
winterone/Shop/kubernetes/deployment.yml
```yml
    spec:
      containers:
        - name: shop
          image: hispres.azurecr.io/shop:v1
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- 무정지 배포를 위한 readiness 옵션 설정 후 적용 시 Siege 결과 Availability가 100% 확인

![무정지배포(readiness 포함) 설정 및 실행](https://user-images.githubusercontent.com/77368578/108004281-c75a5e80-7038-11eb-857d-72a1c8bde94c.png)
![무정지배포(readiness 포함) 설정 결과](https://user-images.githubusercontent.com/77368578/108004284-ca554f00-7038-11eb-8f62-9fcb3b069ed2.png)

# Self-healing (Liveness Probe)

- Self-healing 확인을 위한 Liveness Probe 옵션 변경
winterone/Shop/kubernetes/deployment_live.yml
```yml
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8081
            initialDelaySeconds: 5
            periodSeconds: 5
```

- Shop pod에 Liveness Probe 옵션 적용 확인

![self-healing설정 결과](https://user-images.githubusercontent.com/77368578/108004513-697a4680-7039-11eb-917a-1e100ddd2ccd.png)

- Shop pod에서 적용 시 retry발생 확인

![self-healing설정 후 restart증적](https://user-images.githubusercontent.com/77368578/108004507-6717ec80-7039-11eb-809f-67316db013c6.png)

