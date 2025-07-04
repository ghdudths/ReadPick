package com.portfolio.ReadPick.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.portfolio.ReadPick.vo.UserImageVo;
import com.portfolio.ReadPick.vo.UserVo;

@Mapper
public interface UserMapper {

    List<UserVo> selectList();

    UserVo selectOneFromId(String id);

    int userInsert(UserVo user);

	int userFirstAtUpdate(UserVo user);

	void insertUserImage(UserImageVo userImage);

    int deleteUserImage(int userIdx);

    String selectUserImageFromUserIdx(int userIdx);

    void updateUserImage(UserImageVo userImage);

}
