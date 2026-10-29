package com.wetube.wetube_service.service.premium.paypal;

/** PayPal trả lỗi hoặc không gọi được; message là câu an toàn để hiện cho người dùng (không chứa khoá). */
public class PaypalException extends RuntimeException {

    public PaypalException(String message) {
        super(message);
    }

    public PaypalException(String message, Throwable cause) {
        super(message, cause);
    }
}
