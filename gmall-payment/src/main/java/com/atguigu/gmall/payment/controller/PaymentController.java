package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;


    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @RequestMapping("index")
    public String index(String orderId, HttpServletRequest request){

        //选择支付渠道

        //保存订单id
        request.setAttribute("orderId",orderId);
        //获取总金额,通过orderId获取订单的总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
//        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request,HttpServletResponse response){
        /**
         * 1. 保存支付记录，将支付记录写入数据库
         * 去重，对账（跟支付宝对账）。幂等性操作：保证每一笔交易只交易一次
         * （幂等性关注的是以后的多次请求是否对资源产生了副作用，而不是关注的结果）
         * parmentInfo
         * 2. 生成支付二维码
         */
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfo = new PaymentInfo();
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //属性赋值
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("给老詹一尊雕像");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());

        paymentService.savePaymentInfo(paymentInfo);

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);//设置同步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址,异步回调

        // 声明一个Map
        Map<String,Object> bizContnetMap=new HashMap<>();
        bizContnetMap.put("out_trade_no",paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code","FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject",paymentInfo.getSubject());
        bizContnetMap.put("total_amount",paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        return form;
    }

    //http://payment.gmall.com/alipay/callback/return
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){
        return "redirect:"+AlipayConfig.return_order_url;
    }

    // 异步回调
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request){

        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean flag = false; //调用SDK验证签名
        try {
            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(flag){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 对业务的二次校验
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            // 支付成功之后，需要做什么？
            // 需要得到trade_status
            String trade_status = paramMap.get("trade_status");
            // 通过out_trade_no 查询支付状态记录
            String out_trade_no = paramMap.get("out_trade_no");

            // String total_amount = paramMap.get("total_amount");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 当前的订单支付状态如果是已经付款，或者是关闭
                // select * from paymentInfo where out_trade_no =?
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);//paymentService

                if (paymentInfo.getPaymentStatus()==PaymentStatus.PAID || paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "failure";
                }

                // 更新交易记录的状态！
                // update paymentInfo set PaymentStatus=PaymentStatus.PAID , callbackTime = new Date() where out_trade_no=?

                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());

                paymentService.updatePaymentInfo(out_trade_no,paymentInfoUPD);//paymentService

                //发送消息队列给订单:orderId,result
                paymentService.sendPaymentResult(paymentInfo,"success");

                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    // http://payment.gmall.com/refund?orderId=100
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        // 退款接口
        boolean result =  paymentService.refund(orderId);
        return ""+result;
    }

    // 根据orderId 支付
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map wxSubmit(String orderId){
        // 调用服务层生成数据

        // 调用service服务层判断一下是否可以继续支付
        /*
            boolean flag = paymentSerivce.checkPay(orderId); flag ==true :验证成功，可以支付，false 表示验证失败！不能支付
            if(flag){
                   orderId = UUID.randomUUID().toString().replace("-","");
                   Map map = paymentSerivce.createNative(orderId,"1");
                   System.out.println(map.get("code_url"));
                    // map中必须有code_url
                   return map;
            }else{
                    return new HashMap();
            }
         */
        // IdWorker 自动生成一个Id

        // orderId 订单编号，1 表示金额 分
        orderId = "97";//97
//        orderId = UUID.randomUUID().toString().replace("-","");
        Map map = paymentService.createNative(orderId,"1");
        System.out.println(map.get("code_url"));
        // map中必须有code_url
        return map;
    }

    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "OK";
    }

    // 查询订单信息
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return ""+flag;
    }


}
