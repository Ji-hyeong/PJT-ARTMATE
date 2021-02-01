package com.ssafy.artmate.service;

import java.util.List;

import com.ssafy.artmate.Dto.SearchDto;
import com.ssafy.artmate.Dto.UserDto;

public interface SearchService {

	SearchDto selectKeyword(String keyword); //검색어가 테이블에 존재하는지 조회
	boolean insertKeyword(String keyword); //해당하는 검색어 없다면 삽입
	boolean modifyKeywordCnt(SearchDto search); //검색횟수 하나 업데이트
	List<UserDto> selectKeywordUser(String keyword); //사용자 테이블에서 검색
	List<String> selectPopular(); //검색횟수 상위 5개 리스트 출력
	List<String> selectAllKeywords(); //전체 검색어 리스트 출력

}