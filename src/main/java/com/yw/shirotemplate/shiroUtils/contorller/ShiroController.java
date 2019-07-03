package com.yw.shirotemplate.shiroUtils.contorller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.yw.shirotemplate.myShiro.entity.User;
import com.yw.shirotemplate.myShiro.service.IUserService;
import com.yw.shirotemplate.shiroUtils.commons.CodeAndMsgEnum;
import com.yw.shirotemplate.shiroUtils.commons.ResponseEntity;
import com.yw.shirotemplate.shiroUtils.core.shiro.utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ShiroController {

    @Autowired
    DefaultKaptcha producer;
    @Autowired
    private IUserService userService;

    /**
     * 登录
     *
     * @param userInfo
     * @return
     */
    @RequestMapping(value = "/userLogin", method = RequestMethod.POST)
    public Map ajaxLogin(User userInfo, HttpServletResponse response) {
        Map result = new HashMap<>(4);
//        //验证码
//        boolean flag = userService.checkRandomToken(userInfo.getsToken(), userInfo.getTextStr());
//        if(!flag) {
//            result.put("code", CodeAndMsgEnum.ERROR.getcode());
//            result.put("msg", "验证码错误！");
//            return result;
//        }
        System.err.println("用户Code为：" + userInfo.getUserCode());
        User vo = this.userService.findByUserCode(userInfo.getUserCode());
        if (null != vo && vo.getUserPwd().equals(userInfo.getUserPwd())) {
            String tokenStr = JWTUtil.sign(userInfo.getUserCode(), userInfo.getUserPwd());
            userService.addTokenToRedis(userInfo.getUserCode(), tokenStr);
            result.put("code", CodeAndMsgEnum.SUCCESS.getcode());
            result.put("msg", "登录成功！");
            response.setHeader("Authorization", tokenStr);
        } else {
            result.put("code", CodeAndMsgEnum.ERROR.getcode());
            result.put("msg", "帐号或密码错误！");
        }
        System.err.println(response);
        return result;
    }

    /**
     * 退出
     *
     * @return
     * @throws Exception
     */
    @RequestMapping("/logout")
    public Map logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jwtToken = request.getHeader("Authorization");
        userService.removeJWTToken(jwtToken);
        response.setHeader("Authorization", null);
        return ResponseEntity.responseSuccess(null);
    }

    /**
     * 生成验证码
     *
     * @return
     */
    @RequestMapping("/captcha")
    @ResponseBody
    public Map captcha() throws IOException {
        Map result;
        try {
            // 生成文字验证码
            String text = producer.createText();
            // 生成图片验证码
            ByteArrayOutputStream outputStream = null;
            BufferedImage image = producer.createImage(text);
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", outputStream);
            // 对字节数组Base64编码
            BASE64Encoder encoder = new BASE64Encoder();
            //保存到redis
            Map temp = userService.createRandomToken(text);
            temp.put("img", encoder.encode(outputStream.toByteArray()));
            result = ResponseEntity.responseSuccess(temp);
        } catch (Exception e) {
            e.printStackTrace();
            result = ResponseEntity.responseError();
        }
        return result;
    }

    @RequestMapping("/listOnLine")
    @ResponseBody
    public Map listOnLine() throws IOException {
        Map result;
        try {
            List<User> vo = userService.listOnLineUser();
            result = ResponseEntity.responseSuccess(vo);
        } catch (Exception e) {
            e.printStackTrace();
            result = ResponseEntity.responseError();
        }
        return result;
    }

    @RequestMapping("/kickOutUser")
    @ResponseBody
    public Map kickOutUser(String userCode) {
        Map result;
        try {
            boolean flag = userService.removeJWTToken(userCode);
            result = ResponseEntity.responseSuccess(flag);
        } catch (Exception e) {
            e.printStackTrace();
            result = ResponseEntity.responseError();
        }
        return result;
    }
}
