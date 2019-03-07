package xjtucad.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import xjtucad.SSOManager;
import xjtucad.dao.UserDao;
import xjtucad.manager.ITokenManager;
import xjtucad.model.LoginResult;
import xjtucad.model.Result;
import xjtucad.util.KeyName;
import xjtucad.util.MD5;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

public class Login extends HttpServlet {

    SSOManager ssoManager = SSOManager.getInstance();
    ITokenManager tokenManager = ssoManager.getTokenManager();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json        ;charset=UTF-8");
        Map<String,String> userInfo = (Map<String,String>)req.getSession().getAttribute("userInfo");
        Result result = new Result();
        if(userInfo==null){
            result.state=false;
        }else {
            result.state=true;
            result.content=JSONObject.toJSONString(userInfo);
        }
        resp.getWriter().write(JSONObject.toJSONString(result));
        resp.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //设置输出格式
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
        UserDao userDao = new UserDao();
        //获取登录参数
        String serviceURL = req.getParameter("serviceURL");//请求的地址
        String username = req.getParameter("username");//用户名
        String password = req.getParameter("password");//密码
        Result result = new Result();
        if(username==null||username.length()<1){
            result.content=LoginResult.NO_USERNAME.toString();
        }else if(password==null||password.length()<1){
            result.content=LoginResult.NO_PASSWORD.toString();
        }else {
            //首先验证用户名
            LoginResult info = userDao.validateUsername(username);
            if(info!=LoginResult.SUCCESS){
                result.content = info.toString();
            }else {
                Map<String,String> user = userDao.getUser(username,password);
                if(user==null||user.isEmpty()){
                    result.content=LoginResult.PASSWORD_ERROR.toString();
                }else {
                    String token = MD5.md5(username+System.currentTimeMillis());
                    tokenManager.storeUserInfo(user,token);
                    result.state=true;
                    result.content = LoginResult.SUCCESS.toString();
                    //新建cookie并设置cookie的过期时间
                    resp.addCookie(packCookie(KeyName.TOKEN,token,1000*60*60*24,"/"));
                    //这里使用URLEncoder.encode进行编码主要是为了在cookie中保存中文，
                    resp.addCookie(packCookie(KeyName.COOKIE_USER_INFO,URLEncoder.encode(JSONObject.toJSONString(user),"utf-8"),1000*60*60*24,"/"));
                    req.getSession().setAttribute(KeyName.TOKEN,token);
                }
            }
        }
        if(result.state){
            resp.sendRedirect(serviceURL);
        }else {
            resp.sendRedirect("/webresources/userLogin.jsp?serviceURL="+serviceURL+"&error="+ URLEncoder.encode(result.content, "utf-8"));
        }
    }

    public Cookie packCookie(String name,String value,int maxAge,String path){
        Cookie cookie = new Cookie(name,value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        return cookie;
    }

}