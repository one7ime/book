package io.kt.cloud.tanda.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import io.kt.cloud.tanda.App;
import io.kt.cloud.tanda.enumeration.BookStatus;
import io.kt.cloud.tanda.event.BookCanceled;
import io.kt.cloud.tanda.event.BookPlaced;
import io.kt.cloud.tanda.kafka.KafkaSender;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@Getter
@Setter
@ToString
@Data
public class Book {

	@Transient
	public static final String STATUS_PLACED = "PLACED";
	@Transient
	public static final String STATUS_PENDDING = "PENDDING";
	@Transient
	public static final String STATUS_CANCELED = "CANCELED";

	@Id
	@GeneratedValue
	private Long bookId; // ID : 자동생성
	private String customerInfo; // 고객정보 : "유병전/전화번호/카드번호"
	private String departmentLoc; // 출발지 : 분당KT본사
	private String arrivalLoc; // 도착지 : 강남역
	private String bookStatus; // 둘중 하나 : 접수,취소(고객),취소(시스템)
	private LocalDateTime lastModifyTime; // DB INSERT, UPDATE Time으로 @PreUpate Hook에서 셋팅
	private boolean confirmed = false;
//  private int bookFree;// 대기 운전사 수 
//	private String dispatchStatus;
	
	@PrePersist
	public void checkBP() {
		if (getCustomerInfo() == null || getDepartmentLoc() == null || getArrivalLoc() == null) {
			throw new RuntimeException("잘못된 접수입 니다: ");
		}
		setBookStatus(BookStatus.ACCEPTED.getHangul());
		setLastModifyTime(LocalDateTime.now());
	}

	@PostPersist
	public void placedBook() {
		BookPlaced bookPlaced = new BookPlaced();
		bookPlaced.setBookId(this.getBookId());
		bookPlaced.setCustomerInfo(this.getCustomerInfo());
		bookPlaced.setDepartmentLoc(this.getDepartmentLoc());
		bookPlaced.setArrivalLoc(this.getArrivalLoc());
		bookPlaced.setBookStatus(BookStatus.ACCEPTED.getHangul());
		bookPlaced.setLastModifyTime(LocalDateTime.now());
		KafkaSender.pub(bookPlaced);
	}
	
	@PreUpdate
	public void checkBC() {
		
		if (getBookId() == null && !getBookStatus().equals(BookStatus.CANCELED_BY_CUSTUMER.getHangul())) {
			throw new RuntimeException("잘못된 취소입 니다: " + this);
		}

		if (!isConfirmed() && getBookStatus().equals(BookStatus.CANCELED_BY_CUSTUMER.getHangul())) {
			this.setBookStatus(BookStatus.CANCEL_PENDED.getHangul());
		}
		
		setLastModifyTime(LocalDateTime.now());
	}

	@PostUpdate
	public void canceledBook() {
		
		if (bookStatus != null && 
				(bookStatus.equals(BookStatus.CANCELED_BY_CUSTUMER.getHangul()) ||
						bookStatus.equals(BookStatus.CANCEL_PENDED.getHangul()))) {
			// 주문 번호 조회
			BookRepository bookRepository = App.applicationContext.getBean(BookRepository.class);			
			bookRepository.findById(this.getBookId()).ifPresent( book -> {
				
				BookCanceled bookCanceled = new BookCanceled();
				
				bookCanceled.setBookId(book.getBookId());
				bookCanceled.setCustomerInfo(book.getCustomerInfo());
				bookCanceled.setDepartmentLoc(book.getDepartmentLoc());
				bookCanceled.setArrivalLoc(book.getArrivalLoc());				
				bookCanceled.setBookStatus(BookStatus.CANCELED_BY_CUSTUMER.getHangul());
				bookCanceled.setLastModifyTime(LocalDateTime.now());
				if (!book.isConfirmed()) {
					KafkaSender.pub(bookCanceled);	
				}
			});
		}
	}
}
