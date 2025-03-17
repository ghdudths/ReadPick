package com.portfolio.ReadPick.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.portfolio.ReadPick.vo.ReviewUserVo;
import com.portfolio.ReadPick.vo.ReviewVo;

@Mapper
public interface ReviewMapper {

    void insertReview(ReviewVo reviewVo);

    ReviewVo selectOneReview(int userIdx, int bookIdx);

    void reviewUpdate(ReviewVo reviewVo);

	void reviewDelete(int userIdx, int bookIdx);

	List<ReviewUserVo> selectReview(int bookIdx);

    int selectOneBookIdx(int rvIdx);

    List<ReviewUserVo> selectReviewMore(int bookIdx, int rvIdx);

}
