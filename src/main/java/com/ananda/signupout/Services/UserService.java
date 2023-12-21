package com.ananda.signupout.Services;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.ananda.signupout.Repository.UserRepository;
import com.ananda.signupout.StaticInfo.StaticInfos;
import com.ananda.signupout.model.EmailModel;
import com.ananda.signupout.model.OtpUserModel;
import com.ananda.signupout.model.ResponseMessage;
import com.ananda.signupout.model.User;
import com.ananda.signupout.model.VerifyUser;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailModel emailModel;

    @Autowired
    private OtpUserModel otpUserModel;

    @Autowired
    private ResponseMessage responseMessage;

    public String hashPassword(String password) {
        String strong_salt = BCrypt.gensalt(10);
        String encyptedPassword = BCrypt.hashpw(password, strong_salt);
        return encyptedPassword;
    }

    public ResponseEntity<Object> userAddService(User user) {
        try {
            if ((user.getUserName() != "") & (user.getEmail() != "")) {
                User userByEmail = userRepository.findByEmail(user.getEmail());
                if (userByEmail == null) {
                    String strong_salt = BCrypt.gensalt(10);
                    String encyptedPassword = BCrypt.hashpw(user.getPassword(), strong_salt);
                    user.setPassword(encyptedPassword);
                    userRepository.save(user);
                    responseMessage.setSuccess(true);
                    responseMessage.setMessage("Account Created!");
                    return ResponseEntity.badRequest().body(responseMessage);
                } else {
                    return ResponseEntity.ok("User with this email already exists!");
                }
            } else {
                return ResponseEntity.badRequest().body("Invalid user name or email");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error!");
        }
    }

    public ResponseEntity<Object> verifyingTheUserService(VerifyUser verifyUser) {
        try {
            User user = userRepository.findByEmail(verifyUser.getEmail());
            if (user != null) {
                if (BCrypt.checkpw(verifyUser.getPassword(), user.getPassword())) {
                    StaticInfos.loginStatus = true;
                    responseMessage.setSuccess(true);
                    responseMessage.setMessage("Logged in!");
                    return ResponseEntity.ok().body(responseMessage);
                } else {
                    responseMessage.setSuccess(false);
                    responseMessage.setMessage("Invalid email or password");
                    return ResponseEntity.badRequest().body(responseMessage);
                }
            } else {
                responseMessage.setSuccess(false);
                responseMessage.setMessage("Invalid email or password");
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error!");
        }
    }

    public ResponseEntity<Object> getUserDetailsByEmailService(String email) {
        try {
            User userByEmail = userRepository.findByEmail(email);
            if (userByEmail != null) {
                return ResponseEntity.ok(userByEmail);
            } else {
                responseMessage.setSuccess(false);
                responseMessage.setMessage("Invalid email");
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error!");
        }
    }

    public ResponseEntity<Object> sendingEmailService(String email, OtpUserModel otpUserModel) {
        try {
            User userByEmail = userRepository.findByEmail(email);
            if (userByEmail != null) {
                otpUserModel.setEmail(email);
                int otp = StaticInfos.generateRandom6DigitNumber();
                otpUserModel.setOtp(otp);

                emailModel.setRecipient(email);
                emailModel.setSubject("OTP for Resetting your password");
                emailModel.setMsgBody("Your OTP for resetting your password is " + Integer.toString(otp)
                        + ". It is valid only for 1 minute.");

                String response = emailService.sendSimpleMail(emailModel);
                responseMessage.setSuccess(true);
                responseMessage.setMessage(response);
                return ResponseEntity.badRequest().body(responseMessage);
            } else {
                responseMessage.setSuccess(false);
                responseMessage.setMessage("Invalid Email");
                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error!");
        }
    }

    public ResponseEntity<Object> verifyTheOtpEnteredByUser(String otpFromUser) {
        try {
            if (otpFromUser.equals(Integer.toString(otpUserModel.getOtp()))) {
                responseMessage.setSuccess(true);
                responseMessage.setMessage("OTP Verified");
                return ResponseEntity.ok().body(responseMessage);

            } else {
                responseMessage.setSuccess(false);
                responseMessage.setMessage("Invalid OTP, check your registered Email to get the 6-digit OTP");

                return ResponseEntity.badRequest().body(responseMessage);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error!");
        }
    }

    public ResponseEntity<Object> forgotPasswordService(String email) {
        return sendingEmailService(email, otpUserModel);
    }

    public ResponseEntity<Object> resetThePasswordService(String passwordFromUser) {
        try {
            User user = userRepository.findByEmail(otpUserModel.getEmail());
            user.setPassword(hashPassword(passwordFromUser));
            userRepository.save(user);

            responseMessage.setSuccess(true);
            responseMessage.setMessage("Password Changed Successfully");
            return ResponseEntity.ok().body(responseMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error!");
        }
    }

}
