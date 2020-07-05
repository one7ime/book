package io.kt.cloud.tanda.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class TaxiDispatch {
		
	@Id
	private Long dispatchId;				//ID 	: 자동생성
	private Long bookId;					//Book Entity와 relation
	private String TaxiInfo;				//배차된 택시정보 : 차번호/전화번호
	private String dispatchStatus;			//배차됨, 운행시작됨, 운행종료됨, 배차취소됨
	private LocalDateTime lastModifyTime;	//DB INSERT, UPDATE Time으로 @PreUpate Hook에서 셋팅 
	
}
