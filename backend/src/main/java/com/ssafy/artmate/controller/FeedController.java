package com.ssafy.artmate.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.ssafy.artmate.dto.FeedDto;
import com.ssafy.artmate.service.AwsS3Service;
import com.ssafy.artmate.service.FeedService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@CrossOrigin(origins = { "*" }, maxAge = 6000)
@RestController
@RequestMapping("/api")
@Api(value = "Feed")
public class FeedController {

	@Autowired
	private AwsS3Service awservice;

	@Autowired
	private FeedService feedService;

	// 피드에 글쓰기
	@ApiOperation(value = "피드 작성", notes = "피드 작성 성공시 true, 댓글 작성 실패시 false 반환", response = Boolean.class)
	@PostMapping(value = "/feed")
	public Boolean insertFeed(@ApiParam(value = "피드 객체(feed)", required = true) @RequestPart FeedDto feed,
			@ApiParam(value = "사진 파일(file)", required = false) @RequestPart(value = "file", required = true) MultipartFile file)
			throws IOException {
		if (file != null) { // 파일이 있을때만
			String url = awservice.uploadObject(file, file.getOriginalFilename(), "feed"); // aws s3에 이미지 업로드 후 url리턴
			feed.setFeedImg(url);// 주소 바꿈
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDate = dateFormat.format(new Date());

		feed.setWriteDate(currentDate);
		return feedService.insertFeed(feed);
	}

	// 내 피드 목록 가져오기
	@ApiOperation(value = "현재 아이디가 작성한 피드 목록을 반환(* 마이피드에서 사용)", notes = "피드 리스트(피드번호, 이미지) 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/feed/list/{userId}", produces = "text/json; charset=utf8")
	public String selectAllMyFeed(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectAllMyFeed(userId));
		return result;
	}

	// 피드 상세보기
	@ApiOperation(value = "선택한 번호의 상세정보를 반환", notes = "피드 반환", response = FeedDto.class)
	@GetMapping(value = "/feed/{userId}/{id}")
	public FeedDto selectOneFeed(
			@ApiParam(value = "로그인 된 회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 번호", required = true, example = "43") @PathVariable("id") int id) {
		return feedService.selectOneFeed(userId, id);
	}

	// 피드 글 수정
	@ApiOperation(value = "번호에 해당하는 피드 내용 수정", notes = "피드 수정 성공시 true, 피드 수정 실패시 false 반환", response = Boolean.class)
	@PutMapping(value = "/feed")
	public Boolean modifyFeed(@ApiParam(value = "피드 객체", required = true) @RequestPart FeedDto feed,
			@ApiParam(value = "프로필사진(file)", required = true) @RequestPart(value = "file", required = false) MultipartFile file)
			throws IOException {
		if (file != null) { // 파일이 있을때만
			String url = awservice.uploadObject(file, file.getOriginalFilename(), "feed"); // aws s3에 이미지 업로드 후 url리턴
			feed.setFeedImg(url);// 주소 바꿈
			feedService.modifyFeedImg(feed); // 피드 이미지 변경
		}
		return feedService.modifyFeed(feed); // 피드 내용 변경(이미지 빼고)
	}

	// 피드 글 삭제
	@ApiOperation(value = "번호에 해당하는 피드 내용 삭제", notes = "피드 삭제 성공시 true, 피드 삭제 실패시 false 반환", response = Boolean.class)
	@DeleteMapping("/feed/{id}")
	public Boolean deleteFeed(@ApiParam(value = "피드 번호", required = true, example = "1") @PathVariable("id") int id) {
		// 서버로부터 이미지 삭제하기 위해 이미지 주소 가져오기
		String url = feedService.selectFeedImg(id);
		awservice.deleteObject(url, "feed");
		return feedService.deleteFeed(id);
	}

