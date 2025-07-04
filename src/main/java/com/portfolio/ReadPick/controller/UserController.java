package com.portfolio.ReadPick.controller;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.portfolio.ReadPick.dao.BookCategoryMapper;
import com.portfolio.ReadPick.dao.BookmarkMapper;
import com.portfolio.ReadPick.dao.UserMapper;
import com.portfolio.ReadPick.service.BookService;
import com.portfolio.ReadPick.vo.BsVo;
import com.portfolio.ReadPick.vo.UserImageVo;
import com.portfolio.ReadPick.vo.UserSessionDTO;
import com.portfolio.ReadPick.vo.UserVo;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class UserController {

    @Autowired
    HttpSession session;

    @Autowired
    UserMapper userMapper;

    @Autowired
    BookCategoryMapper bookCategoryMapper;

    @Autowired
    BookmarkMapper bookmarkMapper;

    @Autowired
    BookService bookService;

    @Autowired
    ServletContext application;

    @PostMapping("list")
    public ResponseEntity<List<UserVo>> list() {

        List<UserVo> list = userMapper.selectList();

        return ResponseEntity.ok(list);
    }

    @PostMapping("login")
    @Operation(summary = "로그인", description = "로그인하기")
    public ResponseEntity<?> login(@RequestBody UserVo user) {
        String id = user.getId();
        String pw = user.getPw();

        UserVo dbUser = userMapper.selectOneFromId(id);
        // 조회된 아이디가 없는(틀린)경우 || 비밀번호가 틀린 경우
        if (dbUser == null || dbUser.getPw().equals(pw) == false) {
            return ResponseEntity.ok("fail");
        }

        // 로그인처리: 현재 로그인된 객체(user)정보를 session저장
        if (session.getAttribute("user") == null) {
            session.setAttribute("user", dbUser);
            session.setMaxInactiveInterval(30 * 60); // 세션 유효 시간 30분
        }

        // 로그인정보전달용 DTO객체
        UserSessionDTO sessionUserInfo = new UserSessionDTO();
        sessionUserInfo.setUserIdx(dbUser.getUserIdx());
        sessionUserInfo.setNickName(dbUser.getNickName());
        sessionUserInfo.setUserName(dbUser.getUserName());
        sessionUserInfo.setEmail(dbUser.getEmail());
        sessionUserInfo.setAdminAt(dbUser.getAdminAt());
        sessionUserInfo.setFirstAt(dbUser.getFirstAt());
        sessionUserInfo.setId(dbUser.getId());
        return ResponseEntity.ok(sessionUserInfo);
    }

    // 로그아웃
    @PostMapping("logout")
    @Operation(summary = "로그아웃", description = "로그아웃하기")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        if (session != null)
            session.invalidate();
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("success");
    }// end:logout()

    // 아이디 중복체크
    @PostMapping("checkId")
    @Operation(summary = "아이디 중복체크", description = "회원가입 시 아이디 중복체크")
    public ResponseEntity<String> check_id(String id) {

        UserVo userId = userMapper.selectOneFromId(id);

        boolean bResult = false;
        if (userId == null)
            bResult = true;
        else
            bResult = false;

        String json = String.format("{\"result\": %b }", bResult);

        return ResponseEntity.ok(json);
    }// end:check_id()

    // 회원가입
    @PostMapping("userInsert")
    @Operation(summary = "회원가입", description = "회원가입하기")
    public ResponseEntity<String> userInsert(@RequestBody UserVo user) {

        int res = userMapper.userInsert(user);

        return ResponseEntity.ok("success");
    }

    // 로그인체크
    @PostMapping("checkLogin")
    @Operation(summary = "로그인체크", description = "로그인체크")
    public ResponseEntity<String> checkLogin(HttpServletResponse response) {
        UserVo user = (UserVo) session.getAttribute("user");
        if (user == null) {
            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(0);
            response.addCookie(cookie);
            return ResponseEntity.ok("fail");
        }
        return ResponseEntity.ok("success");
    }

    @GetMapping("userPick")
    @Operation(summary = "첫 로그인 시 띄울 장르 페이지")
    public ResponseEntity<List<BsVo>> userPick() {

        List<BsVo> categories = bookCategoryMapper.selectCategoryView();

        return ResponseEntity.ok(categories);
    }

    @PostMapping("userPickResult")
    @Operation(summary = "테스트불가", description = "처음 로그인 했을 때 고른 장르들을 저장하는 코드지만 데이터를 정확하게 보내야 하는 이슈로 스웨거에서 테스트불가")
    public ResponseEntity<String> userPickResult(@RequestBody List<List<Integer>> userPick) {

        UserVo user = (UserVo) session.getAttribute("user");

        try {
            if (user != null) {
                for (int i = 0; i < userPick.size(); i++) {
                    int bsIdx = userPick.get(i).get(0);
                    int bssIdx = userPick.get(i).get(1);
                    int bmIdx = bookCategoryMapper.selectBmIdxOneByBsIdx(bsIdx);
                    int res = bookCategoryMapper.insertUserPick(user.getUserIdx(), bmIdx, bsIdx, bssIdx);

                }
                user.setFirstAt("N");
                int res = userMapper.userFirstAtUpdate(user);
                System.out.println("userPick = " + bookCategoryMapper.selectUserPick());

                return ResponseEntity.ok("success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("fail!!!!!!!!!!!!!!!!!!!!");
        }

        return ResponseEntity.ok("fail");
    }

    @GetMapping("firstAt")
    @Operation(summary = "첫 로그인 여부", description = "유저가 로그인 시 첫 로그인 여부를 확인")
    public ResponseEntity<String> firstAt() {
        UserVo user = (UserVo) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.ok("로그인필요");
        }
        // System.out.println(user.getFirstAt());
        return ResponseEntity.ok(user.getFirstAt());
    }

    // user 프로필 이미지 추가
    @PostMapping(value = "userImageInsert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> userImageInsert(@RequestPart("file") MultipartFile file) throws Exception {

        UserVo user = (UserVo) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.ok("login:fail");
        }
        int userIdx = user.getUserIdx();
        String absPath = application.getRealPath("/resources/images/");

        try {
            if (!file.isEmpty()) {
                String filename = file.getOriginalFilename();
                UserImageVo userImage = new UserImageVo();
                userImage.setUserIdx(userIdx);
                userImage.setFileName(filename);
                userMapper.insertUserImage(userImage);
                File f = new File(absPath, filename);
                if (f.exists()) {// 동일한 파일이 존재하냐?
                    // 시간_파일명 이름변경
                    long tm = System.currentTimeMillis();
                    filename = String.format("%d_%s", tm, filename);
                    f = new File(absPath, filename);
                }
                file.transferTo(f);
            } else {
                return ResponseEntity.ok("fail");
            }
        } catch (Exception e) {
            System.out.println("userImageInsert: " + e.getMessage());
            return ResponseEntity.ok("fail");
        }

        return ResponseEntity.ok("success");
    }

    // 프로필 이미지 삭제
    @PostMapping("userImageDelete")
    public ResponseEntity<String> userImageDelete() {
        UserVo user = (UserVo) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.ok("login:fail");
        }
        int userIdx = user.getUserIdx();
        String absPath = application.getRealPath("/resources/images/");
        String deleteFileName = userMapper.selectUserImageFromUserIdx(userIdx);
        File deleteFile = new File(absPath, deleteFileName);
        if (deleteFile.exists()) {
            boolean deleted = deleteFile.delete();
            if (!deleted) {
                System.out.println("파일 삭제 실패: " + deleted);
            } else {
                System.out.println("파일 삭제 성공: " + deleted);
            }
        }

        int res = userMapper.deleteUserImage(userIdx);
        return ResponseEntity.ok("success");
    }

    // 프로필 이미지 변경
    @PostMapping(value = "userImageUpdate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> userImageUpdate(@RequestPart("file") MultipartFile file) throws Exception {
        UserVo user = (UserVo) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.ok("login:fail");
        }
        int userIdx = user.getUserIdx();
        String absPath = application.getRealPath("/resources/images/");

        // 기존 이미지 삭제
        String deleteFileName = userMapper.selectUserImageFromUserIdx(userIdx);
        System.out.println("deleteFileName: " + deleteFileName);
        File deleteFile = new File(absPath, deleteFileName);
        try {
            if (deleteFile.exists()) {
                boolean deleted = deleteFile.delete();
                if (!deleted) {
                    System.out.println("파일 삭제 실패: " + deleted);
                } else {
                    System.out.println("파일 삭제 성공: " + deleted);
                }
            } else {
                return ResponseEntity.ok("fileDelete:fail");
            }
        } catch (Exception e) {
            // TODO: handle exception
            System.out.println(e);
        }

        try {
            if (!file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                File f = new File(absPath, fileName);
                file.transferTo(f);
                UserImageVo userImage = new UserImageVo();
                userImage.setUserIdx(userIdx);
                userImage.setFileName(fileName);
                userMapper.updateUserImage(userImage);
            } else {
                return ResponseEntity.ok("fail");
            }
        } catch (Exception e) {
            System.out.println("userImageUpdate: " + e.getMessage());
            return ResponseEntity.ok("fail");
        }

        return ResponseEntity.ok("success");
    }

}
