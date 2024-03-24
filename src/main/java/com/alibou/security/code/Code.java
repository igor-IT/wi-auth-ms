package com.alibou.security.code;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Document(collection = "codes")
public class Code {
	@MongoId
	private String id;
	private String code;
	private CodeStatus status;
	private String phone;
	@JsonProperty("timestamp")
	private Date date;
	private Type type;
}