	// 내가 팔로우한 회원들 피드 목록 가져오기 (뉴스피드)
	@ApiOperation(value = "현재 로그인한 회원이 팔로우한 회원들의 피드 목록 반환", notes = "피드 리스트 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/newsfeed/list/{userId}", produces = "text/json; charset=utf8")
	public String selectAllNewsFeed(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectAllNewsFeed(userId));
		return result;
	}

	// 북마크에 피드 추가하기
	@ApiOperation(value = "내 북마크에 피드 추가", notes = "북마크에 추가 성공시 true, 추가 실패시 false 반환", response = Boolean.class)
	@PutMapping(value = "/bookmark/{userId}/{feedId}")
	public Boolean insertBookmark(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 번호", required = true, example = "1") @PathVariable("feedId") int feedId) {
		if (feedService.selectOneBookmark(userId, feedId) != null) {
			return false;
		}
		return feedService.insertBookmark(userId, feedId);
	}

	// 북마크에 피드 삭제하기
	@ApiOperation(value = "내 북마크에 피드 삭제", notes = "북마크에 삭제 성공시 true, 삭제 실패시 false 반환", response = Boolean.class)
	@DeleteMapping(value = "/bookmark/{userId}/{feedId}")
	public Boolean deleteBookmark(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 번호", required = true, example = "1") @PathVariable("feedId") int feedId) {
		return feedService.deleteBookmark(userId, feedId);
	}

	// 북마크 목록 가져오기
	@ApiOperation(value = "현재 로그인한 회원의 북마크 목록을 반환", notes = "북마크 리스트 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/bookmark/list/{userId}", produces = "text/json; charset=utf8")
	public String selectMyBookmark(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectMyBookmark(userId));
		return result;
	}

	// 좋아요 추가하기
	@ApiOperation(value = "좋아요 추가", notes = "좋아요 추가 성공시 true, 추가 실패시 false 반환", response = Boolean.class)
	@PutMapping(value = "/likemark/{userId}/{feedId}")
	public Boolean insertLikemark(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 번호", required = true, example = "1") @PathVariable("feedId") int feedId) {
		if (feedService.selectOneLikemark(userId, feedId) != null) {
			return false;
		}
		FeedDto feed = feedService.selectOneFeed(userId, feedId);
		int likeCnt = feed.getLikeCnt();
		feed.setLikeCnt(likeCnt + 1);
		feedService.modifyLikeCnt(feed);
		return feedService.insertLikemark(userId, feedId);
	}

	// 좋아요 삭제하기
	@ApiOperation(value = "좋아요 삭제", notes = "좋아요 삭제 성공시 true, 삭제 실패시 false 반환", response = Boolean.class)
	@DeleteMapping(value = "/likemark/{userId}/{feedId}")
	public Boolean deleteLikemark(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 번호", required = true, example = "1") @PathVariable("feedId") int feedId) {
		if (feedService.deleteLikemark(userId, feedId)) {
			FeedDto feed = feedService.selectOneFeed(userId, feedId);
			int likeCnt = feed.getLikeCnt();
			feed.setLikeCnt(likeCnt - 1);
			feedService.modifyLikeCnt(feed);
			return true;
		}
		return false;
	}

	// 모든 유저의 피드 목록 가져오기 (전체 피드)
	@ApiOperation(value = "모든 유저의 피드 목록(전체 피드)을 반환", notes = "전체 피드 리스트 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/feed/allList/{userId}", produces = "text/json; charset=utf8")
	public String selectAllFeed(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectAllFeed(userId));
		return result;
	}

	// 전시할 피드 선택해서 저장
	@ApiOperation(value = "3D 전시할 피드 저장", notes = "저장 성공시 true, 실패시 false", response = Boolean.class)
	@PutMapping(value = "/feed/exhibit/{userId}/{feedId}")
	public boolean insertFeedExhibit(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 아이디", required = true, example = "17") @PathVariable("feedId") int feedId) {
		if (!feedService.checkFeedExhibit(userId, feedId))
			return false;
		return feedService.insertFeedExhibit(userId, feedId);
	}

	// 전시할 피드 중에서 삭제
	@ApiOperation(value = "3D 전시할 피드 중 피드 삭제", notes = "삭제 성공시 true, 실패시 false", response = Boolean.class)
	@DeleteMapping(value = "/feed/exhibit/{userId}/{feedId}")
	public boolean deleteFeedExhibit(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId,
			@ApiParam(value = "피드 아이디", required = true, example = "17") @PathVariable("feedId") int feedId) {
		return feedService.deleteFeedExhibit(userId, feedId);
	}

	// 전시할 피드 목록 가져오기
	@ApiOperation(value = "3D 전시할 피드 목록 가져오기", notes = "전시할 피드 리스트 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/feed/exhibit/{userId}", produces = "text/json; charset=utf8")
	public String selecteFeedExhibit(
			@ApiParam(value = "회원 아이디", required = true, example = "unni2@naver.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectFeedExhibit(userId));
		return result;
	}

	// 유저 피드테마 번호 업데이트
	@ApiOperation(value = "3D 유저 피드테마 번호 업데이트", notes = "유저 피드테마 번호 업데이트 성공시 success, 실패시 fail 반환 ", response = Boolean.class)
	@PutMapping(value = "/feed/theme/{userId}/{theme}")
	public Boolean insertFeedTheme(
			@ApiParam(value = "회원 아이디", required = true, example = "jhw1234527@gmail.com") @PathVariable("userId") String userId,
			@ApiParam(value = "테마 번호", required = true, example = "1") @PathVariable("theme") int theme) {
		//유저 피드테마 번호 업데이트
		return feedService.modifyFeedTheme(userId, theme); //있으면 업데이트
	}
	
	//유저 피드테마 번호 가져오기
	@ApiOperation(value = "3D 유저 피드테마 번호 가져오기", notes = "유저 테마 번호 가져오기(int)", response = Integer.class)
	@GetMapping(value = "/feed/theme/{userId}", produces = "text/json; charset=utf8")
	public String selectFeedTheme(
			@ApiParam(value = "회원 아이디", required = true, example = "jhw1234527@gmail.com") @PathVariable("userId") String userId) {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectFeedTheme(userId));
		return result;
	}
	
	// 전시할 피드 목록 가져오기
	@ApiOperation(value = "인기순으로 피드 목록 가져오기", notes = "인기순 피드 리스트 반환", response = FeedDto.class, responseContainer = "List")
	@GetMapping(value = "/feed/popular", produces = "text/json; charset=utf8")
	public String selectePopularFeed() {
		Gson gs = new Gson();
		String result = gs.toJson(feedService.selectPopularFeed());
		return result;
	}
}
